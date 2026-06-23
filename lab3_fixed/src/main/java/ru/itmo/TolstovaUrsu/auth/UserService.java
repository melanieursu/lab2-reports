package ru.itmo.TolstovaUrsu.auth;

import ru.itmo.TolstovaUrsu.db.DatabaseManager;
import ru.itmo.TolstovaUrsu.db.PostgresStorage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

public class UserService {

    private final Map<String, User> users = new LinkedHashMap<>();
    private User currentUser = null;

    private final String usersFilePath;

    private PostgresStorage pgStorage;
    private boolean dbMode = false;


    public UserService() {
        this.usersFilePath = null;
    }


    public UserService(String usersFilePath) {
        this.usersFilePath = usersFilePath;
        loadUsersFromFile();
    }


    public void setPgStorage(PostgresStorage pgStorage) {
        this.pgStorage = pgStorage;
        this.dbMode = true;
    }

    public void register(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не может быть пустым.");
        }

        String key = login.trim().toLowerCase();

        if (users.containsKey(key)) {
            throw new IllegalArgumentException("Логин '" + login + "' уже занят.");
        }

        User user = new User(login.trim(), hash(password));

        if (dbMode) {
            if (pgStorage == null) {
                throw new IllegalStateException("PostgreSQL-хранилище пользователей не инициализировано.");
            }

            try {
                pgStorage.insertUser(user);
                users.put(key, user);
            } catch (SQLException e) {
                throw new IllegalArgumentException(
                        "Ошибка БД при регистрации пользователя: "
                                + DatabaseManager.friendlyMessage(e)
                );
            }

            return;
        }


        users.put(key, user);
        saveUsersToFile();
    }

    public User login(String login, String password) {
        if (login == null || password == null) {
            throw new IllegalArgumentException("Логин и пароль не могут быть пустыми.");
        }

        String key = login.trim().toLowerCase();
        User user = users.get(key);

        if (user == null || !user.getPasswordHash().equals(hash(password))) {
            throw new IllegalArgumentException("Неверный логин или пароль.");
        }

        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void registerWithHash(String login, String passwordHash) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым.");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Хэш пароля не может быть пустым.");
        }

        String key = login.trim().toLowerCase();

        if (users.containsKey(key)) {
            throw new IllegalArgumentException("Логин '" + login + "' уже занят.");
        }

        users.put(key, new User(login.trim(), passwordHash));
    }

    public Optional<User> findByLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(users.get(login.trim().toLowerCase()));
    }


    private void loadUsersFromFile() {
        if (usersFilePath == null) {
            return;
        }

        File file = new File(usersFilePath);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",", 2);

                if (parts.length != 2) {
                    continue;
                }

                String login = parts[0].trim();
                String hash = parts[1].trim();

                if (!login.isEmpty() && !hash.isEmpty()) {
                    users.put(login.toLowerCase(), new User(login, hash));
                }
            }

        } catch (IOException e) {
            System.err.println("[UserService] Не удалось загрузить пользователей: " + e.getMessage());
        }
    }


    private void saveUsersToFile() {
        if (usersFilePath == null) {
            return;
        }

        File file = new File(usersFilePath);

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            pw.println("# login,passwordHash(SHA-256)");

            for (User u : users.values()) {
                pw.println(u.getLogin() + "," + u.getPasswordHash());
            }

        } catch (IOException e) {
            System.err.println("[UserService] Не удалось сохранить пользователей: " + e.getMessage());
        }
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(64);

            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 недоступен", e);
        }
    }
}