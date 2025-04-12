package com.ll.server.domain.member.entity;

import com.ll.server.domain.member.dto.MemberUpdateParam;
import com.ll.server.domain.member.enums.MemberRole;
import com.ll.server.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "members")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Member extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email; //이게 id 역할
    @Column(nullable = false)
    private String password;
    @Column(unique = true, nullable = false)
    private String nickname;
    private String profileUrl;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    public void update(MemberUpdateParam param) {
        if (param.getNickname() != null && !param.getNickname().isBlank()) {
            this.nickname = param.getNickname();
        }

        this.profileUrl = param.getProfileUrl();
    }
}

