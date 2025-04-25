package org.orgaprop.controlprest.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.orgaprop.controlprest.databases.PrefDatabase;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utilitaire de diagnostic pour les problèmes de base de données.
 * Classe d'aide pour résoudre et détecter les problèmes de base de données.
 */
public class DBTroubleshooter {

    private static final String TAG = "DBTroubleshooter";
    private static final Executor dbExecutor = Executors.newSingleThreadExecutor();

    /**
     * Vérifie l'intégrité de la base de données des préférences
     *
     * @param context Le contexte de l'application
     * @return true si la base de données est intègre, false sinon
     */
    public static boolean checkPrefDatabaseIntegrity(Context context) {
        try {
            Cursor cursor = PrefDatabase.getInstance(context).getOpenHelper()
                    .getReadableDatabase()
                    .query("PRAGMA integrity_check");

            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);
                cursor.close();

                boolean isOk = "ok".equalsIgnoreCase(result);
                if (!isOk) {
                    Log.e(TAG, "Database integrity check failed: " + result);
                    Toast.makeText(context, "Database integrity check failed: " + result, Toast.LENGTH_SHORT).show();
                }
                return isOk;
            }
            cursor.close();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking database integrity", e);
            Toast.makeText(context, "Error checking database integrity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Répare la base de données corrompue en la recréant si nécessaire
     *
     * @param context Le contexte de l'application
     * @return true si la réparation a réussi, false sinon
     */
    public static boolean repairDatabase(Context context) {
        try {
            // Fermer la connexion à la base de données
            PrefDatabase.closeDatabase();

            // Supprimer le fichier de base de données
            File dbFile = context.getDatabasePath("pref2.db");
            if (dbFile.exists() && dbFile.delete()) {
                Log.d(TAG, "Corrupted database deleted successfully");
                Toast.makeText(context, "Corrupted database deleted successfully", Toast.LENGTH_SHORT).show();

                // Recréer la base de données
                PrefDatabase.getInstance(context);
                return true;
            } else {
                Log.e(TAG, "Failed to delete corrupted database");
                Toast.makeText(context, "Failed to delete corrupted database", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error repairing database", e);
            Toast.makeText(context, "Error repairing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Analyse de manière asynchrone l'état de la base de données et tente de la réparer si nécessaire
     *
     * @param context Le contexte de l'application
     * @param callback Callback pour recevoir le résultat
     */
    public static void diagnoseAndRepair(Context context, DiagnosisCallback callback) {
        dbExecutor.execute(() -> {
            try {
                // Vérifier si la base de données existe
                File dbFile = context.getDatabasePath("pref2.db");
                if (!dbFile.exists()) {
                    Log.w(TAG, "Database file doesn't exist, will be created");
                    Toast.makeText(context, "Database file doesn't exist, will be created", Toast.LENGTH_SHORT).show();
                    // La base sera créée automatiquement lors de la première utilisation
                    callback.onResult(true, "Database will be created");
                    return;
                }

                // Vérifier l'intégrité
                boolean integrityOk = checkPrefDatabaseIntegrity(context);
                if (!integrityOk) {
                    Log.w(TAG, "Database integrity check failed, attempting repair");
                    Toast.makeText(context, "Database integrity check failed, attempting repair", Toast.LENGTH_SHORT).show();
                    boolean repaired = repairDatabase(context);
                    callback.onResult(repaired, repaired ?
                            "Database repaired successfully" :
                            "Failed to repair database");
                } else {
                    callback.onResult(true, "Database is in good condition");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during database diagnosis", e);
                Toast.makeText(context, "Error during database diagnosis: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                callback.onResult(false, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtient des informations sur la base de données pour le débogage
     *
     * @param context Le contexte de l'application
     * @return Chaîne contenant les informations de débogage
     */
    public static String getDatabaseDebugInfo(Context context) {
        StringBuilder info = new StringBuilder();
        try {
            File dbFile = context.getDatabasePath("pref2.db");

            info.append("Database Path: ").append(dbFile.getAbsolutePath()).append("\n");
            info.append("Exists: ").append(dbFile.exists()).append("\n");

            if (dbFile.exists()) {
                info.append("Size: ").append(dbFile.length()).append(" bytes\n");
                info.append("Last Modified: ").append(dbFile.lastModified()).append("\n");
                info.append("Readable: ").append(dbFile.canRead()).append("\n");
                info.append("Writable: ").append(dbFile.canWrite()).append("\n");

                // Vérifier les autorisations supplémentaires sur les versions récentes d'Android
                try {
                    info.append("Parent: ").append(Objects.requireNonNull(dbFile.getParentFile()).getName()).append("\n");
                } catch (Exception e) {
                    info.append("Error accessing file attributes: ").append(e.getMessage()).append("\n");
                }

                try {
                    // Vérifier si on peut ouvrir la base de données
                    boolean canOpen = PrefDatabase.getInstance(context).isOpen();
                    info.append("Can Open: ").append(canOpen).append("\n");
                } catch (SQLiteException e) {
                    info.append("Open Error: ").append(e.getMessage()).append("\n");
                }
            }

            // Ajouter des informations sur l'espace disque
            info.append("Storage Info:\n");
            info.append("  Available: ").append(dbFile.getFreeSpace() / 1024 / 1024).append(" MB\n");
            info.append("  Total: ").append(dbFile.getTotalSpace() / 1024 / 1024).append(" MB\n");

        } catch (Exception e) {
            info.append("Error getting debug info: ").append(e.getMessage());
        }

        return info.toString();
    }

    /**
     * Interface de rappel pour les opérations de diagnostic
     */
    public interface DiagnosisCallback {
        void onResult(boolean success, String message);
    }
}
