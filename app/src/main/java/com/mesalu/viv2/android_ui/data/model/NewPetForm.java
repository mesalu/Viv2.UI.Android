package com.mesalu.viv2.android_ui.data.model;

/**
 * Mirror of Api's AppInterface.Dto.NewPetForm, represents a flattened version of a Pet
 * instance.
 */
public class NewPetForm {
    String name;
    String morph;
    int speciesId;

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

    public int getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
    }
}
