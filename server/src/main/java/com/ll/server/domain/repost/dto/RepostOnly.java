package com.ll.server.domain.repost.dto;

import com.ll.server.domain.mention.repostmention.dto.RepostMentionDTO;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
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
public class RepostOnly {
    private Long repostId;
    private Long writerId;
    private String nickname;
    private String content;
    private int commentCount;
    private int likeCount;
    private String imageUrl;
    private LocalDateTime createDate;
    private String memberProfileImageUrl;
    private List<RepostMentionDTO> repostMentions;

    public RepostOnly(Repost repost) {
        repostId = repost.getId();
        writerId = repost.getMember().getId();
        nickname = repost.getMember().getNickname();
        content = repost.getContent();

        commentCount = repost.getComments().size();

        likeCount = repost.getLikes().size();

        imageUrl = repost.getImageUrl();
        createDate = repost.getCreateDate();

        memberProfileImageUrl = repost.getMember().getProfileUrl();

        repostMentions = repost.getMentions().stream().map(RepostMentionDTO::new).collect(Collectors.toList());

    }

    public RepostOnly(Repost repost, List<RepostMention> mentions){
        repostId = repost.getId();
        writerId = repost.getMember().getId();
        nickname = repost.getMember().getNickname();
        content = repost.getContent();

        commentCount = repost.getComments().size();

        likeCount = repost.getLikes().size();

        imageUrl = repost.getImageUrl();
        createDate = repost.getCreateDate();

        memberProfileImageUrl = repost.getMember().getProfileUrl();

        repostMentions = mentions.stream()
                .map(RepostMentionDTO::new).collect(Collectors.toList());
    }
}
