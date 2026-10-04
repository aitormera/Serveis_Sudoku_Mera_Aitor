package com.clientFX;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static String clientId = "";
    private static boolean nameSent = false;

    public static CtrlConfig ctrlConfig;
    public static CtrlPlay ctrlPlay;
    public static CtrlRanking ctrlRanking;

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        final int windowWidth = 850;
        final int windowHeight = 650;

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewPlay", "/assets/viewPlay.fxml");
        UtilsViews.addView(getClass(), "ViewRanking", "/assets/viewRanking.fxml");

        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlPlay = (CtrlPlay) UtilsViews.getController("ViewPlay");
        ctrlRanking = (CtrlRanking) UtilsViews.getController("ViewRanking");

        Scene scene = new Scene(UtilsViews.parentContainer);

        stage.setScene(scene);
        stage.setTitle("Sudoku");
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();

        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:/icons/icon.png");
            stage.getIcons().add(icon);
        }
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(0);
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    public static List<JSONObject> getPlayersSorted(JSONObject msgObj) {
        JSONArray arr = msgObj.getJSONArray("players");
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getJSONObject(i));
        }
        list.sort((a, b) -> b.getInt("points") - a.getInt("points"));
        return list;
    }

    public static void connectToServer() {

        String name = ctrlConfig.txtName.getText().trim();
        if (name.isEmpty()) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText("Escriu el nom del jugador");
            return;
        }

        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");

        pauseDuring(1500, () -> {

            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);

            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });
        });
    }

    private static void wsMessage(String response) {
        JSONObject msgObj = new JSONObject(response);

        switch (msgObj.getString("type")) {
            case "serverData":
                clientId = msgObj.getString("clientId");

                if (!nameSent) {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "setName");
                    obj.put("name", ctrlConfig.txtName.getText().trim());
                    wsClient.safeSend(obj.toString());
                    nameSent = true;
                }

                ctrlPlay.updateData(msgObj);
                ctrlRanking.updateData(msgObj);

                if (msgObj.getBoolean("finished")) {
                    if (!"ViewRanking".equals(UtilsViews.getActiveView())) {
                        UtilsViews.setViewAnimating("ViewRanking");
                    }
                } else {
                    if (!"ViewPlay".equals(UtilsViews.getActiveView())) {
                        UtilsViews.setViewAnimating("ViewPlay");
                    }
                }
                break;
        }
    }

    private static void wsError(String response) {

        String connectionRefused = "Connection refused";
        if (response.indexOf(connectionRefused) != -1) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText(connectionRefused);
            pauseDuring(1500, () -> {
                ctrlConfig.txtMessage.setText("");
            });
        }
    }
}
