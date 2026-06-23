package ru.itmo.TolstovaUrsu.cli;

import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.cli.commands.*;
import ru.itmo.TolstovaUrsu.service.ReportService;
import ru.itmo.TolstovaUrsu.storage.AppStorage;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

import java.util.*;

public class CommandInterpreter {

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Scanner scanner;

    public CommandInterpreter(ReportService service, AppStorage storage,
                               Scanner scanner, UserService userService) {
        this.scanner = scanner;
        registerCommands(new CommandContext(service, storage, scanner, userService));
    }

    private void registerCommands(CommandContext ctx) {
        // ─── авторизация ────────────────────────────
        register(new RegisterCommand(ctx));
        register(new LoginCommand(ctx));
        register(new LogoutCommand(ctx));
        register(new WhoAmICommand(ctx));

        // ─── основные операции ──────────────────────
        register(new RepCreateSampleCommand(ctx));
        register(new RepAddLineCommand(ctx));
        register(new RepListCommand(ctx));
        register(new RepShowCommand(ctx));
        register(new RepLinesCommand(ctx));
        register(new RepUpdateLineCommand(ctx));
        register(new RepDelLineCommand(ctx));
        register(new RepFinalizeCommand(ctx));
        register(new RepSignCommand(ctx));
        register(new RepExportCommand(ctx));
        register(new SaveCommand(ctx));
        register(new LoadCommand(ctx));
    }

    private void register(Command command) {
        commands.put(command.name(), command);
    }

    public void run() {
        System.out.println("Система отчётов по анализам (ЛР2, предметная область 6)");
        System.out.println("Введите 'help' для списка команд, 'exit' для выхода.");
        System.out.println("Для работы с коллекцией выполните 'login' или 'register'.");
        System.out.println();

        while (true) {
            String prompt = scanner.hasNextLine() ? "" : null;
            if (prompt == null) break;

            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            String rawLine = scanner.nextLine().trim();
            if (rawLine.isBlank()) continue;

            String[] tokens = tokenize(rawLine);
            if (tokens.length == 0) continue;

            String commandName = tokens[0].toLowerCase();

            if ("exit".equals(commandName)) {
                System.out.println("AREVUAR!");
                return;
            }
            if ("help".equals(commandName)) {
                printHelp();
                continue;
            }

            Command command = commands.get(commandName);
            if (command == null) {
                System.out.println("Неизвестная команда: '" + commandName
                        + "'. Введите 'help' для списка команд.");
                continue;
            }

            try {
                command.execute(tokens);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: ожидалось числовое значение.");
            }
        }
    }

    private void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  === Авторизация ===");
        for (String name : List.of("register", "login", "logout", "whoami")) {
            Command cmd = commands.get(name);
            if (cmd != null) System.out.printf("  %-55s%n", cmd.usage());
        }
        System.out.println("  === Отчёты (требуется авторизация) ===");
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            if (!Set.of("register", "login", "logout", "whoami").contains(entry.getKey())) {
                System.out.printf("  %-55s%n", entry.getValue().usage());
            }
        }
        System.out.println("  help");
        System.out.println("  exit");
    }

    private String[] tokenize(String line) {
        List<String> tokens   = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes      = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }
}
