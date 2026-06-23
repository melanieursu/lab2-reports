package ru.itmo.TolstovaUrsu.db;

import java.io.*;
import java.util.Properties;

/**
 * Читает параметры подключения к БД из файла конфигурации (db.cfg)
 * или переменных окружения.
 *
 * <p>Порядок приоритета:
 * <ol>
 *   <li>Переменные окружения: {@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}.</li>
 *   <li>Файл конфигурации (путь задаётся при создании, по умолчанию {@code db.cfg}).</li>
 * </ol>
 */
public final class DbConfig {

    private final String url;
    private final String user;
    private final String password;

    private DbConfig(String url, String user, String password) {
        this.url      = url;
        this.user     = user;
        this.password = password;
    }

    public String getUrl()      { return url; }
    public String getUser()     { return user; }
    public String getPassword() { return password; }

    /** Создаёт конфигурацию, используя переменные окружения с fallback на файл. */
    public static DbConfig load(String configFilePath) {
        // 1) попробуем ENV-переменные
        String envUrl  = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        if (envUrl != null && envUser != null && envPass != null) {
            return new DbConfig(envUrl.trim(), envUser.trim(), envPass.trim());
        }

        // 2) fallback — файл
        Properties props = new Properties();
        File file = new File(configFilePath);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
            } catch (IOException e) {
                System.err.println("[DbConfig] Не удалось прочитать " + configFilePath
                        + ": " + e.getMessage());
            }
        } else {
            // создаём шаблон если файла нет
            createTemplate(file);
        }

        String url  = props.getProperty("db.url",      "jdbc:postgresql://localhost:5433/lab3db");
        String user = props.getProperty("db.user",     "postgres");
        String pass = props.getProperty("db.password", "");
        return new DbConfig(url.trim(), user.trim(), pass.trim());
    }

    private static void createTemplate(File file) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Конфигурация подключения к PostgreSQL");
            pw.println("# Можно переопределить переменными окружения: DB_URL, DB_USER, DB_PASSWORD");
            pw.println("db.url=jdbc:postgresql://localhost:5433/lab3db");
            pw.println("db.user=postgres");
            pw.println("db.password=secret");
            System.out.println("[DbConfig] Создан шаблон конфигурации: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[DbConfig] Не удалось создать шаблон: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "DbConfig{url='" + url + "', user='" + user + "'}";
    }
}
