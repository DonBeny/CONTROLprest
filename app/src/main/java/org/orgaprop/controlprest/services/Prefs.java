package org.orgaprop.controlprest.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import org.orgaprop.controlprest.databases.PrefDatabase;
import org.orgaprop.controlprest.models.Pref;

import java.util.concurrent.Executors;

public class Prefs {

//************ PRIVATE VARIABLES

    private Context mContext;

//************ PUBLIC VARIABLES



//************ CONSTRUCTORS

    public Prefs(Context context) {
        mContext = context;
    }

//************ SETTERS

    public void setMbr(String idMbr) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong("1"));
            values.put("param", PrefDatabase.ID_MBR);
            values.put("value", idMbr);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setAdrMac(String adrMac) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong("2"));
            values.put("param", PrefDatabase.ADR_MAC);
            values.put("value", adrMac);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setAgency(String agency) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong("3"));
            values.put("param", PrefDatabase.AGENCY);
            values.put("value", agency);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setGroup(String group) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong("4"));
            values.put("param", PrefDatabase.GROUP);
            values.put("value", group);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setResidence(String residence) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong("5"));
            values.put("param", PrefDatabase.RESIDENCE);
            values.put("value", residence);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }

//************ GETTERS

    public void getMbr(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "new";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.ID_MBR);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getAdrMac(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "new";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.ADR_MAC);

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getAgency(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.AGENCY);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getGroup(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.GROUP);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getResidence(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.RESIDENCE);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }

//************** INTERFACES

    public interface Callback<T> {
        void onResult(T result);
    }

}
