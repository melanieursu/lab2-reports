package ru.itmo.TolstovaUrsu.validation;

import java.util.Set; 

public class ReportValidator {

    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_UNIT_LENGTH = 16;

    private static final Set<String> ALLOWED_UNITS = Set.of(
            "pH", "mS/cm", "NTU", "mg/L"
    );

    public void validateName(String name) throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Ошибка: название отчёта не может быть пустым");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    "Ошибка: название слишком длинное (макс. " + MAX_NAME_LENGTH + ")");
        }
    }

    public void validateUnit(String unit) throws ValidationException {
        if (unit == null || unit.isBlank()) {
            throw new ValidationException("Ошибка: единицы измерения не могут быть пустыми");
        }
        if (unit.length() > MAX_UNIT_LENGTH) {
            throw new ValidationException(
                    "Ошибка: единицы слишком длинные (макс. " + MAX_UNIT_LENGTH + ")");
        }

        // trim убирает случайные пробелы по краям
        if (!ALLOWED_UNITS.contains(unit.trim())) {
            throw new ValidationException(
                    "Ошибка: недопустимые единицы измерения '" + unit + "'. " +
                            "Разрешены только: pH / mS/cm / NTU / mg/L"
            );
        }
    }

    public double parseValue(String raw) throws ValidationException {
        try {
            double val = Double.parseDouble(raw);
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                throw new ValidationException("Ошибка: значение должно быть обычным числом");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    "Ошибка: значение должно быть числом, получено: '" + raw + "'");
        }
    }
}

