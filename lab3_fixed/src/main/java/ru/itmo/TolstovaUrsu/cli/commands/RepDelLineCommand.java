package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepDelLineCommand extends BaseCommand {

    public RepDelLineCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_delline"; }
    @Override public String usage() { return "rep_delline <line_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        long lineId = parseLong(tokens[1], "line_id");
        ctx.getService().deleteLine(lineId);
        System.out.println("OK deleted");
    }
}
