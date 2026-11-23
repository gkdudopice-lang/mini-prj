
package com.human.mini_prj.service;

import com.human.mini_prj.dto.NaverUserInfo;
import com.human.mini_prj.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

// 네이버 서버와 통신하는 부분만 담당
@Slf4j
@Service
public class NaverOAuthService {

    private static final String TOKEN_URL =
            "https://nid.naver.com/oauth2.0/token";

    private static final String USER_URL =
            "https://openapi.naver.com/v1/nid/me";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient = RestClient.create();

    private final String clientId;
    private final String clientSecret;


    public NaverOAuthService(
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // ① 인가 코드 → 네이버 Access Token
    public String getAccessToken(String code, String state) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("state", state);

        try {
            Map<String, Object> res = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_TYPE);

            if (res == null || res.get("access_token") == null) {
                throw naverFailed();
            }

            return (String) res.get("access_token");

        } catch (RestClientException e) {
            log.warn("네이버 토큰 요청 실패: {}", e.getMessage());
            throw naverFailed();
        }
    }

    // ② 네이버 Access Token → 사용자 정보
    @SuppressWarnings("unchecked")
    public NaverUserInfo getUserInfo(String naverAccessToken) {

        try {
            Map<String, Object> res = restClient.get()
                    .uri(USER_URL)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + naverAccessToken
                    )
                    .retrieve()
                    .body(MAP_TYPE);

            if (res == null || res.get("response") == null) {
                throw naverFailed();
            }

            Map<String, Object> userInfo =
                    (Map<String, Object>) res.get("response");

            String id = String.valueOf(userInfo.get("id"));

            String nickname = (String) userInfo.get("nickname");
            if (nickname == null || nickname.isBlank()) {
                nickname = "네이버회원";
            }

            String image = (String) userInfo.get("profile_image");

            return new NaverUserInfo(id, nickname, image);

        } catch (RestClientException e) {
            log.warn("네이버 사용자 정보 요청 실패: {}", e.getMessage());
            throw naverFailed();
        }
    }

    private CustomException naverFailed() {
        return new CustomException(
                HttpStatus.UNAUTHORIZED,
                "NAVER_LOGIN_FAILED",
                "네이버 로그인에 실패했습니다."
        );
    }
}
