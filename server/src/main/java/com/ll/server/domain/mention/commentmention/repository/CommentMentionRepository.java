package com.ll.server.domain.mention.commentmention.repository;

import com.ll.server.domain.comment.entity.Comment;
import com.ll.server.domain.mention.commentmention.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {

    @Query(
    """
    select distinct cm from CommentMention cm
    join fetch cm.member
    join fetch cm.comment
    where cm.comment in :comments
    """)
    List<CommentMention> getRelatedMentions(List<Comment> comments);

    @Query(
    """
    select distinct cm from CommentMention cm
    join fetch cm.member
    join fetch cm.comment
    where cm.comment = :comment
    """)
    List<CommentMention> findCommentMentionsByComment(Comment comment);
}
