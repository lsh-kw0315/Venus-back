package com.ll.server.domain.repost.repository;

import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.repost.entity.Repost;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepostRepository extends JpaRepository<Repost, Long> {

    //멤버의 닉네임을 기반으로 repost 검색
    List<Repost> findRepostsByMember_NicknameAndDeletedAtIsNull(String nickname);

    //멤버의 ID를 기반으로 repost 검색
    @Query("""
    select r from Repost r
    join fetch r.member
    where r.member = :member and r.deletedAt is null
    """)
    Page<Repost> findRepostsByMemberAndDeletedAtIsNull(Member member, Pageable pageable);

    @Query("""
    select r from Repost r
    join fetch r.member
    join fetch r.news
    where r.deletedAt is null
    """)
    Page<Repost> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("select r from Repost r join fetch r.member join fetch r.news where r.deletedAt is null and r.id = :repostId")
    Optional<Repost> findByIdAndDeletedAtIsNull(Long repostId);

    @Query("""
            SELECT distinct r FROM Repost r
            join fetch r.member
            WHERE r.news.id = :newsId AND r.news.deletedAt IS NULL AND r.deletedAt IS NULL
            ORDER BY
            r.pinned DESC,
            r.createDate DESC,
            r.id DESC
            """)
    List<Repost> firstGetNewsReposts(@Param("newsId") Long newsId,
                                     Limit limit);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            WHERE r.news.id = :newsId AND r.createDate < :lastTime AND r.id < :lastId AND r.pinned = false AND r.news.deletedAt IS NULL AND r.deletedAt IS NULL
            ORDER BY
            r.createDate DESC,
            r.id DESC
            """)
        //첫 페이지에만 고정된 것들이 있다고 가정한다.
    List<Repost> afterGetNewsReposts(@Param("newsId") Long newsId,
                                     @Param("lastTime") LocalDateTime lastTime,
                                     @Param("lastId") Long lastId,
                                     Limit limit);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            WHERE r.news.id = :newsId AND r.news.deletedAt IS NULL AND r.deletedAt IS NULL
            ORDER BY
            r.pinned DESC ,
            r.createDate DESC,
            r.id DESC
            """)
    Page<Repost> getNewsReposts(@Param("newsId") Long newsId, Pageable pageable);


    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            where r.deletedAt is null
            ORDER BY
            r.createDate DESC,
            r.id DESC
    """)
    List<Repost> findAllByDeletedAtIsNullOrderByCreateDateDescIdDesc(Limit limit);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            where r.createDate < :lastTime and r.id < :lastId and r.deletedAt is null
            ORDER BY
            r.createDate DESC,
            r.id DESC
    """)
    List<Repost> findAllByDeletedAtIsNullAndCreateDateBeforeAndIdLessThanOrderByCreateDateDescIdDesc(LocalDateTime lastTime, Long lastId, Limit limit);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            where r.deletedAt is null and r.id in :ids
            ORDER BY
            r.createDate DESC,
            r.id DESC
    """)
    List<Repost> findAllByIdInAndDeletedAtIsNullOrderByCreateDateDescIdDesc(List<Long> ids);

    Repost findRepostByNewsIdAndPinnedIsTrueAndDeletedAtIsNull(Long newsId);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            where r.content like concat('%',:keyword,'%') and r.deletedAt is null
            ORDER BY
            r.createDate DESC,
            r.id DESC
    """)
    Page<Repost> findByContentContainingAndDeletedAtIsNull(String keyword,Pageable pageable);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            where r.content like concat('%',:keyword,'%') and r.deletedAt is null and r.createDate < :lastTime and r.id < :lastId
            ORDER BY
            r.createDate DESC,
            r.id DESC
    """)
    List<Repost> searchContentCursorAfter(String keyword,LocalDateTime lastTime, Long lastId,Limit limit);

    @Query("""
            SELECT r FROM Repost r
            join fetch r.member
            where r.content like concat('%',:keyword,'%') and r.deletedAt is null
            ORDER BY
            r.createDate DESC,
            r.id DESC
    """)
    List<Repost> searchContentCursorFirst(String keyword, Limit limit);

    @Query("SELECT r FROM Repost r join fetch r.member INNER JOIN r.likes l WHERE r.createDate >= :startOfDay GROUP BY r.id ORDER BY COUNT(l) DESC, r.id DESC")
    List<Repost> findTodayshotReposts(@Param("startOfDay") LocalDateTime startOfDay, Pageable pageable);

    //좋아요 누른 것만 가지고 온다.
    @Query("""
    select r from Repost r
    join fetch r.member
    join Like l on l.repost = r and l.member.id = :memberId and r.deletedAt is null and l.deleted is false
    ORDER BY r.id DESC
    """)
    Page<Repost> findLikeRepost(Long memberId, Pageable pageable);

    @Query("""
    select count(r) from Repost r join Like l on l.repost = r and r.deletedAt is null and l.deleted is false and l.member.id = :memberId
""")
    long countLikeRepost(Long memberId);


}
