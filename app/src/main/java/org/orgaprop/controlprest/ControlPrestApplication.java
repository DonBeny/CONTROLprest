package org.orgaprop.controlprest;

import android.app.Application;
import android.util.Log;

import org.orgaprop.controlprest.utils.ToastManager;

/**
 * Classe Application principale pour l'application ControlPrest.
 * Initialise les composants globaux au démarrage de l'application.
 */
public class ControlPrestApplication extends Application {

    private static final String TAG = "ControlPrestApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialiser le ToastManager avec le contexte de l'application
        ToastManager.init(this);
        Log.d(TAG, "ToastManager initialized");

        // Initialiser d'autres composants si nécessaire...
    }
}
