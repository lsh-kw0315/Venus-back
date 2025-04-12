package com.ll.server.domain.mention.repostmention.repository;

import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.repost.entity.Repost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RepostMentionRepository extends JpaRepository<RepostMention, Long> {

    @Query("""
select rm from RepostMention rm
join fetch rm.member
join fetch rm.repost
where rm.repost = :repost
""")
    List<RepostMention> getMentionsOfOneRepost(Repost repost);

    @Query("""
select rm from RepostMention rm
join fetch rm.member
join fetch rm.repost
where rm.repost in :reposts
""")
    List<RepostMention> getMentionsOfRelatedRepost(List<Repost> reposts);
}
