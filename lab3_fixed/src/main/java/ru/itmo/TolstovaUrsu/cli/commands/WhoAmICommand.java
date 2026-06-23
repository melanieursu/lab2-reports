package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class WhoAmICommand extends BaseCommand {

    public WhoAmICommand(CommandContext ctx) { super(ctx); }

    @Override public String name()  { return "whoami"; }
    @Override public String usage() { return "whoami  — показать текущего пользователя"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (ctx.getUserService().isLoggedIn()) {
            System.out.println("Вы вошли как: " + ctx.getUserService().getCurrentUser().getLogin());
        } else {
            System.out.println("Вы не авторизованы. Используйте 'login' для входа.");
        }
    }
}
