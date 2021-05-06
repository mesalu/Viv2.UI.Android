package com.mesalu.viv2.android_ui.data.model;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a set of tokens, used by http data access clients for authorization
 */
public class TokenSet {
    protected String accessToken;
    protected String refreshToken;
    protected ZonedDateTime accessExpiry;
    protected ZonedDateTime refreshExpiry;
    protected UUID userId;
    protected String displayName;

    public TokenSet(String displayName, String accessToken, String refreshToken, ZonedDateTime accessExpiry, ZonedDateTime refreshExpiry, UUID userId) {
        this.displayName = displayName;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }
    public UUID getUserId() {
        return userId;
    }
    public ZonedDateTime getAccessExpiry() {
        return accessExpiry;
    }
    public ZonedDateTime getRefreshExpiry() {
        return refreshExpiry;
    }
}
