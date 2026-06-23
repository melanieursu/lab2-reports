package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.auth.User;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

/**
 * Команда {@code login} — вход в систему.
 *
 * <p>После успешного входа «текущий пользователь» запоминается в сессии ({@link ru.itmo.TolstovaUrsu.auth.UserService}).
 * Все write-команды будут доступны только авторизованному пользователю.
 */
public class LoginCommand extends BaseCommand {

    public LoginCommand(CommandContext ctx) { super(ctx); }

    @Override public String name()  { return "login"; }
    @Override public String usage() { return "login  — войти в систему"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (ctx.getUserService().isLoggedIn()) {
            System.out.println("Вы уже вошли как '"
                    + ctx.getUserService().getCurrentUser().getLogin()
                    + "'. Используйте 'logout' для выхода.");
            return;
        }
        String login    = prompt("Логин");
        String password = prompt("Пароль");
        try {
            User user = ctx.getUserService().login(login, password);
            System.out.println("Добро пожаловать, " + user.getLogin() + "!");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка входа: " + e.getMessage());
        }
    }
}
