package ru.itmo.TolstovaUrsu.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.itmo.TolstovaUrsu.domain.MeasurementParam;
import ru.itmo.TolstovaUrsu.domain.ReportLine;

public class ReportLineDialog extends Dialog<ReportLineDialog.LineFormData> {

    public record LineFormData(String param, String value, String unit) {}

    private final ComboBox<String> paramBox   = new ComboBox<>();
    private final TextField        valueField = new TextField();
    private final TextField        unitField  = new TextField();

    public ReportLineDialog(ReportLine existing) {
        setTitle(existing == null ? "Добавить строку" : "Редактировать строку");
        setHeaderText(null);

        ButtonType okBtn = new ButtonType(existing == null ? "Добавить" : "Сохранить",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        for (MeasurementParam p : MeasurementParam.values())
            paramBox.getItems().add(p.name());
        paramBox.getSelectionModel().selectFirst();

        unitField.setPromptText("pH / mS/cm / NTU / mg/L");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Параметр:"), 0, 0); grid.add(paramBox,   1, 0);
        grid.add(new Label("Значение:"), 0, 1); grid.add(valueField, 1, 1);
        grid.add(new Label("Единицы:"),  0, 2); grid.add(unitField,  1, 2);

        paramBox.setPrefWidth(200);
        valueField.setPrefWidth(200);
        unitField.setPrefWidth(200);

        if (existing != null) {
            paramBox.setValue(existing.getParam().name());
            valueField.setText(String.valueOf(existing.getValue()));
            unitField.setText(existing.getUnit());
        }

        getDialogPane().setContent(grid);

        setResultConverter(btn -> {
            if (btn == okBtn)
                return new LineFormData(
                        paramBox.getValue(),
                        valueField.getText().trim(),
                        unitField.getText().trim());
            return null;
        });
    }
}
