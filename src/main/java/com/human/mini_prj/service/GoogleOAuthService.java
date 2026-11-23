
package com.human.mini_prj.service;

import com.human.mini_prj.dto.GoogleUserInfo;
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

// 구글 서버와 통신하는 부분만 담당
@Slf4j
@Service
public class GoogleOAuthService {

    private static final String TOKEN_URL =
            "https://oauth2.googleapis.com/token";
    private static final String USER_URL =
            "https://www.googleapis.com/oauth2/v2/userinfo";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthService(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret}") String clientSecret,
            @Value("${google.redirect-uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    // ① 인가 코드 → 구글 Access Token
    public String getAccessToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        try {
            Map<String, Object> res = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_TYPE);

            return (String) res.get("access_token");

        } catch (RestClientException e) {
            log.warn("구글 토큰 요청 실패: {}", e.getMessage());
            throw googleFailed();
        }
    }

    // ② 구글 Access Token → 사용자 정보
    public GoogleUserInfo getUserInfo(String googleAccessToken) {
        try {
            Map<String, Object> res = restClient.get()
                    .uri(USER_URL)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + googleAccessToken
                    )
                    .retrieve()
                    .body(MAP_TYPE);

            String id = (String) res.get("id");
            String email = (String) res.get("email");
            String name = (String) res.get("name");
            String picture = (String) res.get("picture");

            return new GoogleUserInfo(id, email, name, picture);

        } catch (RestClientException e) {
            log.warn("구글 사용자 정보 요청 실패: {}", e.getMessage());
            throw googleFailed();
        }
    }

    private CustomException googleFailed() {
        return new CustomException(
                HttpStatus.UNAUTHORIZED,
                "GOOGLE_LOGIN_FAILED",
                "구글 로그인에 실패했습니다."
        );
    }
}
