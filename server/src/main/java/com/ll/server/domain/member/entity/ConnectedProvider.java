package com.ll.server.domain.member.entity;

import com.ll.server.domain.member.enums.Provider;
import com.ll.server.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ConnectedProvider extends BaseEntity {

    @Column(nullable = false)
    private String providerId;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
}
