package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public abstract class BaseCommand implements Command {

    protected final CommandContext ctx;

    protected BaseCommand(CommandContext ctx) {
        this.ctx = ctx;
    }

    // ввод
    protected String prompt(String question) {
        System.out.print(question + ": ");
        return ctx.getScanner().nextLine().trim();
    }

    protected long parseLong(String raw, String argName) throws ValidationException {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    "Ошибка: " + argName + " должен быть целым числом, получено: '" + raw + "'");
        }
    }

    protected boolean requireTokens(String[] tokens, int min) {
        if (tokens.length >= min) return true;
        System.out.println("Использование: " + usage());
        return false;
    }

    protected boolean requireLogin() {
        if (!ctx.getUserService().isLoggedIn()) {
            System.out.println("Ошибка: команда доступна только авторизованным пользователям. Выполните login.");
            return false;
        }
        return true;
    }


    protected void printHeader(String fmt, Object... args) {
        System.out.printf(fmt + "%n", args);
    }

    protected void printDivider(int len) {
        System.out.println("-".repeat(len));
    }

    protected String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
