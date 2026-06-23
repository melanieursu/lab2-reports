package ru.itmo.TolstovaUrsu;

import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.cli.CommandInterpreter;
import ru.itmo.TolstovaUrsu.db.DatabaseManager;
import ru.itmo.TolstovaUrsu.db.DbConfig;
import ru.itmo.TolstovaUrsu.db.PostgresStorage;
import ru.itmo.TolstovaUrsu.service.ReportService;
import ru.itmo.TolstovaUrsu.storage.AppStorage;
import ru.itmo.TolstovaUrsu.ui.LabApp;
import ru.itmo.TolstovaUrsu.ui.ServiceLocator;

import java.util.Scanner;

public class Main {

    /** Путь к файлу конфигурации БД. */
    private static final String DB_CONFIG = "db.cfg";

    public static void main(String[] args) {
        boolean cliMode = false;
        String autoLoad = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cli" -> cliMode = true;
                case "--load" -> {
                    if (i + 1 < args.length) {
                        autoLoad = args[++i];
                    }
                }
            }
        }

        /*
         * Пользователи больше не загружаются из users.csv.
         * В PostgreSQL-режиме они должны храниться только в таблице users.
         */
        UserService userService = new UserService();

        ReportService service = new ReportService();
        service.setUserService(userService);

        /*
         * AppStorage оставлен только для совместимости старого кода / XML-функций.
         * Основное хранение выполняется через PostgreSQL.
         */
        AppStorage storage = new AppStorage(service);

        DatabaseManager dbManager = null;

        try {
            // ─── PostgreSQL обязателен ─────────────────────────
            DbConfig cfg = DbConfig.load(DB_CONFIG);
            dbManager = new DatabaseManager(cfg);

            boolean connected = dbManager.connect();

            if (!connected) {
                System.err.println("[Main] Не удалось подключиться к PostgreSQL.");
                System.err.println("[Main] Проверьте, что сервер PostgreSQL запущен.");
                System.err.println("[Main] Также проверьте настройки db.cfg или переменные окружения.");
                return;
            }

            PostgresStorage pgStorage = new PostgresStorage(dbManager, userService);

            pgStorage.ensureSchema();

            /*
             * Важно:
             * UserService должен знать про PostgreSQL,
             * чтобы регистрация сохраняла пользователей в таблицу users,
             * а не в users.csv.
             */
            userService.setPgStorage(pgStorage);

            // Загружаем пользователей из таблицы users.
            pgStorage.loadUsers();

            // Подключаем PostgreSQL-хранилище к сервису отчётов.
            service.setPgStorage(pgStorage);

            // Загружаем отчёты и строки отчётов из PostgreSQL в память.
            service.loadFromDatabase();

            System.out.println("[Main] Работаем в режиме PostgreSQL.");

            // ─── ServiceLocator для JavaFX ─────────────────────
            ServiceLocator.init(service, storage, userService);

            /*
             * XML-загрузка отключена в PostgreSQL-режиме,
             * чтобы не смешивать файловое и серверное хранилище.
             */
            if (autoLoad != null) {
                System.err.println("[Main] Параметр --load отключён в режиме PostgreSQL.");
                System.err.println("[Main] Данные должны загружаться из базы данных.");
            }

            // ─── Запуск CLI или JavaFX ─────────────────────────
            if (cliMode) {
                Scanner scanner = new Scanner(System.in);
                CommandInterpreter interpreter =
                        new CommandInterpreter(service, storage, scanner, userService);
                interpreter.run();
                scanner.close();
            } else {
                LabApp.startUI(args);
            }

        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }
}