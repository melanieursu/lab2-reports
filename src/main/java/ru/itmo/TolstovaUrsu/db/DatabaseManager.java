package ru.itmo.TolstovaUrsu.db;

import java.sql.*;

/**
 * Менеджер подключения к PostgreSQL.
 * Инкапсулирует одно JDBC-соединение (простой вариант без пула).
 * При недоступности БД не падает — выводит сообщение и работает в деградированном режиме.
 */
public final class DatabaseManager {

    private final DbConfig config;
    private Connection connection;
    private boolean available = false;

    public DatabaseManager(DbConfig config) {
        this.config = config;
    }

    /** Пытается установить соединение. @return {@code true} если успешно. */
    public boolean connect() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                    config.getUrl(), config.getUser(), config.getPassword());
            connection.setAutoCommit(true);
            available = true;
            System.out.println("[DB] Подключено к " + config.getUrl());
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Драйвер PostgreSQL не найден (добавьте postgresql.jar в classpath).");
            available = false;
            return false;
        } catch (SQLException e) {
            System.err.println("[DB] Ошибка подключения: " + e.getMessage());
            available = false;
            return false;
        }
    }

    /** @return {@code true} если соединение активно. */
    public boolean isAvailable() {
        if (!available || connection == null) return false;
        try {
            return !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Возвращает соединение. Если оно разорвано — пробует переподключиться.
     * @throws SQLException если не удалось подключиться.
     */
    public Connection getConnection() throws SQLException {
        if (!isAvailable()) {
            if (!connect()) {
                throw new SQLException("База данных недоступна. Проверьте db.cfg / переменные окружения.");
            }
        }
        return connection;
    }

    /** Закрывает соединение. */
    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            available = false;
        }
    }

    // ─── SQL-утилиты ────────────────────────────────────────────

    /** Оборачивает распространённые SQL-ошибки в понятные сообщения. */
    public static String friendlyMessage(SQLException e) {
        String state = e.getSQLState() != null ? e.getSQLState() : "";
        return switch (state) {
            case "23505" -> "Нарушение уникальности: " + e.getMessage();
            case "23503" -> "Нарушение внешнего ключа: " + e.getMessage();
            case "23502" -> "Нарушение NOT NULL: " + e.getMessage();
            case "08001", "08006", "08003" -> "Ошибка соединения с БД: " + e.getMessage();
            default      -> "SQL-ошибка (" + state + "): " + e.getMessage();
        };
    }
}
