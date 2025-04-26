package org.orgaprop.controlprest.services;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "ControlPrestPrefs";

    // Clés constantes pour les préférences
    public static final String KEY_MBR_ID = "mbr_id";
    public static final String KEY_ADR_MAC = "adr_mac";
    public static final String KEY_AGENCY = "agency";
    public static final String KEY_GROUP = "group";
    public static final String KEY_RESIDENCE = "residence";

    private final SharedPreferences sharedPreferences;
    private static PreferencesManager instance;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }



    /**
     * Récupère l'instance unique de PreferencesManager
     */
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
        return instance;
    }

    /**
     * Initialisation des préférences par défaut si nécessaire
     */
    public void initializeDefaultPrefsIfNeeded() {
        if (!sharedPreferences.contains(KEY_MBR_ID)) {
            setMbrId("new");
        }
        if (!sharedPreferences.contains(KEY_ADR_MAC)) {
            setAdrMac("new");
        }
        if (!sharedPreferences.contains(KEY_AGENCY)) {
            setAgency("");
        }
        if (!sharedPreferences.contains(KEY_GROUP)) {
            setGroup("");
        }
        if (!sharedPreferences.contains(KEY_RESIDENCE)) {
            setResidence("");
        }
    }



    // Méthodes setter
    public void setMbrId(String id) {
        sharedPreferences.edit().putString(KEY_MBR_ID, id).apply();
    }

    public void setAdrMac(String adrMac) {
        sharedPreferences.edit().putString(KEY_ADR_MAC, adrMac).apply();
    }

    public void setAgency(String agency) {
        sharedPreferences.edit().putString(KEY_AGENCY, agency).apply();
    }

    public void setGroup(String group) {
        sharedPreferences.edit().putString(KEY_GROUP, group).apply();
    }

    public void setResidence(String residence) {
        sharedPreferences.edit().putString(KEY_RESIDENCE, residence).apply();
    }

    // Méthodes getter
    public String getMbrId() {
        return sharedPreferences.getString(KEY_MBR_ID, "new");
    }

    public String getAdrMac() {
        return sharedPreferences.getString(KEY_ADR_MAC, "new");
    }

    public String getAgency() {
        return sharedPreferences.getString(KEY_AGENCY, "");
    }

    public String getGroup() {
        return sharedPreferences.getString(KEY_GROUP, "");
    }

    public String getResidence() {
        return sharedPreferences.getString(KEY_RESIDENCE, "");
    }

    // Méthode pour effacer toutes les préférences
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

}
