package com.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;

public class Sudoku {

    public static final int MIDA = 9;

    private static final int CASELLES_BUIDES = 45;

    private final int[][] solucio = new int[MIDA][MIDA];
    private final int[][] tauler = new int[MIDA][MIDA];
    private final boolean[][] encertades = new boolean[MIDA][MIDA];

    public Sudoku() {
        generarSolucio();
        generarTauler();
    }

    private void generarSolucio() {
        List<Integer> numeros = new ArrayList<>();
        for (int i = 1; i <= MIDA; i++) {
            numeros.add(i);
        }
        Collections.shuffle(numeros);

        List<Integer> files = ordreBarrejat();
        List<Integer> columnes = ordreBarrejat();

        for (int fila = 0; fila < MIDA; fila++) {
            for (int col = 0; col < MIDA; col++) {
                int f = files.get(fila);
                int c = columnes.get(col);
                int patro = (3 * (f % 3) + f / 3 + c) % MIDA;
                solucio[fila][col] = numeros.get(patro);
            }
        }
    }

    private List<Integer> ordreBarrejat() {
        List<Integer> bandes = new ArrayList<>(List.of(0, 1, 2));
        Collections.shuffle(bandes);

        List<Integer> ordre = new ArrayList<>();
        for (int banda : bandes) {
            List<Integer> dins = new ArrayList<>(List.of(0, 1, 2));
            Collections.shuffle(dins);
            for (int d : dins) {
                ordre.add(banda * 3 + d);
            }
        }
        return ordre;
    }

    private void generarTauler() {
        for (int fila = 0; fila < MIDA; fila++) {
            for (int col = 0; col < MIDA; col++) {
                tauler[fila][col] = solucio[fila][col];
            }
        }

        int buides = 0;
        while (buides < CASELLES_BUIDES) {
            int fila = ThreadLocalRandom.current().nextInt(MIDA);
            int col = ThreadLocalRandom.current().nextInt(MIDA);
            if (tauler[fila][col] != 0) {
                tauler[fila][col] = 0;
                buides++;
            }
        }
    }

    public boolean esBuida(int fila, int col) {
        if (fila < 0 || fila >= MIDA || col < 0 || col >= MIDA) return false;
        return tauler[fila][col] == 0;
    }

    public boolean jugar(int fila, int col, int valor) {
        if (solucio[fila][col] == valor) {
            tauler[fila][col] = valor;
            encertades[fila][col] = true;
            return true;
        }
        return false;
    }

    public boolean completat() {
        for (int fila = 0; fila < MIDA; fila++) {
            for (int col = 0; col < MIDA; col++) {
                if (tauler[fila][col] == 0) return false;
            }
        }
        return true;
    }

    public JSONArray taulerToJSON() {
        JSONArray arr = new JSONArray();
        for (int fila = 0; fila < MIDA; fila++) {
            JSONArray f = new JSONArray();
            for (int col = 0; col < MIDA; col++) {
                f.put(tauler[fila][col]);
            }
            arr.put(f);
        }
        return arr;
    }

    public JSONArray encertadesToJSON() {
        JSONArray arr = new JSONArray();
        for (int fila = 0; fila < MIDA; fila++) {
            JSONArray f = new JSONArray();
            for (int col = 0; col < MIDA; col++) {
                f.put(encertades[fila][col]);
            }
            arr.put(f);
        }
        return arr;
    }
}
