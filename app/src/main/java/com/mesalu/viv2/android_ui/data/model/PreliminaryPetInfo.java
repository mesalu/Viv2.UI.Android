package com.mesalu.viv2.android_ui.data.model;

import com.google.gson.annotations.SerializedName;

@Deprecated
public class PreliminaryPetInfo {
    private Pet pet;

    @SerializedName("latestSample")
    private EnvDataSample sample;

    public PreliminaryPetInfo(Pet pet, EnvDataSample sample) {
        this.pet = pet;
        this.sample = sample;
    }

    public Pet getPet() {
        return pet;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }

    public EnvDataSample getSample() {
        return sample;
    }

    public void setSample(EnvDataSample sample) {
        this.sample = sample;
    }
}
