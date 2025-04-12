package com.ll.server.domain.comment.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CommentInfinityScrollResponse {
    List<CommentDTO> comments;
    long lastId;
    LocalDateTime lastTime;

    public CommentInfinityScrollResponse(List<CommentDTO> comments) {
        if (comments == null || comments.isEmpty()) {
            this.comments = null;
            lastId = -1;
            lastTime = null;
        } else {
            this.comments = comments;
            lastId = comments.getLast().getCommentId();
            lastTime = comments.getLast().getCreateDate();
        }

    }
}
