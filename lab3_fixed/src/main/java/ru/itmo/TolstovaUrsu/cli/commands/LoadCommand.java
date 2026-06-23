package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class LoadCommand extends BaseCommand {

    public LoadCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "load"; }
    @Override public String usage() { return "load <path>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        String path = tokens[1];
        try {
            ctx.getStorage().load(path);
            System.out.println("OK загружено из " + path);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
