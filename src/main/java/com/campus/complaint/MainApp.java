package com.campus.complaint;

import com.campus.complaint.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX Application Entry Point – Campus Complaint Portal.
 * SDG 16: Peace, Justice and Strong Institutions.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 820);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("Campus Complaint Portal  |  SDG 16");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.show();

        // Graceful shutdown of the background thread
        MainController controller = loader.getController();
        primaryStage.setOnCloseRequest(e -> controller.shutdown());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
