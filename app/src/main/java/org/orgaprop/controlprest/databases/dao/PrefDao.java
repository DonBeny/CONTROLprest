package org.orgaprop.controlprest.databases.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.orgaprop.controlprest.models.Pref;

@Dao
public interface PrefDao {

    @Query("SELECT * FROM Pref WHERE param = :param")
    LiveData<Pref> getPrefFromParam(String param);
    @Query("SELECT * FROM Pref WHERE id = :paramId") LiveData<Pref> getPrefFromId(long paramId);
    @Query("SELECT * FROM Pref WHERE param = :param")
    Cursor getPrefFromParamWithCursor(String param);
    @Query("SELECT * FROM Pref WHERE id = :paramId") Cursor getPrefFromIdWithCursor(long paramId);

    @Insert
    long insertPref(Pref pref);

    @Update
    int updatePref(Pref pref);

    @Query("DELETE FROM Pref WHERE param = :param") int deletePref(String param);
    @Query("DELETE FROM Pref WHERE id = :paramId") int deletePref(long paramId);

}
