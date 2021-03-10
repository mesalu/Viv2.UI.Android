package com.mesalu.viv2.android_ui.data.model;

import java.util.UUID;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private UUID userId;
    private String displayName;
    private String token;

    public LoggedInUser(UUID userId, String displayName, String token) {
        this.userId = userId;
        this.displayName = displayName;
        this.token = token;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }
}