package org.orgaprop.controlprest.utils;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.HashMap;
import java.util.Map;

public class LogManager {

    private static LogManager instance;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics analytics;
    private boolean isInitialized = false;
    private Context appContext;

    // Constantes pour les niveaux de log
    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;
    public static final int ASSERT = 6;

    // Nom de l'événement par défaut pour les erreurs
    private static final String EVENT_APP_ERROR = "app_error";

    /**
     * Constructeur privé pour Singleton
     */
    private LogManager() {
        // Construction privée
    }

    /**
     * Obtient l'instance unique de LogManager
     * @return Instance unique de LogManager
     */
    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    /**
     * Initialise LogManager avec le contexte de l'application
     * @param context Contexte d'application
     */
    public void init(Context context) {
        if (context == null) {
            Log.e("LogManager", "Context cannot be null in init()");
            return;
        }

        try {
            appContext = context.getApplicationContext();
            crashlytics = FirebaseCrashlytics.getInstance();
            analytics = FirebaseAnalytics.getInstance(appContext);

            // Configuration de base de Crashlytics
            crashlytics.setCustomKey("deviceModel", Build.MODEL);
            crashlytics.setCustomKey("deviceManufacturer", Build.MANUFACTURER);
            crashlytics.setCustomKey("osVersion", Build.VERSION.RELEASE);
            crashlytics.log("LogManager initialized");

            // Événement d'initialisation d'Analytics
            Bundle initParams = new Bundle();
            initParams.putString("device_model", Build.MODEL);
            initParams.putString("manufacturer", Build.MANUFACTURER);
            initParams.putString("os_version", Build.VERSION.RELEASE);
            analytics.logEvent("app_initialized", initParams);

            isInitialized = true;
            Log.i("LogManager", "LogManager initialized successfully");
        } catch (Exception e) {
            Log.e("LogManager", "Error initializing LogManager", e);
        }
    }

    /**
     * Vérifie si LogManager est correctement initialisé
     */
    private void checkInit() {
        if (!isInitialized) {
            Log.e("LogManager", "LogManager not initialized! Call init() first.");
        }
    }

    /**
     * Log verbose (niveau le plus détaillé)
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     */
    public void v(String tag, String message) {
        Log.v(tag, message);
        logToCrashlytics(VERBOSE, tag, message);
    }

    /**
     * Log debug
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     */
    public void d(String tag, String message) {
        Log.d(tag, message);
        logToCrashlytics(DEBUG, tag, message);
    }

    /**
     * Log info
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     */
    public void i(String tag, String message) {
        Log.i(tag, message);
        logToCrashlytics(INFO, tag, message);
    }

    /**
     * Log warning
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     */
    public void w(String tag, String message) {
        Log.w(tag, message);
        logToCrashlytics(WARN, tag, message);
    }

    /**
     * Log error
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     */
    public void e(String tag, String message) {
        Log.e(tag, message);
        logToCrashlytics(ERROR, tag, message);

        // Pour les erreurs, on ajoute également un événement Analytics
        if (isInitialized) {
            Bundle errorParams = new Bundle();
            errorParams.putString("error_tag", tag);
            errorParams.putString("error_message", message);
            errorParams.putString("error_type", "error_log");
            analytics.logEvent(EVENT_APP_ERROR, errorParams);
        }
    }

    /**
     * Log error avec exception
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     * @param exception Exception à logger
     */
    public void e(String tag, String message, Throwable exception) {
        Log.e(tag, message, exception);
        logToCrashlytics(ERROR, tag, message);

        if (isInitialized && crashlytics != null) {
            crashlytics.recordException(exception);
        }

        if (isInitialized && analytics != null) {
            Bundle errorParams = new Bundle();
            errorParams.putString("error_tag", tag);
            errorParams.putString("error_message", message);
            errorParams.putString("exception_message", exception.getMessage());
            errorParams.putString("exception_class", exception.getClass().getSimpleName());
            errorParams.putString("error_type", "exception");
            analytics.logEvent(EVENT_APP_ERROR, errorParams);
        }
    }

    /**
     * Log d'assertion - niveau le plus critique
     * @param tag Tag pour identifier la source
     * @param message Message à logger
     */
    public void wtf(String tag, String message) {
        Log.wtf(tag, message);
        logToCrashlytics(ASSERT, tag, message);

        if (isInitialized) {
            Bundle errorParams = new Bundle();
            errorParams.putString("error_tag", tag);
            errorParams.putString("error_message", message);
            errorParams.putString("error_type", "assertion_failed");
            errorParams.putString("severity", "critical");
            analytics.logEvent(EVENT_APP_ERROR, errorParams);
        }
    }

