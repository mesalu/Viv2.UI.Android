package com.mesalu.viv2.android_ui.data.model;

import com.google.gson.annotations.SerializedName;

public class Environment {
    private String id;

    @SerializedName("inhabitantId")
    private int occupantId;
    private String model;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOccupantId() {
        return occupantId;
    }

    public void setOccupantId(int occupantId) {
        this.occupantId = occupantId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
