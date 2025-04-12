package com.ll.server.domain.member.repository;

import com.ll.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    List<Member> findAllByNicknameIn(List<String> mentionedNames);

    Optional<Member> findMemberByEmail(String email);

    @Query("""
select m from Member m
join fetch ConnectedProvider cp on m.id = cp.member.id and cp.providerId = :providerId
""")
    Optional<Member> findMemberByProviderId(String providerId);
}
