package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepUpdateLineCommand extends BaseCommand {

    public RepUpdateLineCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_updateline"; }
    @Override public String usage() {
        return "rep_updateline <line_id> field=value [field=value ...]  (поля: param, value, unit)";
    }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 3)) return;
        if (!requireLogin()) return;

        long lineId = parseLong(tokens[1], "line_id");

        for (int i = 2; i < tokens.length; i++) {
            String[] pair = tokens[i].split("=", 2);
            if (pair.length < 2) {
                System.out.println("Ошибка: неверный формат '" + tokens[i]
                        + "', ожидается field=value");
                return;
            }
            ctx.getService().updateLine(lineId, pair[0], pair[1]);
        }
        System.out.println("OK");
    }
}
