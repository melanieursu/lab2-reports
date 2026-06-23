package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.domain.ReportLine;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

import java.util.List;

public class RepExportCommand extends BaseCommand {

    public RepExportCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_export"; }
    @Override public String usage() { return "rep_export <report_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;

        long   reportId = parseLong(tokens[1], "report_id");
        Report report   = ctx.getService().getById(reportId)
                .orElseThrow(() -> new ValidationException(
                        "Ошибка: отчёт с id=" + reportId + " не найден"));
        List<ReportLine> lines = ctx.getService().getLinesForReport(reportId);

        System.out.println("========================================");
        System.out.println("ОТЧЁТ ПО АНАЛИЗАМ");
        System.out.println("========================================");
        System.out.println("ID:       " + report.getId());
        System.out.println("Название: " + report.getName());
        System.out.println("Статус:   " + report.getStatus());
        if (report.getSampleId() > 0)
            System.out.println("Образец:  " + report.getSampleId());
        if (report.getExperimentId() > 0)
            System.out.println("Эксп-т:   " + report.getExperimentId());
        System.out.println("Автор:    " + report.getOwnerUsername());
        if (report.getSignedBy() != null)
            System.out.println("Подписан: " + report.getSignedBy());
        System.out.println("Создан:   " + report.getCreatedAt());
        System.out.println("----------------------------------------");
        System.out.printf("%-6s %-15s %-12s %-10s%n",
                "ID", "Параметр", "Значение", "Единицы");
        System.out.println("----------------------------------------");
        for (ReportLine l : lines) {
            System.out.printf("%-6d %-15s %-12.4f %-10s%n",
                    l.getId(), l.getParam(), l.getValue(), l.getUnit());
        }
        System.out.println("========================================");
    }
}
