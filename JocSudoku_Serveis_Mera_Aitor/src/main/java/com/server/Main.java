package com.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Main extends WebSocketServer {

    public static final int DEFAULT_PORT = 3000;

    private static final List<String> CHARACTER_NAMES = Arrays.asList(
        "Mario", "Luigi", "Peach", "Toad", "Bowser", "Wario", "Zelda", "Link"
    );

    private static final int PUNTS_ENCERT = 2;
    private static final int PUNTS_ERROR = -1;

    private static final String K_TYPE = "type";
    private static final String K_NAME = "name";
    private static final String K_ROW = "row";
    private static final String K_COL = "col";
    private static final String K_VALUE = "value";
    private static final String K_CLIENT_ID = "clientId";
    private static final String K_BOARD = "board";
    private static final String K_SOLVED = "solved";
    private static final String K_PLAYERS = "players";
    private static final String K_FINISHED = "finished";

    private static final String T_SET_NAME = "setName";
    private static final String T_PLAY = "play";
    private static final String T_NEW_GAME = "newGame";
    private static final String T_SERVER_DATA = "serverData";

    private final ClientRegistry clients;

    private final Map<String, Jugador> jugadors = new ConcurrentHashMap<>();

    private Sudoku sudoku;

    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(CHARACTER_NAMES);
        this.sudoku = new Sudoku();
    }


    private static JSONObject msg(String type) {
        return new JSONObject().put(K_TYPE, type);
    }

    private void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            if (name != null) jugadors.remove(name);
            System.out.println("Client desconnectat durant send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void broadcastStatus() {
        JSONArray arrPlayers = new JSONArray();
        for (Jugador j : jugadors.values()) {
            arrPlayers.put(j.toJSON());
        }

        JSONObject rst = msg(T_SERVER_DATA)
                        .put(K_BOARD, sudoku.taulerToJSON())
                        .put(K_SOLVED, sudoku.encertadesToJSON())
                        .put(K_PLAYERS, arrPlayers)
                        .put(K_FINISHED, sudoku.completat());

        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            rst.put(K_CLIENT_ID, e.getValue());
            sendSafe(e.getKey(), rst.toString());
        }
    }


    private synchronized void jugar(String id, int fila, int col, int valor) {
        Jugador jugador = jugadors.get(id);
        if (jugador == null) return;
        if (sudoku.completat()) return;
        if (valor < 1 || valor > 9) return;
        if (!sudoku.esBuida(fila, col)) return;

        if (sudoku.jugar(fila, col, valor)) {
            jugador.punts += PUNTS_ENCERT;
            System.out.println(jugador.nom + " encerta (" + fila + "," + col + ") = " + valor);
        } else {
            jugador.punts += PUNTS_ERROR;
            System.out.println(jugador.nom + " s'equivoca (" + fila + "," + col + ") = " + valor);
        }
        broadcastStatus();
    }

    private synchronized void novaPartida() {
        if (!sudoku.completat()) return;
        sudoku = new Sudoku();
        for (Jugador j : jugadors.values()) {
            j.punts = 0;
        }
        System.out.println("Nova partida");
        broadcastStatus();
    }


    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String id = clients.add(conn);
        jugadors.put(id, new Jugador(id));
        System.out.println("Client connectat: " + id);
        broadcastStatus();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String id = clients.remove(conn);
        if (id != null) jugadors.remove(id);
        System.out.println("Client desconnectat: " + id);
        broadcastStatus();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String id = clients.nameBySocket(conn);
        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            return;
        }

        String type = obj.optString(K_TYPE, "");
        switch (type) {
            case T_SET_NAME -> {
                String nom = obj.optString(K_NAME, "").trim();
                Jugador jugador = jugadors.get(id);
                if (jugador != null && !nom.isEmpty()) {
                    jugador.nom = nom;
                    System.out.println(id + " es diu " + nom);
                    broadcastStatus();
                }
            }
            case T_PLAY -> {
                int fila = obj.optInt(K_ROW, -1);
                int col = obj.optInt(K_COL, -1);
                int valor = obj.optInt(K_VALUE, 0);
                jugar(id, fila, col, valor);
            }
            case T_NEW_GAME -> {
                novaPartida();
            }
            default -> {
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket engegat al port: " + getPort());
        setConnectionLostTimeout(100);
    }


    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor (shutdown hook)...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor aturat.");
        }));
    }

    private static void awaitForever() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        registerShutdownHook(server);

        System.out.println("Servidor Sudoku en execució al port " + DEFAULT_PORT + ". Prem Ctrl+C per aturar-lo.");
        awaitForever();
    }
}
