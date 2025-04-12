package com.ll.server.domain.repost.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ll.server.domain.comment.entity.Comment;
import com.ll.server.domain.like.entity.Like;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.news.news.entity.News;
import com.ll.server.global.jpa.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Repost extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private News news;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private LocalDateTime deletedAt = null;

    @OneToMany(mappedBy = "repost", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    @JsonIgnore
    private List<RepostMention> mentions = new ArrayList<>();

    @OneToMany(mappedBy = "repost", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    @JsonIgnore
    @BatchSize(size = 100)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "repost", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    @JsonIgnore
    @BatchSize(size = 100)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    private Boolean pinned = false;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;


    public Comment addComment(Member member, List<Member> mentionedMembers, String content) {
        Comment comment = Comment.builder()
                .repost(this)
                .content(content)
                .member(member)
                .build();

        for (Member mentionedMember : mentionedMembers) {
            comment.addMention(mentionedMember);
        }

        comments.add(comment);

        return comment;
    }

    public Like addLike(Member member) {
        for(Like existing : likes){
            if(existing.getMember().getId().equals(member.getId())) {
                existing.setDeleted(false);
                return existing;
            }
        }

        Like like = Like.builder()
                .repost(this)
                .deleted(false)
                .member(member)
                .build();

        likes.add(like);

        return like;

    }

    public void addMention(Member member) {
        RepostMention mention = RepostMention.builder()
                .repost(this)
                .member(member)
                .build();

        this.mentions.add(mention);
    }

    public void deleteComments() {
        comments.forEach(comment ->
                {
                    if (comment.getDeletedAt() == null) {
                        comment.setDeletedAt(LocalDateTime.now());
                        comment.setModifyDate(LocalDateTime.now());
                    }
                }
        );
    }

    public void deleteLikes() {
        likes.forEach(like ->
                {
                    like.setDeleted(true);
                    like.setModifyDate(LocalDateTime.now());
                }
        );
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        setModifyDate(LocalDateTime.now());
        this.pinned = false;
        deleteLikes();
        deleteComments();
    }
}
