package org.orgaprop.controlprest.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Classe utilitaire qui gère l'affichage des Toasts dans l'application.
 * Permet d'afficher des Toasts depuis n'importe quelle classe, y compris les méthodes statiques,
 * sans avoir besoin d'une référence directe au Context.
 */
public class ToastManager {

    private static Context applicationContext;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Initialise le ToastManager avec le contexte de l'application.
     * Cette méthode doit être appelée dans la classe Application ou dans l'activité principale.
     *
     * @param context Le contexte de l'application
     */
    public static void init(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
    }

    /**
     * Affiche un Toast court.
     *
     * @param message Le message à afficher
     */
    public static void showShort(final String message) {
        if (applicationContext != null) {
            mainHandler.post(() -> Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Affiche un Toast long.
     *
     * @param message Le message à afficher
     */
    public static void showLong(final String message) {
        if (applicationContext != null) {
            mainHandler.post(() -> Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Affiche un Toast d'erreur (long).
     *
     * @param message Le message d'erreur à afficher
     */
    public static void showError(final String message) {
        if (applicationContext != null) {
            mainHandler.post(() -> Toast.makeText(applicationContext, "Erreur: " + message, Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Vérifie si le ToastManager est initialisé.
     *
     * @return true si initialisé, false sinon
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }

}
