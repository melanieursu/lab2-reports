package ru.itmo.TolstovaUrsu.domain;

import java.time.Instant;

/**
 * Снимок состояния строки отчёта (ReportLine) на определённый момент времени.
 * Используется для построения визуальной истории изменений объекта.
 */
public final class ReportLineHistory {

    private final long id;
    private final long lineId;
    private final long reportId;
    private final String action;       // INSERT / UPDATE / DELETE
    private final MeasurementParam param;
    private final double value;
    private final String unit;
    private final Instant changedAt;

    public ReportLineHistory(long id, long lineId, long reportId, String action,
                              MeasurementParam param, double value, String unit,
                              Instant changedAt) {
        this.id = id;
        this.lineId = lineId;
        this.reportId = reportId;
        this.action = action;
        this.param = param;
        this.value = value;
        this.unit = unit;
        this.changedAt = changedAt;
    }

    public long getId() { return id; }
    public long getLineId() { return lineId; }
    public long getReportId() { return reportId; }
    public String getAction() { return action; }
    public MeasurementParam getParam() { return param; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public Instant getChangedAt() { return changedAt; }

    @Override
    public String toString() {
        return "ReportLineHistory{lineId=" + lineId + ", action='" + action
                + "', param=" + param + ", value=" + value + ", unit='" + unit
                + "', changedAt=" + changedAt + "}";
    }
}
