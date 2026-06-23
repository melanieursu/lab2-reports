package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.domain.ReportStatus;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

import java.util.List;

public class RepListCommand extends BaseCommand {

    public RepListCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_list"; }
    @Override public String usage() { return "rep_list [--status DRAFT|FINAL|SIGNED]"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        ReportStatus filter = parseStatusFilter(tokens);

        List<Report> list = ctx.getService().listReports(filter);
        if (list.isEmpty()) {
            System.out.println("Отчётов не найдено.");
            return;
        }

        printHeader("%-6s %-30s %-10s", "ID", "Name", "Status");
        printDivider(50);
        for (Report r : list) {
            System.out.printf("%-6d %-30s %-10s%n",
                    r.getId(), truncate(r.getName(), 30), r.getStatus());
        }
    }

    private ReportStatus parseStatusFilter(String[] tokens) throws ValidationException {
        for (int i = 1; i < tokens.length - 1; i++) {
            if ("--status".equalsIgnoreCase(tokens[i])) {
                ReportStatus status = ReportStatus.fromString(tokens[i + 1]);
                if (status == null) {
                    throw new ValidationException(
                            "Ошибка: неизвестный статус '" + tokens[i + 1]
                                    + "'. Используйте DRAFT, FINAL или SIGNED");
                }
                return status;
            }
        }
        return null;
    }
}
