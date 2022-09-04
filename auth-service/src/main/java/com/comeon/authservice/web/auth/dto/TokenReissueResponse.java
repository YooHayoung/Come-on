package com.comeon.authservice.web.auth.dto;

import lombok.Getter;

@Getter
public class TokenReissueResponse {

    private final String accessToken;
    private final Long expiry;

    public TokenReissueResponse(String accessToken, Long expiry) {
        this.accessToken = accessToken;
        this.expiry = expiry;
    }
}