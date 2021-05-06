package com.mesalu.viv2.android_ui.data.model;

import androidx.annotation.Nullable;

public class Pet {
    int id;
    String name;
    String morph;
    Species species;

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

    public String getMorph() {
        return morph;
    }

    public void setMorph(String morph) {
        this.morph = morph;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(@Nullable Species species) {
        this.species = species;
    }
}
