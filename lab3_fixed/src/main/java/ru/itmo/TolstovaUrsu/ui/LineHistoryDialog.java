package ru.itmo.TolstovaUrsu.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.beans.property.SimpleStringProperty;
import ru.itmo.TolstovaUrsu.domain.ReportLineHistory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Окно «Визуальная история изменений объекта» — показывает график изменения
 * значения конкретной строки отчёта (ReportLine) во времени, а также таблицу
 * всех зафиксированных событий (INSERT / UPDATE / DELETE).
 */
public class LineHistoryDialog extends Dialog<Void> {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    public LineHistoryDialog(long lineId, List<ReportLineHistory> history) {
        setTitle("История изменений строки #" + lineId);
        setResizable(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        if (history == null || history.isEmpty()) {
            root.getChildren().add(new Label(
                    "История изменений недоступна (нет записей или БД отключена)."));
        } else {
            // ── график изменения значения во времени ─────────────────
            NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel("Событие №");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Значение (" + history.get(0).getUnit() + ")");

            LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
            chart.setTitle("Динамика изменения значения");
            chart.setCreateSymbols(true);
            chart.setLegendVisible(false);
            chart.setPrefHeight(280);

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            int i = 1;
            for (ReportLineHistory h : history) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(i++, h.getValue());
                series.getData().add(point);
            }
            chart.getData().add(series);

            // подписи точек значением и временем (через tooltip)
            for (XYChart.Data<Number, Number> point : series.getData()) {
                int idx = point.getXValue().intValue() - 1;
                ReportLineHistory h = history.get(idx);
                Tooltip.install(point.getNode(), new Tooltip(
                        h.getAction() + "\n" + FMT.format(h.getChangedAt())
                                + "\nзначение = " + h.getValue() + " " + h.getUnit()));
            }

            // ── таблица с подробной историей ──────────────────────────
            TableView<ReportLineHistory> table = new TableView<>();
            table.setItems(FXCollections.observableArrayList(history));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<ReportLineHistory, String> colTime = new TableColumn<>("Время изменения");
            colTime.setCellValueFactory(cd ->
                    new SimpleStringProperty(FMT.format(cd.getValue().getChangedAt())));

            TableColumn<ReportLineHistory, String> colAction = new TableColumn<>("Действие");
            colAction.setCellValueFactory(cd -> new SimpleStringProperty(switch (cd.getValue().getAction()) {
                case "INSERT" -> "Создание";
                case "UPDATE" -> "Изменение";
                case "DELETE" -> "Удаление";
                default -> cd.getValue().getAction();
            }));

            TableColumn<ReportLineHistory, String> colParam = new TableColumn<>("Параметр");
            colParam.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getParam().name()));

            TableColumn<ReportLineHistory, String> colValue = new TableColumn<>("Значение");
            colValue.setCellValueFactory(cd ->
                    new SimpleStringProperty(String.format("%.4f", cd.getValue().getValue())));

            TableColumn<ReportLineHistory, String> colUnit = new TableColumn<>("Единицы");
            colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

            table.getColumns().addAll(colTime, colAction, colParam, colValue, colUnit);
            table.setPrefHeight(200);

            root.getChildren().addAll(
                    new Label("Изменения значения строки во времени:"),
                    chart,
                    new Label("Журнал событий:"),
                    table);
        }

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        initModality(Modality.APPLICATION_MODAL);
        setHeight(620);
        setWidth(640);
    }
}
