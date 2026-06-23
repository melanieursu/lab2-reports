package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

/**
 * Команда {@code register} — регистрация нового пользователя.
 *
 * <p>Запрашивает логин и пароль, проверяет что логин не занят,
 * хэширует пароль (SHA-256) и сохраняет в файл пользователей.
 */
public class RegisterCommand extends BaseCommand {

    public RegisterCommand(CommandContext ctx) { super(ctx); }

    @Override public String name()  { return "register"; }
    @Override public String usage() { return "register  — зарегистрировать нового пользователя"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        String login    = prompt("Введите логин");
        String password = prompt("Введите пароль");

        try {
            ctx.getUserService().register(login, password);
            System.out.println("Пользователь '" + login + "' успешно зарегистрирован.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка регистрации: " + e.getMessage());
        }
    }
}
