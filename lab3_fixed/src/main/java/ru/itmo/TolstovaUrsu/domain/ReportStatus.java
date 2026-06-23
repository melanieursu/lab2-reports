package ru.itmo.TolstovaUrsu.domain;

public enum ReportStatus {
    DRAFT,
    FINAL,
    SIGNED;

    public static ReportStatus fromString(String value) {
        try {
            return ReportStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
