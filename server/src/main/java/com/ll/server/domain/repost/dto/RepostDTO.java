package com.ll.server.domain.repost.dto;

import com.ll.server.domain.comment.dto.CommentDTO;
import com.ll.server.domain.comment.dto.CommentResponse;
import com.ll.server.domain.like.dto.LikeDTO;
import com.ll.server.domain.like.dto.LikeResponse;
import com.ll.server.domain.mention.repostmention.dto.RepostMentionDTO;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.news.news.dto.NewsOnly;
import com.ll.server.domain.news.news.entity.News;
import com.ll.server.domain.repost.entity.Repost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Getter
public class RepostDTO {
    private Long repostId;
    private Long writerId;
    private String nickname;
    private NewsOnly news;
    private String content;
    private List<RepostMentionDTO> mentions;
    private CommentResponse commentInfo;
    private LikeResponse likeInfo;
    private String imageUrl;
    private String memberProfileImageUrl;
    private LocalDateTime createDate;

    public RepostDTO(Repost repost, List<RepostMention> mentions, CommentResponse comments, LikeResponse like){
        this.news = new NewsOnly(repost.getNews());
        repostId = repost.getId();
        writerId = repost.getMember().getId();
        nickname = repost.getMember().getNickname();
        content = repost.getContent();
        imageUrl = repost.getImageUrl();
        memberProfileImageUrl = repost.getMember().getProfileUrl();
        createDate = repost.getCreateDate();

        this.mentions = mentions.stream().map(RepostMentionDTO::new).collect(Collectors.toList());

        this.commentInfo = comments;
        this.likeInfo = like;
    }

    public RepostDTO(Repost repost) {
        News newsEntity = repost.getNews();
        news = NewsOnly.builder()
                .publisherName(newsEntity.getPublisher())
                .author(newsEntity.getAuthor())
                .id(newsEntity.getId())
                .title(newsEntity.getTitle())
                .content(newsEntity.getContent())
                .contentUrl(newsEntity.getContentUrl())
                .imageUrl(newsEntity.getImageUrl())
                .thumbnailUrl(newsEntity.getThumbnailUrl())
                .build();
        repostId = repost.getId();
        writerId = repost.getMember().getId();
        nickname = repost.getMember().getNickname();
        content = repost.getContent();
        mentions = repost.getMentions().stream()
                .map(RepostMentionDTO::new)
                .collect(Collectors.toList());

        commentInfo = new CommentResponse(
                repost.getComments()
                        .stream().filter(comment -> comment.getDeletedAt() == null)
                        .map(CommentDTO::new)
                        .collect(Collectors.toList())
        );

        likeInfo = new LikeResponse(
                repost.getLikes()
                        .stream().filter(comment -> !comment.getDeleted())
                        .map(LikeDTO::new)
                        .collect(Collectors.toList())
        );

        imageUrl = repost.getImageUrl();
        memberProfileImageUrl = repost.getMember().getProfileUrl();
        createDate = repost.getCreateDate();
    }
}
