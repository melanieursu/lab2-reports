package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

public class RepCreateSampleCommand extends BaseCommand {

    public RepCreateSampleCommand(CommandContext ctx) {
        super(ctx);
    }

    @Override public String name()  { return "rep_create_sample"; }
    @Override public String usage() { return "rep_create_sample <sample_id>"; }

    @Override
    public void execute(String[] tokens) throws ValidationException {
        if (!requireTokens(tokens, 2)) return;
        if (!requireLogin()) return;

        long   sampleId = parseLong(tokens[1], "sample_id");
        String name     = prompt("Название отчёта");

        Report report = ctx.getService().createReportBySample(name, sampleId);
        System.out.println("OK report_id=" + report.getId());
    }
}
