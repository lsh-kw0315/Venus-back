package com.ll.server.domain.like.repository;

import com.ll.server.domain.like.entity.Like;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.repost.entity.Repost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    @Query("""
    select l from Like l
    join fetch l.repost
    join fetch l.member
    where l.deleted is false and l.repost = :repost
    """)
    List<Like> findLikesByRepostAndDeletedIsFalse(Repost repost);

    Optional<Like> findLikeByRepostAndMemberAndDeletedIsFalse(Repost repost, Member member);

    @Modifying(clearAutomatically = true)
    @Query("""
update Like l
set l.deleted = true, l.modifyDate = now()
where l.repost = :repost
""")
    void deleteRepostLike(Repost repost);

    @Query("""
    select l from Like l
    join fetch l.repost
    join fetch l.member
    where l.deleted is false and l.repost in :reposts
    """)
    List<Like> getLikesOfRelatedReposts(List<Repost> reposts);
}
