package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepFinalizeCommand extends BaseCommand {

    public RepFinalizeCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_finalize"; }
    @Override public String usage() { return "rep_finalize <report_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        long reportId = parseLong(tokens[1], "report_id");
        ctx.getService().finalizeReport(reportId);
        System.out.println("OK report " + reportId + " FINAL");
    }
}
