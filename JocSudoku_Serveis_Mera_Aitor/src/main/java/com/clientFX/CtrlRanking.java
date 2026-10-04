package com.clientFX;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CtrlRanking implements Initializable {

    @FXML
    private VBox listRanking;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void updateData(JSONObject msgObj) {
        List<JSONObject> players = Main.getPlayersSorted(msgObj);
        listRanking.getChildren().clear();

        int position = 1;
        for (JSONObject player : players) {
            Label lbl = new Label(position + ". " + player.getString("name") + "   " + player.getInt("points") + " punts");
            lbl.setStyle("-fx-font-size: 20;");
            listRanking.getChildren().add(lbl);
            position++;
        }
    }

    @FXML
    private void playAgain() {
        JSONObject obj = new JSONObject();
        obj.put("type", "newGame");
        Main.wsClient.safeSend(obj.toString());
    }
}
