package com.ll.server.domain.comment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.mention.commentmention.entity.CommentMention;
import com.ll.server.domain.repost.entity.Repost;
import com.ll.server.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Setter
public class Comment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Repost repost;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private LocalDateTime deletedAt = null;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    @ToString.Exclude
    @Builder.Default
    @JsonIgnore
    List<CommentMention> mentions = new ArrayList<>();

    public void addMention(Member member) {
        CommentMention mention = CommentMention
                .builder()
                .comment(this)
                .member(member)
                .build();

        mentions.add(mention);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        setModifyDate(LocalDateTime.now());
    }
}
