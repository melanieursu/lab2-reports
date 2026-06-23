package ru.itmo.TolstovaUrsu.service;

import ru.itmo.TolstovaUrsu.auth.User;
import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.db.DatabaseManager;
import ru.itmo.TolstovaUrsu.db.PostgresStorage;
import ru.itmo.TolstovaUrsu.domain.MeasurementParam;
import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.domain.ReportLine;
import ru.itmo.TolstovaUrsu.domain.ReportStatus;
import ru.itmo.TolstovaUrsu.validation.ReportValidator;
import ru.itmo.TolstovaUrsu.validation.ValidationException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReportService {

    private final ArrayList<Report>     reports     = new ArrayList<>();
    private final ArrayList<ReportLine> reportLines = new ArrayList<>();

    private final ReportValidator validator = new ReportValidator();

    private long nextReportId = 1;
    private long nextLineId   = 1;

    // зависимости — инжектируются после создания
    private UserService     userService;
    private PostgresStorage pgStorage;
    private boolean         dbEnabled = false;

    public void setUserService(UserService us)  { this.userService = us; }
    public void setPgStorage(PostgresStorage pg) {
        this.pgStorage = pg;
        this.dbEnabled = true;
    }


    public Report createReportBySample(String name, long sampleId) throws ValidationException {
        requireAuth();
        validator.validateName(name);
        String owner = currentLogin();
        Report report = new Report(nextReportId, name, sampleId, 0, owner);

        if (dbEnabled) {
            try {
                ensureCurrentUserExistsInDb();

                long dbId = pgStorage.insertReport(report);
                report = new Report(dbId, name, sampleId, 0, owner);
                nextReportId = dbId + 1;
            } catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        } else {
            nextReportId++;
        }
        reports.add(report);
        return report;
    }

    public Report createReportByExperiment(String name, long experimentId) throws ValidationException {
        requireAuth();
        validator.validateName(name);
        String owner = currentLogin();
        Report report = new Report(nextReportId, name, 0, experimentId, owner);

        if (dbEnabled) {
            try {
                ensureCurrentUserExistsInDb();
                long dbId = pgStorage.insertReport(report);
                report = new Report(dbId, name, 0, experimentId, owner);
                nextReportId = dbId + 1;
            } catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        } else {
            nextReportId++;
        }
        reports.add(report);
        return report;
    }

    public Optional<Report> getById(long id) {
        return reports.stream().filter(r -> r.getId() == id).findFirst();
    }

    public List<Report> listReports(ReportStatus statusFilter) {
        return reports.stream()
                .filter(r -> statusFilter == null || r.getStatus() == statusFilter)
                .collect(Collectors.toList());
    }

    public List<ReportLine> getLinesForReport(long reportId) throws ValidationException {
        requireReport(reportId);
        return reportLines.stream()
                .filter(l -> l.getReportId() == reportId)
                .collect(Collectors.toList());
    }

    public Optional<ReportLine> getLineById(long lineId) {
        return reportLines.stream().filter(l -> l.getId() == lineId).findFirst();
    }

    public long countLinesForReport(long reportId) {
        return reportLines.stream().filter(l -> l.getReportId() == reportId).count();
    }

    public List<Report>     getAllReports() { return new ArrayList<>(reports);     }
    public List<ReportLine> getAllLines()   { return new ArrayList<>(reportLines); }

    public List<Report> getMyReports() {
        String login = currentLogin();
        return reports.stream()
                .filter(r -> login.equalsIgnoreCase(r.getOwnerUsername()))
                .collect(Collectors.toList());
    }

    public void finalizeReport(long reportId) throws ValidationException {
        requireAuth();
        Report report = requireReport(reportId);
        requireOwnership(report);
        if (report.getStatus() == ReportStatus.FINAL)
            throw new ValidationException("Ошибка: отчёт уже FINAL");
        if (report.getStatus() == ReportStatus.SIGNED)
            throw new ValidationException("Ошибка: нельзя изменить подписанный отчёт");
        report.setStatus(ReportStatus.FINAL);
        syncReportUpdate(report);
    }

    public void signReport(long reportId, String signerUsername) throws ValidationException {
        requireAuth();
        Report report = requireReport(reportId);
        if (report.getStatus() == ReportStatus.DRAFT)
            throw new ValidationException("Ошибка: сначала выполните finalize отчёта");
        if (report.getStatus() == ReportStatus.SIGNED)
            throw new ValidationException("Ошибка: отчёт уже подписан");
        report.setStatus(ReportStatus.SIGNED);
        report.setSignedBy(signerUsername);
        syncReportUpdate(report);
    }

    public ReportLine addLine(long reportId, MeasurementParam param,
                              double value, String unit) throws ValidationException {
        requireAuth();
        Report report = requireReport(reportId);
        requireOwnership(report);
        if (report.getStatus() != ReportStatus.DRAFT)
            throw new ValidationException(
                    "Ошибка: нельзя добавлять строки в отчёт со статусом " + report.getStatus());
        validator.validateUnit(unit);

        ReportLine line = new ReportLine(nextLineId, reportId, param, value, unit);

        if (dbEnabled) {
            try {
                long dbId = pgStorage.insertLine(line);
                line = new ReportLine(dbId, reportId, param, value, unit);
                nextLineId = dbId + 1;
            } catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        } else {
            nextLineId++;
        }
        reportLines.add(line);
        return line;
    }

    public void updateLine(long lineId, String field, String rawValue) throws ValidationException {
        requireAuth();
        ReportLine line   = requireLine(lineId);
        Report     report = requireReport(line.getReportId());
        requireOwnership(report);

        if (report.getStatus() != ReportStatus.DRAFT)
            throw new ValidationException(
                    "Ошибка: нельзя изменять строки отчёта со статусом " + report.getStatus());

        switch (field.toLowerCase()) {
            case "param" -> {
                MeasurementParam param = MeasurementParam.fromString(rawValue);
                if (param == null)
                    throw new ValidationException(
                            "Ошибка: неизвестный параметр '" + rawValue
                                    + "'. Доступны: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
                line.setParam(param);
            }
            case "value" -> line.setValue(validator.parseValue(rawValue));
            case "unit"  -> { validator.validateUnit(rawValue); line.setUnit(rawValue); }
            default      -> throw new ValidationException(
                    "Ошибка: нельзя менять поле '" + field + "'. Разрешены: param, value, unit");
        }

        if (dbEnabled) {
            try { pgStorage.updateLine(line); }
            catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        }
    }

    public void deleteLine(long lineId) throws ValidationException {
        requireAuth();
        ReportLine line   = requireLine(lineId);
        Report     report = requireReport(line.getReportId());
        requireOwnership(report);

        if (report.getStatus() != ReportStatus.DRAFT)
            throw new ValidationException(
                    "Ошибка: нельзя удалять строки отчёта со статусом " + report.getStatus());

        if (dbEnabled) {
            try { pgStorage.deleteLine(lineId); }
            catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        }
        reportLines.remove(line);
    }

    public void deleteReport(long reportId) throws ValidationException {
        requireAuth();
        Report report = requireReport(reportId);
        requireOwnership(report);

        if (dbEnabled) {
            try { pgStorage.deleteReport(reportId); }
            catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        }
        reportLines.removeIf(l -> l.getReportId() == reportId);
        reports.remove(report);
    }

    public void loadFromDatabase() {
        if (!dbEnabled) return;

        List<Report> dbReports = pgStorage.loadAllReports();
        List<ReportLine> dbLines = pgStorage.loadAllLines();
        replaceAll(dbReports, dbLines);
    }

    public void replaceAll(List<Report> newReports, List<ReportLine> newLines) {
        reports.clear();
        reportLines.clear();
        long maxRid = 0, maxLid = 0;
        for (Report r : newReports) {
            reports.add(r);
            if (r.getId() > maxRid) maxRid = r.getId();
        }
        for (ReportLine l : newLines) {
            reportLines.add(l);
            if (l.getId() > maxLid) maxLid = l.getId();
        }
        nextReportId = maxRid + 1;
        nextLineId   = maxLid + 1;
    }

    public List<ru.itmo.TolstovaUrsu.domain.ReportLineHistory> getLineHistory(long lineId) throws ValidationException {
        requireLine(lineId);
        if (!dbEnabled) return new ArrayList<>();
        return pgStorage.loadLineHistory(lineId);
    }

    public ReportValidator getValidator() { return validator; }

    private void syncReportUpdate(Report r) throws ValidationException {
        if (dbEnabled) {
            try { pgStorage.updateReport(r); }
            catch (SQLException e) {
                throw new ValidationException("Ошибка БД: " + DatabaseManager.friendlyMessage(e));
            }
        }
    }

    private void requireAuth() throws ValidationException {
        if (userService != null && !userService.isLoggedIn())
            throw new ValidationException("Ошибка: необходима авторизация. Используйте команду 'login'.");
    }

    private void requireOwnership(Report report) throws ValidationException {
        if (userService == null) return;
        User current = userService.getCurrentUser();
        if (current == null) return;
        if (!current.getLogin().equalsIgnoreCase(report.getOwnerUsername()))
            throw new ValidationException(
                    "Ошибка: у вас нет прав на изменение этого объекта (владелец: "
                            + report.getOwnerUsername() + ").");
    }

    private void ensureCurrentUserExistsInDb() throws ValidationException {
        if (!dbEnabled) return;

        User current = userService.getCurrentUser();

        if (current == null) {
            throw new ValidationException("Ошибка: необходима авторизация.");
        }

        try {
            pgStorage.upsertUser(current);
        } catch (SQLException e) {
            throw new ValidationException(
                    "Ошибка БД при сохранении пользователя: "
                            + DatabaseManager.friendlyMessage(e)
            );
        }
    }

    private String currentLogin() {
        if (userService != null && userService.isLoggedIn())
            return userService.getCurrentUser().getLogin();
        return "SYSTEM";
    }

    private Report requireReport(long reportId) throws ValidationException {
        return getById(reportId)
                .orElseThrow(() -> new ValidationException(
                        "Ошибка: отчёт с id=" + reportId + " не найден"));
    }

    private ReportLine requireLine(long lineId) throws ValidationException {
        return getLineById(lineId)
                .orElseThrow(() -> new ValidationException(
                        "Ошибка: строка с id=" + lineId + " не найдена"));
    }
}
