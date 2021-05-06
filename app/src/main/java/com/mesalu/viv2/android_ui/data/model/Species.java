package com.mesalu.viv2.android_ui.data.model;

/**
 * Represents the information specifically pertaining to a species in the API.
 * For the API-side model this class reflects see 'SpeciesDto'
 */
public class Species {
    int id;
    String name;
    String scientificName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }
}
