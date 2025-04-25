package org.orgaprop.controlprest.models;

public class ListResidModel {

//********* PRIVATES VARIABLES

    private int id = 0;
    private String ref = "";
    private String name = "";
    private String entry = "";
    private String adr = "";
    private String city = "";
    private String agc = "";
    private String grp = "";
    private boolean visited = false;
    private String last = "";

//********* STATIC VARIABLES

    private static final String TAG = "ListResidModel";

//********* CONSTRUCTORS

    public ListResidModel() {}
    public ListResidModel(String agc,String grp, int id, String ref, String name, String entry, String adr, String city, boolean visited, String last) {
        this.agc = agc;
        this.grp = grp;
        this.id = id;
        this.ref = ref;
        this.name = name;
        this.entry = entry;
        this.adr = adr;
        this.city = city;
        this.visited = visited;
        this.last = last;
    }

//********* PUBLIC FUNCTIONS

    public int getId() {
        return this.id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getRef() {
        return this.ref;
    }
    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEntry() {
        return this.entry;
    }
    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getAdress() {
        return this.adr;
    }
    public void setAdresse(String adr) {
        this.adr = adr;
    }

    public String getCity() {
        return this.city;
    }
    public void setCity(String cp, String city) {
        this.city = cp + " " + city;
    }

    public String getAgc() {
        return this.agc;
    }
    public void setAgc(String agc) {
        this.agc = agc;
    }

    public String getGrp() {
        return this.grp;
    }
    public void setGrp(String grp) {
        this.grp = grp;
    }

    public boolean isVisited() {
        return this.visited;
    }
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public String getLast() {
        return this.last;
    }
    public void setLast(String last) {
        this.last = last;
    }

}
