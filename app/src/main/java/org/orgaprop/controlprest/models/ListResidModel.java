package org.orgaprop.controlprest.models;

import android.util.Log;

import androidx.annotation.NonNull;

import org.orgaprop.controlprest.utils.ToastManager;

public class ListResidModel {

//********* PRIVATES VARIABLES

    private int id = 0;
    private String ref = "";
    private String name = "";
    private String entry = "";
    private String adr = "";
    private String city = "";
    private String agc = "";
    private String grp = "";
    private boolean visited = false;
    private String last = "";

//********* STATIC VARIABLES

    private static final String TAG = "ListResidModel";

//********* CONSTRUCTORS

    public ListResidModel() {}
    public ListResidModel(String agc,String grp, int id, String ref, String name, String entry, String adr, String city, boolean visited, String last) {
        try {
            this.agc = agc != null ? agc : "";
            this.grp = grp != null ? grp : "";
            this.id = id;
            this.ref = ref != null ? ref : "";
            this.name = name != null ? name : "";
            this.entry = entry != null ? entry : "";
            this.adr = adr != null ? adr : "";
            this.city = city != null ? city : "";
            this.visited = visited;
            this.last = last != null ? last : "";
        } catch (Exception e) {
            Log.e(TAG, "Error in constructor: " + e.getMessage());
            ToastManager.showShort("Error in constructor: " + e.getMessage());
        }
    }

//********* PUBLIC FUNCTIONS

    public int getId() {
        return this.id;
    }

    public void setId(Integer id) {
        try {
            this.id = id != null ? id : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error setting id: " + e.getMessage());
            ToastManager.showShort("Error setting id: " + e.getMessage());
            this.id = 0;
        }
    }

    public String getRef() {
        return this.ref;
    }

    public void setRef(String ref) {
        try {
            this.ref = ref != null ? ref : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting ref: " + e.getMessage());
            ToastManager.showShort("Error setting ref: " + e.getMessage());
            this.ref = "";
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        try {
            this.name = name != null ? name : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting name: " + e.getMessage());
            ToastManager.showShort("Error setting name: " + e.getMessage());
            this.name = "";
        }
    }

    public String getEntry() {
        return this.entry;
    }

    public void setEntry(String entry) {
        try {
            this.entry = entry != null ? entry : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting entry: " + e.getMessage());
            ToastManager.showShort("Error setting entry: " + e.getMessage());
            this.entry = "";
        }
    }

    public String getAdress() {
        return this.adr;
    }

    public void setAdresse(String adr) {
        try {
            this.adr = adr != null ? adr : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting adresse: " + e.getMessage());
            ToastManager.showShort("Error setting adresse: " + e.getMessage());
            this.adr = "";
        }
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String cp, String city) {
        try {
            String cpValue = cp != null ? cp : "";
            String cityValue = city != null ? city : "";
            this.city = cpValue.trim() + (cpValue.trim().isEmpty() || cityValue.trim().isEmpty() ? "" : " ") + cityValue.trim();
        } catch (Exception e) {
            Log.e(TAG, "Error setting city: " + e.getMessage());
            ToastManager.showShort("Error setting city: " + e.getMessage());
            this.city = "";
        }
    }

    public String getAgc() {
        return this.agc;
    }

    public void setAgc(String agc) {
        try {
            this.agc = agc != null ? agc : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting agc: " + e.getMessage());
            ToastManager.showShort("Error setting agc: " + e.getMessage());
            this.agc = "";
        }
    }

    public String getGrp() {
        return this.grp;
    }

    public void setGrp(String grp) {
        try {
            this.grp = grp != null ? grp : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting grp: " + e.getMessage());
            ToastManager.showShort("Error setting grp: " + e.getMessage());
            this.grp = "";
        }
    }

    public boolean isVisited() {
        return this.visited;
    }

    public void setVisited(boolean visited) {
        try {
            this.visited = visited;
        } catch (Exception e) {
            Log.e(TAG, "Error setting visited: " + e.getMessage());
            ToastManager.showShort("Error setting visited: " + e.getMessage());
            this.visited = false;
        }
    }

    public String getLast() {
        return this.last;
    }

    public void setLast(String last) {
        try {
            this.last = last != null ? last : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting last: " + e.getMessage());
            ToastManager.showShort("Error setting last: " + e.getMessage());
            this.last = "";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ResidenceInfo: " + name + " (" + ref + "), " + entry + ", " + adr + ", " + city;
    }

}
