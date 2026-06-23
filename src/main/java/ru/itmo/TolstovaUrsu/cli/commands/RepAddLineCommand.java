package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.domain.MeasurementParam;
import ru.itmo.TolstovaUrsu.domain.ReportLine;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepAddLineCommand extends BaseCommand {

    public RepAddLineCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_addline"; }
    @Override public String usage() { return "rep_addline <report_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        long reportId = parseLong(tokens[1], "report_id");

        String paramRaw = prompt("Параметр (PH/CONDUCTIVITY/TURBIDITY/NITRATE)");
        MeasurementParam param = MeasurementParam.fromString(paramRaw);
        if (param == null) {
            throw new ValidationException(
                    "Ошибка: неизвестный параметр '" + paramRaw
                            + "'. Доступны: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
        }

        String valueRaw = prompt("Значение");
        double value    = ctx.getService().getValidator().parseValue(valueRaw);

        String unit = prompt("Единицы");

        ReportLine line = ctx.getService().addLine(reportId, param, value, unit);
        System.out.println("OK line_id=" + line.getId());
    }
}
