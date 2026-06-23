package ru.itmo.TolstovaUrsu.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.itmo.TolstovaUrsu.auth.UserService;

public class LabApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        UserService userService = ServiceLocator.getUserService();

        // ─── окно входа ────────────────────────────────────────
        LoginWindow loginWindow = new LoginWindow(userService);
        loginWindow.showAndWait();

        if (!loginWindow.isAuthenticated()) {
            // пользователь закрыл окно не авторизовавшись — выходим
            System.out.println("Авторизация не выполнена. Завершение работы.");
            return;
        }

        // ─── основное окно ─────────────────────────────────────
        MainWindow window = new MainWindow();
        Scene scene = window.buildScene();
        primaryStage.setTitle("Отчёты по анализам — ЛР2  ["
                + userService.getCurrentUser().getLogin() + "]");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(520);
        primaryStage.show();
    }

    public static void startUI(String[] args) {
        Application.launch(LabApp.class, args);
    }
}
