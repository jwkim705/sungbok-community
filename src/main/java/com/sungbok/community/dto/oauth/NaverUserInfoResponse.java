package com.sungbok.community.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverUserInfoResponse {
    private NaverResponse response;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverResponse {
        private String id;
        private String email;
        private String name;

        @JsonProperty("profile_image")
        private String profileImage;
    }
}
