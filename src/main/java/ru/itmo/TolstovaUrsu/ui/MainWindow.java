package ru.itmo.TolstovaUrsu.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.itmo.TolstovaUrsu.auth.User;
import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.domain.MeasurementParam;
import ru.itmo.TolstovaUrsu.domain.Report;
import ru.itmo.TolstovaUrsu.domain.ReportLine;
import ru.itmo.TolstovaUrsu.domain.ReportStatus;
import ru.itmo.TolstovaUrsu.service.ReportService;
import ru.itmo.TolstovaUrsu.storage.AppStorage;
import ru.itmo.TolstovaUrsu.validation.ValidationException;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MainWindow {

    private final ReportService service     = ServiceLocator.getService();
    private final AppStorage    storage     = ServiceLocator.getStorage();
    private final UserService   userService = ServiceLocator.getUserService();

    // master
    private final ObservableList<Report>     reportData = FXCollections.observableArrayList();
    private TableView<Report>                reportTable;

    // detail
    private final ObservableList<ReportLine> lineData   = FXCollections.observableArrayList();
    private TableView<ReportLine>            lineTable;

    // detail labels
    private Label lblName, lblStatus, lblSample, lblOwner, lblSigned, lblLines;

    // detail buttons
    private Button btnAddLine, btnEditLine, btnDelLine;
    private Button btnFinalize, btnSign;

    // status bar
    private Label statusBar;

    public Scene buildScene() {

        // ─── toolbar ────────────────────────────────────────────
        Button btnCreate  = new Button("➕ Новый отчёт");
        Button btnDelete  = new Button("🗑 Удалить");
        Button btnRefresh = new Button("🔄 Обновить");
        Button btnSave    = new Button("💾 Сохранить XML");
        Button btnLoad    = new Button("📂 Загрузить XML");

        btnDelete.setDisable(true);

        Label  lblUser    = new Label();
        Button btnLogout  = new Button("🚪 Выйти");
        refreshUserLabel(lblUser);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolbar = new ToolBar(
                btnCreate, btnDelete, new Separator(),
                btnRefresh, new Separator(),
                btnSave, btnLoad,
                spacer, lblUser, btnLogout);

        // ─── master (left) ──────────────────────────────────────
        reportTable = buildReportTable();
        reportTable.setItems(reportData);
        reportTable.setPlaceholder(new Label("Нет отчётов. Нажмите «Новый отчёт»."));
        VBox masterBox = new VBox(4, new Label("Отчёты:"), reportTable);
        masterBox.setPadding(new Insets(8));
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        // ─── detail (right) ────────────────────────────────────
        VBox detailBox = buildDetailPanel();

        // ─── split ─────────────────────────────────────────────
        SplitPane split = new SplitPane(masterBox, detailBox);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.38);

        statusBar = new Label("Готово  |  Пользователь: " + currentLogin());

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(split);
        root.setBottom(statusBar);
        BorderPane.setMargin(statusBar, new Insets(4, 8, 4, 8));

        // ─── listeners ─────────────────────────────────────────
        reportTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    refreshDetail(sel);
                    btnDelete.setDisable(sel == null || !isOwner(sel));
                });

        btnCreate.setOnAction(e -> {
            ReportDialog dlg = new ReportDialog(null);
            dlg.showAndWait().ifPresent(data -> {
                try {
                    service.createReportBySample(data.name(), data.sampleId());
                    refreshMaster();
                    setStatus("Отчёт создан");
                } catch (ValidationException ex) { showError(ex.getMessage()); }
            });
        });

        btnDelete.setOnAction(e -> {
            Report sel = selectedReport();
            if (sel == null) { showError("Выберите отчёт для удаления"); return; }
            if (!isOwner(sel)) { showError("Ошибка: у вас нет прав на удаление этого объекта."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить отчёт «" + sel.getName() + "» и все его строки?",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        service.deleteReport(sel.getId());
                        refreshMaster();
                        clearDetail();
                        setStatus("Отчёт удалён");
                    } catch (ValidationException ex) { showError(ex.getMessage()); }
                }
            });
        });

        btnRefresh.setOnAction(e -> {
            Report sel = selectedReport();
            refreshMaster();
            if (sel != null)
                reportData.stream().filter(r -> r.getId() == sel.getId())
                        .findFirst().ifPresent(r -> reportTable.getSelectionModel().select(r));
            setStatus("Обновлено");
        });

        btnSave.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Сохранить XML");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML файлы", "*.xml"));
            File file = fc.showSaveDialog(root.getScene().getWindow());
            if (file != null) {
                try {
                    storage.save(file.getAbsolutePath());
                    setStatus("Сохранено в " + file.getName());
                } catch (Exception ex) { showError("Ошибка сохранения: " + ex.getMessage()); }
            }
        });

        btnLoad.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Загрузить XML");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML файлы", "*.xml"));
            File file = fc.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                try {
                    storage.load(file.getAbsolutePath());
                    refreshMaster();
                    clearDetail();
                    setStatus("Загружено из " + file.getName());
                } catch (Exception ex) { showError("Ошибка загрузки: " + ex.getMessage()); }
            }
        });

        btnFinalize.setOnAction(e -> {
            Report sel = selectedReport();
            if (sel == null) return;
            try {
                service.finalizeReport(sel.getId());
                refreshMaster();
                refreshDetail(service.getById(sel.getId()).orElse(null));
                setStatus("Отчёт переведён в FINAL");
            } catch (ValidationException ex) { showError(ex.getMessage()); }
        });

        btnSign.setOnAction(e -> {
            Report sel = selectedReport();
            if (sel == null) return;
            try {
                service.signReport(sel.getId(), currentLogin());
                refreshMaster();
                refreshDetail(service.getById(sel.getId()).orElse(null));
                setStatus("Отчёт подписан");
            } catch (ValidationException ex) { showError(ex.getMessage()); }
        });

        btnLogout.setOnAction(e -> {
            userService.logout();
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        });

        refreshMaster();
        return new Scene(root, 1100, 660);
    }

    // ─────────────────── master table ──────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<Report> buildReportTable() {
        TableView<Report> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Report, Long>   colId     = new TableColumn<>("ID");
        TableColumn<Report, String> colName   = new TableColumn<>("Название");
        TableColumn<Report, String> colStatus = new TableColumn<>("Статус");
        TableColumn<Report, String> colOwner  = new TableColumn<>("Владелец");

        colId    .setCellValueFactory(new PropertyValueFactory<>("id"));
        colName  .setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatus().name()));
        colOwner .setCellValueFactory(new PropertyValueFactory<>("ownerUsername"));

        colId.setMaxWidth(45); colId.setMinWidth(35);
        colOwner.setMaxWidth(130);

        // подсветка строк: чужие объекты — серые
        tv.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Report r, boolean empty) {
                super.updateItem(r, empty);
                if (r == null || empty) {
                    setStyle("");
                } else {
                    String base = switch (r.getStatus()) {
                        case SIGNED -> "-fx-background-color: #e8f5e9;";
                        case FINAL  -> "-fx-background-color: #fff8e1;";
                        default     -> "";
                    };
                    // серый для чужих
                    if (!isOwner(r) && !base.isEmpty()) {
                        setStyle(base + " -fx-opacity: 0.75;");
                    } else if (!isOwner(r)) {
                        setStyle("-fx-text-fill: #9e9e9e;");
                    } else {
                        setStyle(base);
                    }
                }
            }
        });

        tv.getColumns().addAll(colId, colName, colStatus, colOwner);
        return tv;
    }

    // ─────────────────── detail panel ──────────────────────────

    private VBox buildDetailPanel() {
        lblName   = new Label("—");
        lblStatus = new Label("—");
        lblSample = new Label("—");
        lblOwner  = new Label("—");
        lblSigned = new Label("—");
        lblLines  = new Label("—");

        GridPane card = new GridPane();
        card.setHgap(12); card.setVgap(6);
        card.setPadding(new Insets(8, 8, 4, 8));
        card.add(bold("Название:"), 0, 0); card.add(lblName,   1, 0);
        card.add(bold("Статус:"),   0, 1); card.add(lblStatus, 1, 1);
        card.add(bold("Образец:"),  0, 2); card.add(lblSample, 1, 2);
        card.add(bold("Владелец:"), 0, 3); card.add(lblOwner,  1, 3);
        card.add(bold("Подписан:"), 0, 4); card.add(lblSigned, 1, 4);
        card.add(bold("Строк:"),    0, 5); card.add(lblLines,  1, 5);

        btnAddLine  = new Button("➕ Добавить строку");
        btnEditLine = new Button("✏ Редактировать");
        btnDelLine  = new Button("🗑 Удалить строку");
        btnFinalize = new Button("📋 FINAL");
        btnSign     = new Button("✅ Подписать");

        HBox detailBtns = new HBox(6,
                btnAddLine, btnEditLine, btnDelLine,
                new Separator(), btnFinalize, btnSign);
        detailBtns.setPadding(new Insets(6, 8, 6, 8));

        lineTable = buildLineTable();
        lineTable.setItems(lineData);
        lineTable.setPlaceholder(new Label("Нет строк"));

        // handlers
        btnAddLine.setOnAction(e -> {
            Report sel = selectedReport();
            if (sel == null) return;
            ReportLineDialog dlg = new ReportLineDialog(null);
            dlg.showAndWait().ifPresent(data -> {
                try {
                    MeasurementParam param = MeasurementParam.valueOf(data.param());
                    service.addLine(sel.getId(), param, Double.parseDouble(data.value()), data.unit());
                    refreshDetail(service.getById(sel.getId()).orElse(null));
                    setStatus("Строка добавлена");
                } catch (ValidationException ex) { showError(ex.getMessage());
                } catch (NumberFormatException ex) { showError("Значение должно быть числом."); }
            });
        });

        btnEditLine.setOnAction(e -> {
            ReportLine selLine = lineTable.getSelectionModel().getSelectedItem();
            if (selLine == null) { showError("Выберите строку для редактирования"); return; }
            Report selRep = selectedReport();
            if (selRep == null) return;
            ReportLineDialog dlg = new ReportLineDialog(selLine);
            dlg.showAndWait().ifPresent(data -> {
                try {
                    service.updateLine(selLine.getId(), "param", data.param());
                    service.updateLine(selLine.getId(), "value", data.value());
                    service.updateLine(selLine.getId(), "unit",  data.unit());
                    refreshDetail(service.getById(selRep.getId()).orElse(null));
                    setStatus("Строка обновлена");
                } catch (ValidationException ex) { showError(ex.getMessage()); }
            });
        });

        btnDelLine.setOnAction(e -> {
            ReportLine selLine = lineTable.getSelectionModel().getSelectedItem();
            if (selLine == null) { showError("Выберите строку для удаления"); return; }
            Report selRep = selectedReport();
            try {
                service.deleteLine(selLine.getId());
                refreshDetail(service.getById(selRep.getId()).orElse(null));
                setStatus("Строка удалена");
            } catch (ValidationException ex) { showError(ex.getMessage()); }
        });

        VBox detail = new VBox(6,
                new Label("Детали отчёта:"),
                card,
                detailBtns,
                new Separator(),
                new Label("Строки отчёта:"),
                lineTable);
        detail.setPadding(new Insets(8));
        VBox.setVgrow(lineTable, Priority.ALWAYS);
        return detail;
    }

    @SuppressWarnings("unchecked")
    private TableView<ReportLine> buildLineTable() {
        TableView<ReportLine> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ReportLine, Long>   colId    = new TableColumn<>("ID");
        TableColumn<ReportLine, String> colParam = new TableColumn<>("Параметр");
        TableColumn<ReportLine, String> colVal   = new TableColumn<>("Значение");
        TableColumn<ReportLine, String> colUnit  = new TableColumn<>("Единицы");

        colId   .setCellValueFactory(new PropertyValueFactory<>("id"));
        colParam.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getParam().name()));
        colVal  .setCellValueFactory(cd ->
                new SimpleStringProperty(String.format("%.4f", cd.getValue().getValue())));
        colUnit .setCellValueFactory(new PropertyValueFactory<>("unit"));

        colId.setMaxWidth(55); colId.setMinWidth(40);
        tv.getColumns().addAll(colId, colParam, colVal, colUnit);
        return tv;
    }

    // ─────────────────── refresh helpers ───────────────────────

    private void refreshMaster() {
        Report sel = selectedReport();
        List<Report> reports = service.getMyReports();
        reportData.setAll(reports);
        if (sel != null)
            reportData.stream().filter(r -> r.getId() == sel.getId())
                    .findFirst().ifPresent(r -> reportTable.getSelectionModel().select(r));
    }

    private void refreshDetail(Report r) {
        if (r == null) { clearDetail(); return; }

        lblName  .setText(r.getName());
        lblStatus.setText(r.getStatus().name());
        lblSample.setText(r.getSampleId() > 0 ? String.valueOf(r.getSampleId()) : "—");
        lblOwner .setText(r.getOwnerUsername());
        lblSigned.setText(r.getSignedBy() != null ? r.getSignedBy() : "—");

        List<ReportLine> lines;
        try { lines = service.getLinesForReport(r.getId()); }
        catch (ValidationException e) { lines = List.of(); }
        lblLines.setText(String.valueOf(lines.size()));
        lineData.setAll(lines);

        boolean isDraft  = r.getStatus() == ReportStatus.DRAFT;
        boolean isFinal  = r.getStatus() == ReportStatus.FINAL;
        boolean ownedByMe = isOwner(r);

        // Edit/Delete активны только для владельца и только в DRAFT
        btnAddLine .setDisable(!isDraft || !ownedByMe);
        btnEditLine.setDisable(!isDraft || !ownedByMe);
        btnDelLine .setDisable(!isDraft || !ownedByMe);
        btnFinalize.setDisable(!isDraft || !ownedByMe);
        btnSign    .setDisable(!isFinal || !ownedByMe);
    }

    private void clearDetail() {
        lblName.setText("—"); lblStatus.setText("—"); lblSample.setText("—");
        lblOwner.setText("—"); lblSigned.setText("—"); lblLines.setText("—");
        lineData.clear();
        btnAddLine.setDisable(true); btnEditLine.setDisable(true);
        btnDelLine.setDisable(true); btnFinalize.setDisable(true);
        btnSign.setDisable(true);
    }

    private void refreshUserLabel(Label lbl) {
        User u = userService.getCurrentUser();
        lbl.setText(u != null ? "👤 " + u.getLogin() : "👤 не авторизован");
        lbl.setStyle(u != null ? "-fx-text-fill: #1b5e20; -fx-font-weight: bold;"
                               : "-fx-text-fill: #b71c1c;");
    }

    // ─────────────────── utils ─────────────────────────────────

    private boolean isOwner(Report r) {
        User current = userService.getCurrentUser();
        if (current == null) return false;
        return current.getLogin().equalsIgnoreCase(r.getOwnerUsername());
    }

    private String currentLogin() {
        User u = userService.getCurrentUser();
        return u != null ? u.getLogin() : "—";
    }

    private void setStatus(String msg) {
        if (statusBar != null)
            statusBar.setText(msg + "  |  Пользователь: " + currentLogin());
    }

    private Report selectedReport() {
        return reportTable.getSelectionModel().getSelectedItem();
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Ошибка");
        a.setHeaderText(null);
        a.showAndWait();
    }
}
