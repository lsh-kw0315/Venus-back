package com.ll.server.domain.saved.repository;

import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.news.news.entity.News;
import com.ll.server.domain.saved.entity.Saved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SavedRepository extends JpaRepository<Saved,Long> {

    @Query("select s from Saved s join fetch s.news join fetch s.member where s.deleted is false and s.news = :news")
    List<Saved> getRelatedSaved(News news);

    @Query("select s from Saved s join fetch s.news join fetch s.member where s.deleted is false and s.news = :news and s.member = :member")
    Optional<Saved> getOneMemberSavedNews(News news, Member member);
}
