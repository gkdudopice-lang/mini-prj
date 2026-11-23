
package com.human.mini_prj.dto;

// 구글에서 받아온 사용자 정보
public record GoogleUserInfo(
        String id,
        String email,
        String name,
        String picture
) {
}
