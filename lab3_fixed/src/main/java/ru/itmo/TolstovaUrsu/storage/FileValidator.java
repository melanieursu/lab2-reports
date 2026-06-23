package ru.itmo.TolstovaUrsu.storage;

import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.domain.ReportLine;
import ru.itmo.TolstovaUrsu.storage.XmlFileStorage.LoadResult;

import java.util.*;


public class FileValidator {

    public static class FileValidationException extends Exception {
        public FileValidationException(String message) {
            super(message);
        }
    }

    public void validate(LoadResult result) throws FileValidationException {
        List<String> errors = new ArrayList<>();

        Set<Long> reportIds = new HashSet<>();
        Set<Long> lineIds   = new HashSet<>();

        for (Report r : result.reports) {
            if (reportIds.contains(r.getId()))
                errors.add("Дублирующийся id отчёта: " + r.getId());
            reportIds.add(r.getId());

            if (r.getName() == null || r.getName().isBlank())
                errors.add("Отчёт id=" + r.getId() + ": поле name пустое");
            else if (r.getName().length() > 128)
                errors.add("Отчёт id=" + r.getId() + ": name длиннее 128 символов");

            if (r.getStatus() == null)
                errors.add("Отчёт id=" + r.getId() + ": статус некорректный");

            if (r.getOwnerUsername() == null || r.getOwnerUsername().isBlank())
                errors.add("Отчёт id=" + r.getId() + ": поле ownerUsername пустое");
        }

        for (ReportLine l : result.lines) {
            if (lineIds.contains(l.getId()))
                errors.add("Дублирующийся id строки: " + l.getId());
            lineIds.add(l.getId());

            if (!reportIds.contains(l.getReportId()))
                errors.add("Строка id=" + l.getId()
                        + ": reportId=" + l.getReportId()
                        + " ссылается на несуществующий отчёт");

            if (Double.isNaN(l.getValue()) || Double.isInfinite(l.getValue()))
                errors.add("Строка id=" + l.getId() + ": некорректное значение value");

            if (l.getUnit() == null || l.getUnit().isBlank())
                errors.add("Строка id=" + l.getId() + ": поле unit пустое");

            if (l.getParam() == null)
                errors.add("Строка id=" + l.getId() + ": param некорректный");
        }

        if (!errors.isEmpty()) {
            throw new FileValidationException(
                    "Ошибка загрузки файла (" + errors.size() + " проблем):\n"
                            + String.join("\n", errors));
        }
    }
}
