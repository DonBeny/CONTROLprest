package org.orgaprop.controlprest.databases.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.orgaprop.controlprest.models.Pref;

/**
 * Interface DAO (Data Access Object) pour les opérations sur les préférences de l'application.
 * Améliorée avec gestion des conflits et des transactions.
 */
@Dao
public interface PrefDao {

    /**
     * Récupère une préférence à partir de son paramètre.
     * @param param Le nom du paramètre
     * @return LiveData contenant la préférence
     */
    @Query("SELECT * FROM Pref WHERE param = :param")
    LiveData<Pref> getPrefFromParam(String param);

    /**
     * Récupère une préférence à partir de son id.
     * @param paramId L'ID du paramètre
     * @return LiveData contenant la préférence
     */
    @Query("SELECT * FROM Pref WHERE id = :paramId")
    LiveData<Pref> getPrefFromId(long paramId);

    /**
     * Récupère une préférence à partir de son paramètre avec un Cursor.
     * @param param Le nom du paramètre
     * @return Cursor contenant la préférence
     */
    @Query("SELECT * FROM Pref WHERE param = :param")
    Cursor getPrefFromParamWithCursor(String param);

    /**
     * Récupère une préférence à partir de son id avec un Cursor.
     * @param paramId L'ID du paramètre
     * @return Cursor contenant la préférence
     */
    @Query("SELECT * FROM Pref WHERE id = :paramId")
    Cursor getPrefFromIdWithCursor(long paramId);

    /**
     * Insère une nouvelle préférence avec gestion des conflits.
     * @param pref La préférence à insérer
     * @return L'ID de la ligne insérée
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPref(Pref pref);

    /**
     * Met à jour une préférence existante.
     * @param pref La préférence à mettre à jour
     * @return Le nombre de lignes mises à jour
     */
    @Update
    int updatePref(Pref pref);

    /**
     * Supprime une préférence à partir de son paramètre.
     * @param param Le nom du paramètre à supprimer
     * @return Le nombre de lignes supprimées
     */
    @Query("DELETE FROM Pref WHERE param = :param")
    int deletePref(String param);

    /**
     * Supprime une préférence à partir de son id.
     * @param paramId L'ID du paramètre à supprimer
     * @return Le nombre de lignes supprimées
     */
    @Query("DELETE FROM Pref WHERE id = :paramId")
    int deletePref(long paramId);

    /**
     * Vérifie si une préférence existe.
     * @param param Le nom du paramètre à vérifier
     * @return 1 si existe, 0 sinon
     */
    @Query("SELECT COUNT(*) FROM Pref WHERE param = :param")
    int prefExists(String param);
}
