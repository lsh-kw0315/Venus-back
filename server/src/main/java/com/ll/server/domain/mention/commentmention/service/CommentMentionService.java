package com.ll.server.domain.mention.commentmention.service;


import com.ll.server.domain.comment.entity.Comment;
import com.ll.server.domain.comment.repository.CommentRepository;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.mention.commentmention.entity.CommentMention;
import com.ll.server.domain.mention.commentmention.repository.CommentMentionRepository;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentMentionService {
    CommentMentionRepository commentMentionRepository;
    CommentRepository commentRepository;

    @Transactional
    public CommentMention save(Comment comment, Member member) {
        CommentMention mention =
                CommentMention.builder()
                        .comment(comment)
                        .member(member)
                        .build();

        return commentMentionRepository.save(mention);
    }

    public List<CommentMention> findByComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));
        return commentMentionRepository.findCommentMentionsByComment(comment);
    }
}
