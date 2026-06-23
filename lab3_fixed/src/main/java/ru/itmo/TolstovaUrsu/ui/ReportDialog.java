package ru.itmo.TolstovaUrsu.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.itmo.TolstovaUrsu.domain.Report;


public class ReportDialog extends Dialog<ReportDialog.ReportFormData> {

    public record ReportFormData(String name, long sampleId) {}

    private final TextField nameField     = new TextField();
    private final TextField sampleIdField = new TextField();

    public ReportDialog(Report existing) {
        setTitle(existing == null ? "Создать отчёт" : "Редактировать отчёт");
        setHeaderText(null);

        ButtonType okBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        sampleIdField.setPromptText("0 — если не привязан к образцу");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Название отчёта:"), 0, 0); grid.add(nameField,     1, 0);
        grid.add(new Label("ID образца:"),      0, 1); grid.add(sampleIdField, 1, 1);

        nameField.setPrefWidth(220);
        sampleIdField.setPrefWidth(220);

        if (existing != null) {
            nameField.setText(existing.getName());
            sampleIdField.setText(String.valueOf(existing.getSampleId()));
        } else {
            sampleIdField.setText("0");
        }

        getDialogPane().setContent(grid);

        setResultConverter(btn -> {
            if (btn == okBtn) {
                long sid = 0;
                try { sid = Long.parseLong(sampleIdField.getText().trim()); }
                catch (NumberFormatException ignored) {}
                return new ReportFormData(nameField.getText().trim(), sid);
            }
            return null;
        });
    }
}
