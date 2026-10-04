package com.clientFX;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CtrlPlay implements Initializable {

    private static final int MIDA = 9;
    private static final double MIDA_CASELLA = 48;

    @FXML
    private Label txtPlayer;

    @FXML
    private GridPane board;

    @FXML
    private HBox numbers;

    @FXML
    private VBox listPlayers;

    private final Button[][] cells = new Button[MIDA][MIDA];
    private int[][] values = new int[MIDA][MIDA];
    private boolean[][] solved = new boolean[MIDA][MIDA];

    private int selRow = -1;
    private int selCol = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        for (int row = 0; row < MIDA; row++) {
            for (int col = 0; col < MIDA; col++) {
                Button cell = new Button();
                cell.setMinSize(MIDA_CASELLA, MIDA_CASELLA);
                cell.setPrefSize(MIDA_CASELLA, MIDA_CASELLA);
                cell.setMaxSize(MIDA_CASELLA, MIDA_CASELLA);
                final int r = row;
                final int c = col;
                cell.setOnAction((event) -> { selectCell(r, c); });
                cells[row][col] = cell;
                board.add(cell, col, row);
            }
        }

        for (int i = 1; i <= MIDA; i++) {
            Button btn = new Button(String.valueOf(i));
            btn.setPrefSize(44, 36);
            final int value = i;
            btn.setOnAction((event) -> { sendValue(value); });
            numbers.getChildren().add(btn);
        }

        refreshBoard();
    }

    private void selectCell(int row, int col) {
        if (values[row][col] != 0) return;
        selRow = row;
        selCol = col;
        refreshBoard();
    }

    private void sendValue(int value) {
        if (selRow < 0 || selCol < 0) return;

        JSONObject obj = new JSONObject();
        obj.put("type", "play");
        obj.put("row", selRow);
        obj.put("col", selCol);
        obj.put("value", value);
        Main.wsClient.safeSend(obj.toString());
    }

    public void updateData(JSONObject msgObj) {
        JSONArray arrBoard = msgObj.getJSONArray("board");
        JSONArray arrSolved = msgObj.getJSONArray("solved");
        for (int row = 0; row < MIDA; row++) {
            for (int col = 0; col < MIDA; col++) {
                values[row][col] = arrBoard.getJSONArray(row).getInt(col);
                solved[row][col] = arrSolved.getJSONArray(row).getBoolean(col);
            }
        }

        if (selRow >= 0 && values[selRow][selCol] != 0) {
            selRow = -1;
            selCol = -1;
        }

        refreshBoard();
        refreshPlayers(msgObj);
    }

    private void refreshBoard() {
        for (int row = 0; row < MIDA; row++) {
            for (int col = 0; col < MIDA; col++) {
                Button cell = cells[row][col];
                int value = values[row][col];
                cell.setText(value == 0 ? "" : String.valueOf(value));

                String background = "white";
                String weight = "normal";
                if (solved[row][col]) {
                    background = "#7ed47e";
                } else if (value != 0) {
                    background = "#e4e4e4";
                    weight = "bold";
                } else if (row == selRow && col == selCol) {
                    background = "#b9dcff";
                }

                double top = (row % 3 == 0) ? 3 : 0.5;
                double left = (col % 3 == 0) ? 3 : 0.5;
                double bottom = (row == MIDA - 1) ? 3 : 0.5;
                double right = (col == MIDA - 1) ? 3 : 0.5;

                cell.setStyle(
                    "-fx-background-color: " + background + ";" +
                    "-fx-background-radius: 0;" +
                    "-fx-border-color: #333333;" +
                    "-fx-border-width: " + top + " " + right + " " + bottom + " " + left + ";" +
                    "-fx-font-size: 20;" +
                    "-fx-font-weight: " + weight + ";" +
                    "-fx-text-fill: black;"
                );
            }
        }
    }

    private void refreshPlayers(JSONObject msgObj) {
        JSONArray players = msgObj.getJSONArray("players");
        listPlayers.getChildren().clear();

        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            String name = player.getString("name");
            Label lbl = new Label(name + "   " + player.getInt("points"));
            lbl.setStyle("-fx-font-size: 16;");
            if (player.getString("id").equals(Main.clientId)) {
                txtPlayer.setText("Juga: " + name);
            }
            listPlayers.getChildren().add(lbl);
        }
    }
}
