package ru.itmo.TolstovaUrsu.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.itmo.TolstovaUrsu.auth.User;
import ru.itmo.TolstovaUrsu.auth.UserService;

public class LoginWindow {

    private final UserService userService;
    private final Stage       stage;
    private boolean           authenticated = false;

    public LoginWindow(UserService userService) {
        this.userService = userService;
        this.stage = new Stage();
        stage.setTitle("Вход в систему — Отчёты ЛР2");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);
        stage.setScene(buildScene());
    }

    private Scene buildScene() {
        Label title = new Label("Отчёты по анализам");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitle = new Label("Лабораторная работа 2");
        subtitle.setStyle("-fx-text-fill: #666;");

        Label loginLbl = new Label("Логин:");
        TextField loginField = new TextField();
        loginField.setPromptText("Введите логин");

        Label passLbl = new Label("Пароль:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Введите пароль");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.add(loginLbl,  0, 0); form.add(loginField, 1, 0);
        form.add(passLbl,   0, 1); form.add(passField,  1, 1);

        Button btnLogin    = new Button("Войти");
        Button btnRegister = new Button("Зарегистрироваться");
        btnLogin.setDefaultButton(true);
        btnLogin.setPrefWidth(160);
        btnRegister.setPrefWidth(180);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #d32f2f;");
        statusLabel.setWrapText(true);

        HBox buttons = new HBox(10, btnLogin, btnRegister);
        buttons.setAlignment(Pos.CENTER);

        btnLogin.setOnAction(e -> {
            String login = loginField.getText().trim();
            String pass  = passField.getText();
            if (login.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Заполните логин и пароль.");
                return;
            }
            try {
                userService.login(login, pass);
                authenticated = true;
                stage.close();
            } catch (IllegalArgumentException ex) {
                statusLabel.setText(ex.getMessage());
                passField.clear();
            }
        });

        btnRegister.setOnAction(e -> {
            String login = loginField.getText().trim();
            String pass  = passField.getText();
            if (login.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Заполните логин и пароль для регистрации.");
                return;
            }
            try {
                userService.register(login, pass);
                statusLabel.setStyle("-fx-text-fill: #2e7d32;");
                statusLabel.setText("Пользователь '" + login + "' зарегистрирован. Войдите.");
                passField.clear();
            } catch (IllegalArgumentException ex) {
                statusLabel.setStyle("-fx-text-fill: #d32f2f;");
                statusLabel.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(16,
                new VBox(4, title, subtitle),
                new Separator(),
                form,
                statusLabel,
                buttons);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_CENTER);
        root.setMinWidth(380);

        return new Scene(root);
    }


    public void showAndWait() {
        stage.showAndWait();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
