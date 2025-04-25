package org.orgaprop.controlprest.models;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entité représentant une préférence dans la base de données
 */
@Entity
public class Pref {

    @PrimaryKey
    private long id;

    @NonNull
    private String param = "";

    @NonNull
    private String value = "";

    /**
     * Constructeur par défaut (requis pour Room)
     */
    @Ignore
    public Pref() {}

    /**
     * Constructeur avec paramètres
     * @param param Nom du paramètre
     * @param value Valeur du paramètre
     */
    public Pref(@NonNull String param, @NonNull String value) {
        this.param = param;
        this.value = value;
    }

    /**
     * Constructeur complet
     * @param id Identifiant
     * @param param Nom du paramètre
     * @param value Valeur du paramètre
     */
    @Ignore
    public Pref(long id, @NonNull String param, @NonNull String value) {
        this.id = id;
        this.param = param;
        this.value = value;
    }

    // --- GETTERS ---

    /**
     * @return Identifiant de la préférence
     */
    public long getId() {
        return this.id;
    }

    /**
     * @return Nom du paramètre
     */
    @NonNull
    public String getParam() {
        return this.param;
    }

    /**
     * @return Valeur du paramètre
     */
    @NonNull
    public String getValue() {
        return this.value;
    }

    // --- SETTERS ---

    /**
     * Définit l'identifiant de la préférence
     * @param id Identifiant à définir
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Définit le nom du paramètre
     * @param param Nom du paramètre à définir
     */
    public void setParam(@NonNull String param) {
        this.param = param;
    }

    /**
     * Définit la valeur du paramètre
     * @param value Valeur du paramètre à définir
     */
    public void setValue(@NonNull String value) {
        this.value = value;
    }

    // --- UTILS ---

    /**
     * Crée une instance de Pref à partir de ContentValues
     * @param values ContentValues contenant les données
     * @return Instance de Pref créée
     */
    public static Pref fromContentValues(@Nullable ContentValues values) {
        final Pref pref = new Pref();

        if (values != null) {
            if (values.containsKey("id")) {
                pref.setId(values.getAsLong("id"));
            }
            if (values.containsKey("param")) {
                pref.setParam(values.getAsString("param"));
            }
            if (values.containsKey("value")) {
                pref.setValue(values.getAsString("value"));
            }
        }

        return pref;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pref pref = (Pref) o;

        if (id != pref.id) return false;
        if (!param.equals(pref.param)) return false;
        return value.equals(pref.value);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(id);
        result = 31 * result + param.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Pref{" +
                "id=" + id +
                ", param='" + param + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
