package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepShowCommand extends BaseCommand {

    public RepShowCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_show"; }
    @Override public String usage() { return "rep_show <report_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;

        long   reportId = parseLong(tokens[1], "report_id");
        Report report   = ctx.getService().getById(reportId)
                .orElseThrow(() -> new ValidationException(
                        "Ошибка: отчёт с id=" + reportId + " не найден"));

        long lineCount = ctx.getService().countLinesForReport(reportId);

        System.out.println("Report #" + report.getId());
        System.out.println("  name:       " + report.getName());
        System.out.println("  status:     " + report.getStatus());
        if (report.getSampleId() > 0)
            System.out.println("  sample_id:  " + report.getSampleId());
        if (report.getExperimentId() > 0)
            System.out.println("  exp_id:     " + report.getExperimentId());
        System.out.println("  owner:      " + report.getOwnerUsername());
        if (report.getSignedBy() != null)
            System.out.println("  signed_by:  " + report.getSignedBy());
        System.out.println("  lines:      " + lineCount);
        System.out.println("  created:    " + report.getCreatedAt());
    }
}
