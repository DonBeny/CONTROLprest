package org.orgaprop.controlprest.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.orgaprop.controlprest.databases.PrefDatabase;
import org.orgaprop.controlprest.models.Pref;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Prefs {

//************ PRIVATE VARIABLES

    private static final String TAG = "Prefs";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 500;

    // Exécuteur pour les opérations de base de données
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    // Handler pour poster les résultats sur le thread principal
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Contexte de l'application
    private static Context mContext;

//************ PUBLIC VARIABLES



//************ CONSTRUCTORS

    /**
     * Constructeur.
     * @param context Contexte de l'application
     */
    public Prefs(Context context) {
        mContext = context.getApplicationContext(); // Utiliser le contexte d'application pour éviter les fuites mémoire
    }

//************ SETTERS

    /**
     * Enregistre l'ID membre.
     * @param idMbr ID du membre
     */
    public void setMbr(String idMbr) {
        setPref(1, PrefDatabase.ID_MBR, idMbr, null);
    }

    /**
     * Enregistre l'adresse MAC.
     * @param adrMac Adresse MAC
     */
    public void setAdrMac(String adrMac) {
        setPref(2, PrefDatabase.ADR_MAC, adrMac, null);
    }

    /**
     * Enregistre l'agence.
     * @param agency Agence
     */
    public void setAgency(String agency) {
        setPref(3, PrefDatabase.AGENCY, agency, null);
    }

    /**
     * Enregistre le groupe.
     * @param group Groupe
     */
    public void setGroup(String group) {
        setPref(4, PrefDatabase.GROUP, group, null);
    }

    /**
     * Enregistre la résidence.
     * @param residence Résidence
     */
    public void setResidence(String residence) {
        setPref(5, PrefDatabase.RESIDENCE, residence, null);
    }

    /**
     * Méthode générique pour définir une préférence avec gestion des erreurs et retries.
     *
     * @param id ID de la préférence
     * @param param Nom du paramètre
     * @param value Valeur du paramètre
     * @param callback Callback optionnel pour notifier la fin de l'opération
     */
    private void setPref(long id, String param, String value, Callback<Boolean> callback) {
        dbExecutor.execute(() -> {
            int retryCount = 0;
            boolean success = false;
            Exception lastException = null;

            while (retryCount < MAX_RETRY_COUNT && !success) {
                try {
                    ContentValues values = new ContentValues();
                    values.put("id", id);
                    values.put("param", param);
                    values.put("value", value);

                    PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));
                    success = true;
                    Log.d(TAG, "Successfully set preference: " + param + " = " + value);
                    Toast.makeText(mContext, "Successfully set preference: " + param + " = " + value, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    lastException = e;
                    Log.e(TAG, "Error setting preference: " + param + ", attempt " + (retryCount + 1), e);
                    Toast.makeText(mContext, "Error setting preference: " + param + ", attempt " + (retryCount + 1) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    retryCount++;

                    // Attendre avant de réessayer
                    if (retryCount < MAX_RETRY_COUNT) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            // Notifier le résultat si un callback est fourni
            if (callback != null) {
                final boolean finalSuccess = success;
                final Exception finalException = lastException;
                mainHandler.post(() -> {
                    if (finalSuccess) {
                        callback.onResult(true);
                    } else {
                        callback.onError(finalException);
                    }
                });
            }
        });
    }

//************ GETTERS

    /**
     * Récupère l'ID membre.
     * @param callback Callback pour recevoir le résultat
     */
    public void getMbr(Callback<String> callback) {
        getPref(PrefDatabase.ID_MBR, "new", callback);
    }

    /**
     * Récupère l'adresse MAC.
     * @param callback Callback pour recevoir le résultat
     */
    public void getAdrMac(Callback<String> callback) {
        getPref(PrefDatabase.ADR_MAC, "new", callback);
    }

    /**
     * Récupère l'agence.
     * @param callback Callback pour recevoir le résultat
     */
    public void getAgency(Callback<String> callback) {
        getPref(PrefDatabase.AGENCY, "", callback);
    }

    /**
     * Récupère le groupe.
     * @param callback Callback pour recevoir le résultat
     */
    public void getGroup(Callback<String> callback) {
        getPref(PrefDatabase.GROUP, "", callback);
    }

    /**
     * Récupère la résidence.
     * @param callback Callback pour recevoir le résultat
     */
    public void getResidence(Callback<String> callback) {
        getPref(PrefDatabase.RESIDENCE, "", callback);
    }

    /**
     * Méthode générique pour récupérer une préférence avec gestion des erreurs et retries.
     *
     * @param param Nom du paramètre
     * @param defaultValue Valeur par défaut
     * @param callback Callback pour recevoir le résultat
     */
    private void getPref(String param, String defaultValue, Callback<String> callback) {
        if (callback == null) {
            Log.w(TAG, "Callback must not be null");
            Toast.makeText(mContext, "Callback must not be null", Toast.LENGTH_SHORT).show();
            return;
        }

        dbExecutor.execute(() -> {
            int retryCount = 0;
            String result = defaultValue;
            Exception lastException = null;
            boolean success = false;

            while (retryCount < MAX_RETRY_COUNT && !success) {
                try (Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(param)) {

                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow("value"));
                        success = true;
                    } else {
                        success = true;
                    }
                } catch (Exception e) {
                    lastException = e;
                    Log.e(TAG, "Error getting preference: " + param + ", attempt " + (retryCount + 1), e);
                    Toast.makeText(mContext, "Error getting preference: " + param + ", attempt " + (retryCount + 1) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    retryCount++;

                    // Attendre avant de réessayer
                    if (retryCount < MAX_RETRY_COUNT) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            // Notifier le résultat
            final String finalResult = result;
            final Exception finalException = lastException;
            boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (finalSuccess) {
                    callback.onResult(finalResult);
                } else {
                    callback.onError(finalException);
                    // Fournir également la valeur par défaut en cas d'erreur
                    callback.onResult(defaultValue);
                }
            });
        });
    }

//************** INTERFACES

    /**
     * Interface de rappel étendue pour une meilleure gestion des erreurs.
     */
    public interface Callback<T> {
        void onResult(T result);
        default void onError(Exception e) {
            Log.e(TAG, "Operation failed", e);
            Toast.makeText(mContext, "Operation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
