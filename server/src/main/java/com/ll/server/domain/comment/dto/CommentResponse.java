package com.ll.server.domain.comment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CommentResponse {
    private List<CommentDTO> comments;
    private long count;

    public CommentResponse(List<CommentDTO> comments) {
        if(comments==null || comments.isEmpty()){
            this.count = 0;
            this.comments =null;
        }else {
            this.comments = comments;
            count = comments.size();
        }
    }

    public CommentResponse(List<CommentDTO> comments, long count){
        this.comments = comments;
        this.count = count;
    }
}
