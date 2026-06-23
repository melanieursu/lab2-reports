package ru.itmo.TolstovaUrsu.domain;

public enum MeasurementParam {
    PH,
    CONDUCTIVITY,
    TURBIDITY,
    NITRATE;

    public static MeasurementParam fromString(String value) {
        try {
            return MeasurementParam.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
