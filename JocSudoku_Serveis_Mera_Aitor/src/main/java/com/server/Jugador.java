package com.server;

import org.json.JSONObject;

public class Jugador {
    public String id;
    public String nom;
    public int punts;

    public Jugador(String id) {
        this.id = id;
        this.nom = id;
        this.punts = 0;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", nom);
        obj.put("points", punts);
        return obj;
    }
}