    /**
     * Enregistre l'id de l'utilisateur connecté
     * @param userId ID unique de l'utilisateur
     */
    public void setUserId(String userId) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Log.w("LogManager", "setUserId called with empty or null id");
            return;
        }

        try {
            crashlytics.setUserId(userId);
            analytics.setUserId(userId);
            Log.d("LogManager", "User ID set: " + userId);
        } catch (Exception e) {
            Log.e("LogManager", "Error setting user ID", e);
        }
    }

    /**
     * Définit une clé personnalisée pour Crashlytics
     * @param key Nom de la clé
     * @param value Valeur de la clé
     */
    public void setCustomKey(String key, String value) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        try {
            crashlytics.setCustomKey(key, value != null ? value : "null");
        } catch (Exception e) {
            Log.e("LogManager", "Error setting custom key: " + key, e);
        }
    }

    /**
     * Définit une clé personnalisée pour Crashlytics (valeur numérique)
     * @param key Nom de la clé
     * @param value Valeur de la clé
     */
    public void setCustomKey(String key, int value) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        try {
            crashlytics.setCustomKey(key, value);
        } catch (Exception e) {
            Log.e("LogManager", "Error setting custom key: " + key, e);
        }
    }

    /**
     * Définit une clé personnalisée pour Crashlytics (valeur booléenne)
     * @param key Nom de la clé
     * @param value Valeur de la clé
     */
    public void setCustomKey(String key, boolean value) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        try {
            crashlytics.setCustomKey(key, value);
        } catch (Exception e) {
            Log.e("LogManager", "Error setting custom key: " + key, e);
        }
    }

    /**
     * Enregistre une exception non fatale dans Crashlytics
     * @param exception Exception à enregistrer
     */
    public void recordException(Throwable exception) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        if (exception == null) {
            Log.w("LogManager", "recordException called with null exception");
            return;
        }

        try {
            crashlytics.recordException(exception);

            // Enregistrer également dans Analytics
            Bundle errorParams = new Bundle();
            errorParams.putString("exception_class", exception.getClass().getSimpleName());
            errorParams.putString("exception_message", exception.getMessage());
            errorParams.putString("error_type", "recorded_exception");
            analytics.logEvent(EVENT_APP_ERROR, errorParams);

            Log.d("LogManager", "Exception recorded: " + exception.getMessage());
        } catch (Exception e) {
            Log.e("LogManager", "Error recording exception", e);
        }
    }

    /**
     * Enregistre un événement Analytics
     * @param eventName Nom de l'événement
     */
    public void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    /**
     * Enregistre un événement Analytics avec des paramètres
     * @param eventName Nom de l'événement
     * @param params Map de paramètres (clé-valeur)
     */
    public void logEvent(String eventName, Map<String, Object> params) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        if (eventName == null || eventName.isEmpty()) {
            Log.w("LogManager", "logEvent called with empty or null event name");
            return;
        }

        try {
            Bundle bundle = null;
            if (params != null && !params.isEmpty()) {
                bundle = new Bundle();
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof String) {
                        bundle.putString(key, (String) value);
                    } else if (value instanceof Integer) {
                        bundle.putInt(key, (Integer) value);
                    } else if (value instanceof Long) {
                        bundle.putLong(key, (Long) value);
                    } else if (value instanceof Double) {
                        bundle.putDouble(key, (Double) value);
                    } else if (value instanceof Boolean) {
                        bundle.putBoolean(key, (Boolean) value);
                    } else if (value != null) {
                        bundle.putString(key, value.toString());
                    }
                }
            }

            analytics.logEvent(eventName, bundle);
            Log.d("LogManager", "Analytics event logged: " + eventName);
        } catch (Exception e) {
            Log.e("LogManager", "Error logging Analytics event: " + eventName, e);
        }
    }

    /**
     * Logique pour enregistrer dans Crashlytics
     */
    private void logToCrashlytics(int level, String tag, String message) {
        if (!isInitialized || crashlytics == null) {
            return;
        }

        String logMessage = tag + ": " + message;
        crashlytics.log(logMessage);
    }

    /**
     * Enregistre la navigation vers un écran (vue)
     * @param screenName Nom de l'écran
     * @param screenClass Nom de la classe d'écran (optionnel)
     */
    public void logScreenView(String screenName, String screenClass) {
        if (!isInitialized) {
            checkInit();
            return;
        }

        try {
            Bundle screenViewParams = new Bundle();
            screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            if (screenClass != null && !screenClass.isEmpty()) {
                screenViewParams.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewParams);

            // Aussi enregistrer dans Crashlytics pour le contexte
            crashlytics.log("Screen viewed: " + screenName);

            Log.d("LogManager", "Screen view logged: " + screenName);
        } catch (Exception e) {
            Log.e("LogManager", "Error logging screen view", e);
        }
    }

    /**
     * Utilitaire pour créer facilement un Map de paramètres
     * @return Un nouveau Map pour les paramètres d'événement
     */
    public Map<String, Object> createParamsMap() {
        return new HashMap<>();
    }

}
