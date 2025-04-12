package com.ll.server.domain.comment.dto;

import com.ll.server.domain.comment.entity.Comment;
import com.ll.server.domain.mention.commentmention.dto.CommentMentionDTO;
import com.ll.server.domain.mention.commentmention.entity.CommentMention;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Getter
public class CommentDTO {
    private Long commentId;

    private Long repostId;
    private Long repostWriterId;
    private String repostWriterName;

    private List<CommentMentionDTO> mentions;
    private String content;
    private Long commentWriterId;
    private String commentWriterName;
    private String commentWriterProfileImageUrl;

    private LocalDateTime createDate;

    public CommentDTO(Comment comment) {
        commentId = comment.getId();

        repostId = comment.getRepost().getId();
        repostWriterId = comment.getRepost().getMember().getId();
        repostWriterName = comment.getRepost().getMember().getNickname();

        mentions = comment.getMentions().stream()
                .map(CommentMentionDTO::new)
                .collect(Collectors.toList());

        content = comment.getContent();

        commentWriterId = comment.getMember().getId();
        commentWriterName = comment.getMember().getNickname();
        commentWriterProfileImageUrl = comment.getMember().getProfileUrl();
        createDate = comment.getCreateDate();
    }

    public CommentDTO(Comment comment, List<CommentMention> mentions){
        commentId = comment.getId();

        repostId = comment.getRepost().getId();
        repostWriterId = comment.getRepost().getMember().getId();
        repostWriterName = comment.getRepost().getMember().getNickname();

        this.mentions = mentions.stream().map(CommentMentionDTO::new).collect(Collectors.toList());

        content = comment.getContent();

        commentWriterId = comment.getMember().getId();
        commentWriterName = comment.getMember().getNickname();
        commentWriterProfileImageUrl = comment.getMember().getProfileUrl();
        createDate = comment.getCreateDate();
    }
}
