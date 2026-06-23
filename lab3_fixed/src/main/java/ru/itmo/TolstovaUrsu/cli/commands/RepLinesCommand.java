package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.domain.ReportLine;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

import java.util.List;

public class RepLinesCommand extends BaseCommand {

    public RepLinesCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_lines"; }
    @Override public String usage() { return "rep_lines <report_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;

        long             reportId = parseLong(tokens[1], "report_id");
        List<ReportLine> lines    = ctx.getService().getLinesForReport(reportId);

        if (lines.isEmpty()) {
            System.out.println("Строк в отчёте нет.");
            return;
        }

        printHeader("%-6s %-15s %-12s %-10s", "ID", "Param", "Value", "Unit");
        printDivider(47);
        for (ReportLine l : lines) {
            System.out.printf("%-6d %-15s %-12.4f %-10s%n",
                    l.getId(), l.getParam(), l.getValue(), l.getUnit());
        }
    }
}
