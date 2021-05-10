package com.mesalu.viv2.android_ui.data.model;

import java.util.List;

public class NodeController {
    String id;
    String ownerId;
    List<String> environmentIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getEnvironmentIds() {
        return environmentIds;
    }

    public void setEnvironmentIds(List<String> environmentIds) {
        this.environmentIds = environmentIds;
    }
}
