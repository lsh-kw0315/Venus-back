package com.ll.server.domain.member.auth.interfaces.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Getter
@NoArgsConstructor
//@RedisHash(value = "refreshToken")
public class RefreshToken {
    @Id
    private String token;
    private String email;
    //@TimeToLive
    private Long expiration;

    @Builder
    public RefreshToken(String token, String email, Long expiration) {
        this.token = token;
        this.email = email;
        this.expiration = expiration;
    }
}
