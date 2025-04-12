package com.ll.server.domain.member.repository;

import com.ll.server.domain.member.entity.ConnectedProvider;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.member.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectedProviderRepository extends JpaRepository<ConnectedProvider, Long> {

    List<ConnectedProvider> findConnectedProvidersByMember(Member member);

    Optional<ConnectedProvider> findConnectedProviderByMemberAndProvider(Member member, Provider provider);
}
