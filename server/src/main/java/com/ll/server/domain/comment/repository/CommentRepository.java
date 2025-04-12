package com.ll.server.domain.comment.repository;

import com.ll.server.domain.comment.entity.Comment;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.repost.entity.Repost;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findCommentsByMemberAndDeletedAtIsNull(Member user);

    @Query("""
            select distinct c from Comment c
            join fetch c.repost
            join fetch c.repost.member
            join fetch c.member
            where c.repost = :repost
            """)
    Page<Comment> getCommentPage(Repost repost, Pageable pageable);


    @Query("""
    select distinct c from Comment c
    join fetch c.repost
    join fetch c.repost.member
    join fetch c.member
    where c.deletedAt is null and c.repost in :reposts
    """)
    List<Comment> getAllRepostComment(List<Repost> reposts);

    @Query("""
    select distinct c from Comment c
    join fetch c.repost
    join fetch c.repost.member
    join fetch c.member
    where c.repost = :repost
    """)
    List<Comment> getOneReposComment(Repost repost);

    @Query("""
    select count(c) from Comment c
    where c.repost = :repost
    """)
    long getOneRepostCommentCount(Repost repost);


    @Query("select count(c) from Comment c where c.repost = :repost")
    long getCommentTotal(Repost repost);

    @Query("select c from Comment c join fetch c.member where c.repost = :repost and c.id = :commentId")
    Comment findCommentByIdAndRepostAndDeletedAtIsNull(Long commentId,Repost repost);

    List<Comment> findCommentsByRepostAndDeletedAtIsNull(Repost repost);

    @Query("""
    select c from Comment c
    join fetch c.repost
    join fetch c.member
    join fetch c.repost.member
    where c.deletedAt is null and c.repost = :repost
    """)
    List<Comment> findCommentsByRepostAndDeletedAtIsNullOrderByCreateDateAscIdAsc(Repost repost, Limit limit);

    @Query("""
    select c from Comment c
    join fetch c.repost
    join fetch c.member
    join fetch c.repost.member
    where c.deletedAt is null and c.repost = :repost and c.id > :lastCommentId and c.createDate > :lastTime
    """)
    List<Comment> findCommentsByRepostAndIdGreaterThanAndCreateDateAfterAndDeletedAtIsNullOrderByCreateDateAscIdAsc(Repost repost, Long lastCommentId, LocalDateTime lastTime, Limit limit);

    @Modifying(clearAutomatically = true)
    @Query("""
    update Comment c
    set c.deletedAt = now(), c.modifyDate = now()
    where c.repost = :repost
    """)
    void deleteCommentsByRepost(Repost repost);
}
