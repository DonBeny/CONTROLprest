package org.orgaprop.controlprest.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;



public class PreferencesManager {

    private static final String TAG = "PreferencesManager";
    private static final String PREFS_NAME = "ControlPrestPrefs";

    // Clés constantes pour les préférences
    public static final String KEY_MBR_ID = "mbr_id";
    public static final String KEY_ADR_MAC = "adr_mac";
    public static final String KEY_AGENCY = "agency";
    public static final String KEY_GROUP = "group";
    public static final String KEY_RESIDENCE = "residence";

    private final SharedPreferences sharedPreferences;
    private static volatile PreferencesManager instance;

    private final FirebaseCrashlytics crashlytics;
    private final FirebaseAnalytics analytics;



    /**
     * Constructeur privé pour implémentation Singleton
     */
    public PreferencesManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        Context appContext = context.getApplicationContext();
        sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialiser les outils de journalisation
        crashlytics = FirebaseCrashlytics.getInstance();
        analytics = FirebaseAnalytics.getInstance(appContext);

        logInfo("PreferencesManager initialized", "init_success");
    }



    /**
     * Récupère l'instance unique de PreferencesManager
     */
    public static synchronized PreferencesManager getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "Context cannot be null in getInstance()");
            throw new IllegalArgumentException("Context cannot be null");
        }

        if (instance == null) {
            synchronized (PreferencesManager.class) {
                if (instance == null) {
                    instance = new PreferencesManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Initialisation des préférences par défaut si nécessaire
     */
    public void initializeDefaultPrefsIfNeeded() {
        try {
            logInfo("Initializing default preferences", "init_defaults");

            boolean changesApplied = false;

            if (!sharedPreferences.contains(KEY_MBR_ID)) {
                setMbrId("new");
                changesApplied = true;
            }

            if (!sharedPreferences.contains(KEY_ADR_MAC)) {
                setAdrMac("new");
                changesApplied = true;
            }

            if (!sharedPreferences.contains(KEY_AGENCY)) {
                setAgency("");
                changesApplied = true;
            }

            if (!sharedPreferences.contains(KEY_GROUP)) {
                setGroup("");
                changesApplied = true;
            }

            if (!sharedPreferences.contains(KEY_RESIDENCE)) {
                setResidence("");
                changesApplied = true;
            }

            if (changesApplied) {
                logInfo("Default preferences applied", "defaults_applied");
            } else {
                logInfo("No default preferences needed to be applied", "defaults_not_needed");
            }
        } catch (Exception e) {
            logException(e, "init_defaults_error", "Error initializing default preferences");
        }
    }



    /**
     * Définit l'ID du membre
     * @param id ID du membre
     */
    public void setMbrId(String id) {
        try {
            if (id == null) {
                logWarning("Null ID provided, using 'new' instead", "null_id");
                id = "new";
            }

            if (id.isEmpty()) {
                logWarning("Empty ID provided, using 'new' instead", "empty_id");
                id = "new";
            }

            sharedPreferences.edit().putString(KEY_MBR_ID, id).apply();
            logInfo("Member ID set: " + id, "mbr_id_set");
        } catch (Exception e) {
            logException(e, "set_mbr_id_error", "Error setting member ID");
        }
    }

    /**
     * Définit l'adresse MAC
     * @param adrMac Adresse MAC
     */
    public void setAdrMac(String adrMac) {
        try {
            if (adrMac == null) {
                logWarning("Null MAC address provided, using 'new' instead", "null_mac");
                adrMac = "new";
            }

            if (adrMac.isEmpty()) {
                logWarning("Empty MAC address provided, using 'new' instead", "empty_mac");
                adrMac = "new";
            }

            sharedPreferences.edit().putString(KEY_ADR_MAC, adrMac).apply();
            logInfo("MAC address set: " + adrMac, "adr_mac_set");
        } catch (Exception e) {
            logException(e, "set_adr_mac_error", "Error setting MAC address");
        }
    }

    /**
     * Définit l'agence
     * @param agency Agence
     */
    public void setAgency(String agency) {
        try {
            if (agency == null) {
                logWarning("Null agency provided, using empty string instead", "null_agency");
                agency = "";
            }

            sharedPreferences.edit().putString(KEY_AGENCY, agency).apply();
            logInfo("Agency set: " + agency, "agency_set");
        } catch (Exception e) {
            logException(e, "set_agency_error", "Error setting agency");
        }
    }

    /**
     * Définit le groupe
     * @param group Groupe
     */
    public void setGroup(String group) {
        try {
            if (group == null) {
                logWarning("Null group provided, using empty string instead", "null_group");
                group = "";
            }

            sharedPreferences.edit().putString(KEY_GROUP, group).apply();
            logInfo("Group set: " + group, "group_set");
        } catch (Exception e) {
            logException(e, "set_group_error", "Error setting group");
        }
    }

    /**
     * Définit la résidence
     * @param residence Résidence
     */
    public void setResidence(String residence) {
        try {
            if (residence == null) {
                logWarning("Null residence provided, using empty string instead", "null_residence");
                residence = "";
            }

            sharedPreferences.edit().putString(KEY_RESIDENCE, residence).apply();
            logInfo("Residence set: " + residence, "residence_set");
        } catch (Exception e) {
            logException(e, "set_residence_error", "Error setting residence");
        }
    }

    /**
     * Récupère l'ID du membre
     * @return ID du membre, ou 'new' par défaut
     */
    public String getMbrId() {
        try {
            String value = sharedPreferences.getString(KEY_MBR_ID, "new");
            logInfo("Member ID retrieved: " + value, "mbr_id_get");
            return value;
        } catch (Exception e) {
            logException(e, "get_mbr_id_error", "Error getting member ID");
            return "new";
        }
    }

    /**
     * Récupère l'adresse MAC
     * @return Adresse MAC, ou 'new' par défaut
     */
    public String getAdrMac() {
        try {
            String value = sharedPreferences.getString(KEY_ADR_MAC, "new");
            logInfo("MAC address retrieved: " + value, "adr_mac_get");
            return value;
        } catch (Exception e) {
            logException(e, "get_adr_mac_error", "Error getting MAC address");
            return "new";
        }
    }

    /**
     * Récupère l'agence
     * @return Agence, ou chaîne vide par défaut
     */
    public String getAgency() {
        try {
            String value = sharedPreferences.getString(KEY_AGENCY, "");
            logInfo("Agency retrieved: " + value, "agency_get");
            return value;
        } catch (Exception e) {
            logException(e, "get_agency_error", "Error getting agency");
            return "";
        }
    }

    /**
     * Récupère le groupe
     * @return Groupe, ou chaîne vide par défaut
     */
    public String getGroup() {
        try {
            String value = sharedPreferences.getString(KEY_GROUP, "");
            logInfo("Group retrieved: " + value, "group_get");
            return value;
        } catch (Exception e) {
            logException(e, "get_group_error", "Error getting group");
            return "";
        }
    }

    /**
     * Récupère la résidence
     * @return Résidence, ou chaîne vide par défaut
     */
    public String getResidence() {
        try {
            String value = sharedPreferences.getString(KEY_RESIDENCE, "");
            logInfo("Residence retrieved: " + value, "residence_get");
            return value;
        } catch (Exception e) {
            logException(e, "get_residence_error", "Error getting residence");
            return "";
        }
    }

    /**
     * Efface toutes les préférences
     */
    public void clearAll() {
        try {
            logWarning("Clearing all preferences", "clear_all");
            sharedPreferences.edit().clear().apply();
            logInfo("All preferences cleared", "preferences_cleared");
        } catch (Exception e) {
            logException(e, "clear_all_error", "Error clearing all preferences");
        }
    }

//********* LOGGING METHODS

    private void logInfo(String message, String infoType) {
        try {
            crashlytics.log("INFO: " + message);
            Log.i(TAG, message);

            Bundle params = new Bundle();
            params.putString("info_type", infoType);
            params.putString("class", "NFCManager");
            params.putString("info_message", message);
            if (analytics != null) {
                analytics.logEvent("app_info", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logInfo: " + e.getMessage());
        }
    }

    private void logWarning(String message, String warningType) {
        try {
            crashlytics.log("WARNING: " + message);
            Log.w(TAG, message);

            Bundle params = new Bundle();
            params.putString("warning_type", warningType);
            params.putString("class", "NFCManager");
            params.putString("warning_message", message);
            if (analytics != null) {
                analytics.logEvent("app_warning", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logWarning: " + e.getMessage());
        }
    }

    private void logError(String message, String errorType) {
        try {
            crashlytics.log("ERROR: " + message);
            Log.e(TAG, message);

            Bundle params = new Bundle();
            params.putString("error_type", errorType);
            params.putString("class", "NFCManager");
            params.putString("error_message", message);
            if (analytics != null) {
                analytics.logEvent("app_error", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in logError: " + e.getMessage());
        }
    }

    private void logException(Exception e, String errorType, String context) {
        try {
            crashlytics.recordException(e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "No message";
            crashlytics.log("EXCEPTION: " + context + ": " + errorMessage);
            Log.e(TAG, context + ": " + errorMessage, e);

            Bundle params = new Bundle();
            params.putString("error_type", errorType);
            params.putString("class", "NFCManager");
            params.putString("error_message", errorMessage);
            params.putString("error_context", context);
            if (analytics != null) {
                analytics.logEvent("app_exception", params);
            }
        } catch (Exception loggingEx) {
            Log.e(TAG, "Error in logException: " + loggingEx.getMessage());
        }
    }

}
