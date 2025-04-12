package com.ll.server.domain.mention.repostmention.service;

import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.mention.repostmention.repository.RepostMentionRepository;
import com.ll.server.domain.repost.entity.Repost;
import com.ll.server.domain.repost.repository.RepostRepository;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepostMentionService {
    private final RepostMentionRepository repostMentionRepository;
    private final RepostRepository repostRepository;

    @Transactional
    public RepostMention save(Repost repost, Member member) {
        RepostMention mention =
                RepostMention.builder()
                        .repost(repost)
                        .member(member)
                        .build();

        return repostMentionRepository.save(mention);
    }

    public List<RepostMention> findByRepost(Long repostId) {
        Repost repost = repostRepository.findByIdAndDeletedAtIsNull(repostId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));
        return repostMentionRepository.getMentionsOfOneRepost(repost);
    }
}
