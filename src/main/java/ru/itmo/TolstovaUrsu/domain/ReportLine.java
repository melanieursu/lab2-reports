package ru.itmo.TolstovaUrsu.domain;

import java.time.Instant;

public final class ReportLine {

    private long id;
    private long reportId;
    private MeasurementParam param;
    private double value;
    private String unit;
    private Instant createdAt;
    private Instant updatedAt;

    public ReportLine(long id, long reportId, MeasurementParam param,
                      double value, String unit) {
        this.id = id;
        this.reportId = reportId;
        this.param = param;
        this.value = value;
        this.unit = unit;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public long getId() { return id; }

    public long getReportId() { return reportId; }
    public void setReportId(long reportId) { this.reportId = reportId; this.updatedAt = Instant.now(); }

    public MeasurementParam getParam() { return param; }
    public void setParam(MeasurementParam param) { this.param = param; this.updatedAt = Instant.now(); }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; this.updatedAt = Instant.now(); }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; this.updatedAt = Instant.now(); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ReportLine{id=" + id + ", reportId=" + reportId
                + ", param=" + param + ", value=" + value + ", unit='" + unit + "'}";
    }
}
