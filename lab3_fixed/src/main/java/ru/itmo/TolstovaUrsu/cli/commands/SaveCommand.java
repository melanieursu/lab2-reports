package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class SaveCommand extends BaseCommand {

    public SaveCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "save"; }
    @Override public String usage() { return "save <path>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        String path = tokens[1];
        try {
            ctx.getStorage().save(path);
            System.out.println("OK сохранено в " + path);
        } catch (Exception e) {
            System.out.println("Ошибка сохранения: " + e.getMessage());
        }
    }
}
