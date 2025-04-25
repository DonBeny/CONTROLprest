package org.orgaprop.controlprest.databases;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.OnConflictStrategy;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.orgaprop.controlprest.databases.dao.PrefDao;
import org.orgaprop.controlprest.models.Pref;
import org.orgaprop.controlprest.utils.ToastManager;

@Database(entities = {Pref.class}, version = 1, exportSchema = false)
public abstract class PrefDatabase extends RoomDatabase {

    private static final String TAG = "PrefDatabase";

    // Clés de préférences
    public static final String ID_MBR = "id_mbr";
    public static final String ADR_MAC = "adr_mac";
    public static final String AGENCY = "agc";
    public static final String GROUP = "grp";
    public static final String RESIDENCE = "rsd";

    // --- SINGLETON ---
    private static volatile PrefDatabase INSTANCE;

    // --- DAO ---
    public abstract PrefDao mPrefDao();

    /**
     * Obtient une instance unique de la base de données.
     * Amélioré avec gestion des erreurs et debogage.
     *
     * @param context Le contexte de l'application
     * @return L'instance de PrefDatabase
     */
    public static PrefDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PrefDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        PrefDatabase.class,
                                        "pref2.db")
                                .addCallback(prepopulateDatabase())
                                .fallbackToDestructiveMigration() // En cas d'erreur de migration, recréer la BDD
                                .build();
                        Log.d(TAG, "Database instance created successfully");
                        Toast.makeText(context, "Database instance created successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating database instance", e);
                        Toast.makeText(context, "Error creating database instance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        throw new RuntimeException("Critical database error", e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Prépare la base de données en insérant les valeurs initiales.
     * Gestion d'erreurs améliorée.
     *
     * @return Le callback pour la création de la base
     */
    private static RoomDatabase.Callback prepopulateDatabase() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                Log.d(TAG, "Creating database and populating initial values");

                try {
                    // Insertion des valeurs par défaut en utilisant des transactions
                    db.beginTransaction();

                    insertInitialPref(db, 1, ID_MBR, "new");
                    insertInitialPref(db, 2, ADR_MAC, "new");
                    insertInitialPref(db, 3, AGENCY, "");
                    insertInitialPref(db, 4, GROUP, "");
                    insertInitialPref(db, 5, RESIDENCE, "");

                    db.setTransactionSuccessful();
                    Log.d(TAG, "Database populated successfully");
                    ToastManager.showShort("Database populated successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error populating database", e);
                    ToastManager.showError("Error populating database: " + e.getMessage());
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                Log.d(TAG, "Database opened");
                ToastManager.showShort("Database opened");
            }
        };
    }

    /**
     * Méthode utilitaire pour insérer une préférence initiale.
     *
     * @param db Base de données
     * @param id ID de la préférence
     * @param param Nom du paramètre
     * @param value Valeur du paramètre
     */
    private static void insertInitialPref(SupportSQLiteDatabase db, long id, String param, String value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", id);
        contentValues.put("param", param);
        contentValues.put("value", value);

        long result = db.insert("Pref", OnConflictStrategy.REPLACE, contentValues);
        if (result == -1) {
            Log.w(TAG, "Failed to insert initial preference: " + param);
            ToastManager.showShort("Failed to insert initial preference: " + param);
        }
    }

    /**
     * Ferme proprement la base de données.
     * Utile pour libérer les ressources en fin d'application.
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            Log.d(TAG, "Database closed");
            ToastManager.showShort("Database closed");
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    /**
     * Vérifie si la base de données est ouverte.
     *
     * @return true si la base est ouverte, false sinon
     */
    public boolean isOpen() {
        return INSTANCE != null && INSTANCE.getOpenHelper().getWritableDatabase().isOpen();
    }

}
