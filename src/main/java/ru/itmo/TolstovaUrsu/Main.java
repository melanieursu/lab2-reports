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

    /** Путь к файлу пользователей (файловый режим). */
    private static final String USERS_FILE = "users.csv";

    /** Путь к файлу конфигурации БД. */
    private static final String DB_CONFIG  = "db.cfg";

    public static void main(String[] args) {
        boolean cliMode  = false;
        boolean dbMode   = false;
        String  autoLoad = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cli"  -> cliMode  = true;
                case "--db"   -> dbMode   = true;
                case "--load" -> { if (i + 1 < args.length) autoLoad = args[++i]; }
            }
        }

        // ─── UserService (файловое хранение пользователей) ────
        UserService userService = new UserService(USERS_FILE);

        // ─── ReportService ────────────────────────────────────
        ReportService service = new ReportService();
        service.setUserService(userService);

        // ─── Storage ──────────────────────────────────────────
        AppStorage storage = new AppStorage(service);

        // ─── PostgreSQL (опционально: --db или если db.cfg/ENV есть) ──
        DatabaseManager dbManager = null;
        if (dbMode || dbConfigExists()) {
            DbConfig cfg = DbConfig.load(DB_CONFIG);
            dbManager = new DatabaseManager(cfg);
            boolean connected = dbManager.connect();
            if (connected) {
                PostgresStorage pgStorage = new PostgresStorage(dbManager, userService);
                pgStorage.ensureSchema();
                pgStorage.loadUsers();          // подгружаем пользователей из БД
                service.setPgStorage(pgStorage);
                service.loadFromDatabase();     // подгружаем отчёты/строки из БД
                System.out.println("[Main] Работаем в режиме PostgreSQL.");
            } else {
                System.err.println("[Main] Не удалось подключиться к БД. "
                        + "Работаем в файловом режиме.");
            }
        }

        // ─── ServiceLocator для JavaFX ────────────────────────
        ServiceLocator.init(service, storage, userService);

        // ─── Авто-загрузка XML (только файловый режим) ────────
        if (autoLoad != null && dbManager == null) {
            try {
                storage.load(autoLoad);
                System.out.println("Данные загружены из " + autoLoad);
            } catch (Exception e) {
                System.err.println("Не удалось загрузить файл: " + e.getMessage());
            }
        }

        // ─── Запуск ───────────────────────────────────────────
        if (cliMode) {
            Scanner scanner = new Scanner(System.in);
            CommandInterpreter interpreter =
                    new CommandInterpreter(service, storage, scanner, userService);
            interpreter.run();
            scanner.close();
        } else {
            LabApp.startUI(args);
        }

        // ─── Завершение ───────────────────────────────────────
        if (dbManager != null) dbManager.close();
    }

    /** Проверяет наличие db.cfg или нужных ENV-переменных. */
    private static boolean dbConfigExists() {
        if (System.getenv("DB_URL") != null) return true;
        return new java.io.File(DB_CONFIG).exists();
    }
}
