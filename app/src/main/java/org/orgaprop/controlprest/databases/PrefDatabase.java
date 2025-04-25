package org.orgaprop.controlprest.databases;

import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.OnConflictStrategy;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.orgaprop.controlprest.databases.dao.PrefDao;
import org.orgaprop.controlprest.models.Pref;

@Database(entities = {Pref.class}, version = 1, exportSchema = false)
public abstract class PrefDatabase extends RoomDatabase {

    public static final String ID_MBR = "id_mbr";
    public static final String ADR_MAC = "adr_mac";
    public static final String AGENCY = "agc";
    public static final String GROUP = "grp";
    public static final String RESIDENCE = "rsd";

    // --- SINGLETON ---
    private static volatile PrefDatabase INSTANCE;

    // --- DAO ---
    public abstract PrefDao mPrefDao();

    // --- INSTANCE ---
    public static PrefDatabase getInstance(Context context) {
        if( INSTANCE == null ) {
            synchronized (PrefDatabase.class) {
                if( INSTANCE == null ) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    PrefDatabase.class,
                                    "pref2.db")
                            .addCallback(prepopulateDatabase())
                            .build();
                }
            }
        }

        return INSTANCE;
    }

    // --- CALLBACK --
    private static RoomDatabase.Callback prepopulateDatabase() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);

                ContentValues contentValues = new ContentValues();

                contentValues.put("id", Long.parseLong("1"));
                contentValues.put("param", ID_MBR);
                contentValues.put("value", "new");

                db.insert("Pref", OnConflictStrategy.REPLACE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong("2"));
                contentValues.put("param", ADR_MAC);
                contentValues.put("value", "new");

                db.insert("Pref", OnConflictStrategy.REPLACE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong("3"));
                contentValues.put("param", AGENCY);
                contentValues.put("value", "");

                db.insert("Pref", OnConflictStrategy.REPLACE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong("4"));
                contentValues.put("param", GROUP);
                contentValues.put("value", "");

                db.insert("Pref", OnConflictStrategy.REPLACE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong("5"));
                contentValues.put("param", RESIDENCE);
                contentValues.put("value", "");

                db.insert("Pref", OnConflictStrategy.REPLACE, contentValues);
            }
        };
    }

}
