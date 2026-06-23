package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepSignCommand extends BaseCommand {

    public RepSignCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_sign"; }
    @Override public String usage() { return "rep_sign <report_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        // подписывает текущий авторизованный пользователь
        String signer = ctx.getUserService().isLoggedIn()
                ? ctx.getUserService().getCurrentUser().getLogin()
                : "SYSTEM";

        long reportId = parseLong(tokens[1], "report_id");
        ctx.getService().signReport(reportId, signer);
        System.out.println("OK report " + reportId + " SIGNED by " + signer);
    }
}
