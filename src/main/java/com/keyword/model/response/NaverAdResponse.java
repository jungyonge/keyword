package com.keyword.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NaverAdResponse {
    String token;

    public static NaverAdResponse of(String token) {
        return NaverAdResponse.builder()
                .token(token)
                .build();
    }
}
