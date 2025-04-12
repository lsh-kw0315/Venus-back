package com.ll.server.domain.member.auth.interfaces.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SignupRequestDto {
    private String email;
    private String password;
    private String nickname;
}
