package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class LogoutCommand extends BaseCommand {

    public LogoutCommand(CommandContext ctx) { super(ctx); }

    @Override public String name()  { return "logout"; }
    @Override public String usage() { return "logout  — выйти из системы"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!ctx.getUserService().isLoggedIn()) {
            System.out.println("Вы не авторизованы.");
            return;
        }
        String name = ctx.getUserService().getCurrentUser().getLogin();
        ctx.getUserService().logout();
        System.out.println("До свидания, " + name + "!");
    }
}
