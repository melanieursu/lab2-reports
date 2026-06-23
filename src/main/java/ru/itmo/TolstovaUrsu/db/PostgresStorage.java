package ru.itmo.TolstovaUrsu.db;

import ru.itmo.TolstovaUrsu.auth.User;
import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.domain.*;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PostgresStorage {

    private final DatabaseManager db;
    private final UserService userService;

    public PostgresStorage(DatabaseManager db, UserService userService) {
        this.db          = db;
        this.userService = userService;
    }

    // ─────────────────── DDL (автосоздание схемы) ──────────────

    /**
     * Создаёт таблицы если их нет. Безопасно при повторных вызовах.
     */
    public void ensureSchema() {
        if (!db.isAvailable()) return;
        try {
            Connection con = db.getConnection();
            try (Statement st   = con.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id            SERIAL PRIMARY KEY,
                    login         TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reports (
                    id            BIGSERIAL PRIMARY KEY,
                    name          TEXT NOT NULL,
                    sample_id     BIGINT NOT NULL DEFAULT 0,
                    experiment_id BIGINT NOT NULL DEFAULT 0,
                    status        TEXT NOT NULL DEFAULT 'DRAFT',
                    owner_login   TEXT REFERENCES users(login) ON UPDATE CASCADE,
                    signed_by     TEXT,
                    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS report_lines (
                    id         BIGSERIAL PRIMARY KEY,
                    report_id  BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
                    param      TEXT NOT NULL,
                    value      DOUBLE PRECISION NOT NULL,
                    unit       TEXT NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);

            System.out.println("[DB] Схема проверена / создана.");
            } catch (SQLException e) {
                System.err.println("[DB] Ошибка создания схемы: " + DatabaseManager.friendlyMessage(e));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Ошибка подключения при создании схемы: " + e.getMessage());
        }
    }

    // ─────────────────── USERS ─────────────────────────────────

    /** Сохраняет нового пользователя в БД. */
    public void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users(login, password_hash) VALUES(?,?)";
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.executeUpdate();
        }
    }

    /** Загружает всех пользователей из БД в UserService. */
    public void loadUsers() {
        if (!db.isAvailable()) return;
        String sql = "SELECT login, password_hash FROM users";
        try {
            Connection con = db.getConnection();
            try (Statement st   = con.createStatement();
                 ResultSet rs   = st.executeQuery(sql)) {
                while (rs.next()) {
                    String login = rs.getString("login");
                    String hash  = rs.getString("password_hash");
                    try {
                        userService.registerWithHash(login, hash);
                    } catch (IllegalArgumentException ignored) { /* уже есть */ }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Не удалось загрузить пользователей: "
                    + DatabaseManager.friendlyMessage(e));
        }
    }

    // ─────────────────── REPORTS ───────────────────────────────

    /**
     * Вставляет отчёт и возвращает сгенерированный БД id.
     */
    public long insertReport(Report r) throws SQLException {
        String sql = """
            INSERT INTO reports(name, sample_id, experiment_id, status, owner_login,
                                signed_by, created_at, updated_at)
            VALUES(?,?,?,?,?,?,?,?)
        """;
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getName());
            ps.setLong(2, r.getSampleId());
            ps.setLong(3, r.getExperimentId());
            ps.setString(4, r.getStatus().name());
            ps.setString(5, r.getOwnerUsername());
            ps.setString(6, r.getSignedBy());
            ps.setTimestamp(7, Timestamp.from(r.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.from(r.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Не удалось получить сгенерированный ID отчёта.");
    }

    /** Обновляет изменяемые поля отчёта. */
    public void updateReport(Report r) throws SQLException {
        String sql = """
            UPDATE reports SET name=?, sample_id=?, experiment_id=?, status=?,
                               signed_by=?, updated_at=?
            WHERE id=?
        """;
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getName());
            ps.setLong(2, r.getSampleId());
            ps.setLong(3, r.getExperimentId());
            ps.setString(4, r.getStatus().name());
            ps.setString(5, r.getSignedBy());
            ps.setTimestamp(6, Timestamp.from(r.getUpdatedAt()));
            ps.setLong(7, r.getId());
            ps.executeUpdate();
        }
    }

    /** Удаляет отчёт (строки удаляются каскадно). */
    public void deleteReport(long reportId) throws SQLException {
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM reports WHERE id=?")) {
            ps.setLong(1, reportId);
            ps.executeUpdate();
        }
    }

    /** Загружает все отчёты. */
    public List<Report> loadAllReports() {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY id";
        try {
            Connection con = db.getConnection();
            try (Statement st   = con.createStatement();
                 ResultSet rs   = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapReport(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Ошибка загрузки отчётов: " + DatabaseManager.friendlyMessage(e));
        }
        return list;
    }

    // ─────────────────── REPORT LINES ──────────────────────────

    /** Вставляет строку отчёта; возвращает сгенерированный id. */
    public long insertLine(ReportLine l) throws SQLException {
        String sql = """
            INSERT INTO report_lines(report_id, param, value, unit, created_at, updated_at)
            VALUES(?,?,?,?,?,?)
        """;
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, l.getReportId());
            ps.setString(2, l.getParam().name());
            ps.setDouble(3, l.getValue());
            ps.setString(4, l.getUnit());
            ps.setTimestamp(5, Timestamp.from(l.getCreatedAt()));
            ps.setTimestamp(6, Timestamp.from(l.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Не удалось получить сгенерированный ID строки.");
    }

    /** Обновляет строку отчёта. */
    public void updateLine(ReportLine l) throws SQLException {
        String sql = "UPDATE report_lines SET param=?, value=?, unit=?, updated_at=? WHERE id=?";
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, l.getParam().name());
            ps.setDouble(2, l.getValue());
            ps.setString(3, l.getUnit());
            ps.setTimestamp(4, Timestamp.from(l.getUpdatedAt()));
            ps.setLong(5, l.getId());
            ps.executeUpdate();
        }
    }

    /** Удаляет строку отчёта. */
    public void deleteLine(long lineId) throws SQLException {
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM report_lines WHERE id=?")) {
            ps.setLong(1, lineId);
            ps.executeUpdate();
        }
    }

    /** Загружает все строки. */
    public List<ReportLine> loadAllLines() {
        List<ReportLine> list = new ArrayList<>();
        String sql = "SELECT * FROM report_lines ORDER BY id";
        try {
            Connection con = db.getConnection();
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapLine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Ошибка загрузки строк: " + DatabaseManager.friendlyMessage(e));
        }
        return list;
    }

    // ─────────────────── маппинг ───────────────────────────────

    private Report mapReport(ResultSet rs) throws SQLException {
        long         id           = rs.getLong("id");
        String       name         = rs.getString("name");
        long         sampleId     = rs.getLong("sample_id");
        long         experimentId = rs.getLong("experiment_id");
        ReportStatus status       = ReportStatus.valueOf(rs.getString("status"));
        String       owner        = rs.getString("owner_login");
        String       signedBy     = rs.getString("signed_by");
        Instant      createdAt    = rs.getTimestamp("created_at").toInstant();
        Instant      updatedAt    = rs.getTimestamp("updated_at").toInstant();

        Report r = new Report(id, name, sampleId, experimentId, owner);
        r.setStatus(status);
        r.setSignedBy(signedBy);
        r.setCreatedAt(createdAt);
        r.setUpdatedAt(updatedAt);
        return r;
    }

    private ReportLine mapLine(ResultSet rs) throws SQLException {
        long             id        = rs.getLong("id");
        long             reportId  = rs.getLong("report_id");
        MeasurementParam param     = MeasurementParam.valueOf(rs.getString("param"));
        double           value     = rs.getDouble("value");
        String           unit      = rs.getString("unit");
        Instant          createdAt = rs.getTimestamp("created_at").toInstant();
        Instant          updatedAt = rs.getTimestamp("updated_at").toInstant();

        ReportLine l = new ReportLine(id, reportId, param, value, unit);
        l.setCreatedAt(createdAt);
        l.setUpdatedAt(updatedAt);
        return l;
    }

    public void upsertUser(User user) throws SQLException {
        String sql = """
        INSERT INTO users(login, password_hash)
        VALUES (?, ?)
        ON CONFLICT (login) DO UPDATE
        SET password_hash = EXCLUDED.password_hash
    """;

        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.executeUpdate();
        }
    }
}
