package org.orgaprop.controlprest.models;

import android.util.Log;

import androidx.annotation.NonNull;

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
        setAgc(agc);
        setGrp(grp);
        setId(id);
        setRef(ref);
        setName(name);
        setEntry(entry);
        setAdr(adr);
        setCity(city);  // Utilise la version à un seul paramètre
        setVisited(visited);
        setLast(last);
    }

//********* PUBLIC FUNCTIONS

    public int getId() {
        return this.id;
    }

    public void setId(Integer id) {
        try {
            this.id = (id != null && id >= 0) ? id : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error setting id: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting ref: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting name: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting entry: " + e.getMessage(), e);
            this.entry = "";
        }
    }

    public String getAdr() {
        return this.adr;
    }

    public void setAdr(String adr) {
        try {
            this.adr = adr != null ? adr : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting adr: " + e.getMessage(), e);
            this.adr = "";
        }
    }

    // Correction de l'incohérence : renommer getAdress() en getAdr() pour correspondre au nom de la variable
    // ou renommer adr en address pour cohérence

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        try {
            this.city = city != null ? city : "";
        } catch (Exception e) {
            Log.e(TAG, "Error setting city: " + e.getMessage(), e);
            this.city = "";
        }
    }

    // Surcharge pour la compatibilité avec le code existant
    public void setCity(String cp, String city) {
        try {
            String cpValue = cp != null ? cp.trim() : "";
            String cityValue = city != null ? city.trim() : "";

            // Concaténation sécurisée avec vérification des chaînes vides
            if (cpValue.isEmpty() && cityValue.isEmpty()) {
                this.city = "";
            } else if (cpValue.isEmpty()) {
                this.city = cityValue;
            } else if (cityValue.isEmpty()) {
                this.city = cpValue;
            } else {
                this.city = cpValue + " " + cityValue;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting city with cp and city: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting agc: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting grp: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting visited: " + e.getMessage(), e);
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
            Log.e(TAG, "Error setting last: " + e.getMessage(), e);
            this.last = "";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ResidenceInfo: " + name + " (" + ref + "), " + entry + ", " + adr + ", " + city;
    }

    // Méthode utilitaire pour vérifier la validité du modèle
    public boolean isValid() {
        return !ref.isEmpty() && !name.isEmpty();
    }

}
