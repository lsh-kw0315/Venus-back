package com.ll.server.domain.elasticsearch.repost.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.ll.server.domain.elasticsearch.repost.doc.RepostDoc;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.mention.repostmention.repository.RepostMentionRepository;
import com.ll.server.domain.repost.dto.RepostOnly;
import com.ll.server.domain.repost.entity.Repost;
import com.ll.server.domain.repost.repository.RepostRepository;
import com.ll.server.global.config.ElasticSearchClientConfig;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepostDocService {
    private final RepostRepository repostRepository;
    private final RepostMentionRepository repostMentionRepository;

    @SneakyThrows
    @Transactional(readOnly = true)
    public Page<RepostOnly> searchContent(String keyword, Pageable pageable) {
        ElasticsearchClient client = new ElasticSearchClientConfig().createElasticsearchClient();

        SearchRequest searchRequest = SearchRequest.of(
                s -> s.index("repost")
                        .size(pageable.getPageSize())
                        .from((int) pageable.getOffset())
                        .query(q ->
                                q.bool(
                                        b -> b.mustNot(mn -> mn.exists(e -> e.field("deleted_at")))
                                                .should(should -> should.match(m -> m.field("content").query(keyword)))

                                )
                        )
                        .sort
                                (SortOptions.of(sort1 -> sort1.field(f1 -> f1.field("create_date").order(SortOrder.Desc))),
                                        SortOptions.of(sort2 -> sort2.field(f2 -> f2.field("id").order(SortOrder.Desc)))
                                )
        );

        SearchResponse<RepostDoc> response = client.search(searchRequest, RepostDoc.class);

        List<Long> ids = response.hits().hits().stream().map(hit -> Objects.requireNonNull(hit.source()).getId()).toList();
        long totalElements = Objects.requireNonNull(response.hits().total()).value();

        List<Repost> realResult = repostRepository.findAllByIdInAndDeletedAtIsNullOrderByCreateDateDescIdDesc(ids);
        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfRelatedRepost(realResult);
        Map<Long, List<RepostMention>> map = repostMentions.stream()
                .collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));

        return new PageImpl<>(
                realResult.stream()
                        .map(repost -> new RepostOnly(repost,map.get(repost.getId())))
                        .collect(Collectors.toList()),
                pageable,
                totalElements
        );

    }

    @Transactional(readOnly = true)
    @SneakyThrows
    public List<RepostOnly> firstInfinitySearch(int size, String keyword) throws IOException {
        ElasticsearchClient client = new ElasticSearchClientConfig().createElasticsearchClient();

        SearchRequest searchRequest = SearchRequest.of(
                s -> s.index("repost")
                        .size(size)
                        .query(q ->
                                q.bool(
                                        b -> b.mustNot(mn -> mn.exists(e -> e.field("deleted_at")))
                                                .should(should -> should.match(m -> m.field("content").query(keyword)))

                                )
                        )
                        .sort
                                (SortOptions.of(sort1 -> sort1.field(f1 -> f1.field("create_date").order(SortOrder.Desc))),
                                        SortOptions.of(sort2 -> sort2.field(f2 -> f2.field("id").order(SortOrder.Desc)))
                                )
        );

        return getRepostOnlyList(client, searchRequest);
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public List<RepostOnly> afterInfinitySearch(int size, String keyword, LocalDateTime lastTime, Long lastId) {
        ElasticsearchClient client = new ElasticSearchClientConfig().createElasticsearchClient();

        ZonedDateTime zonedDateTime = lastTime.atZone(ZoneId.of("UTC"));
        // 원하는 형식으로 변환
        String formattedDate = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        SearchRequest searchRequest = SearchRequest.of(
                s -> s.index("repost")
                        .size(size)
                        .query(q ->
                                q.bool(
                                        b -> b.mustNot(mn -> mn.exists(e -> e.field("deleted_at")))
                                                .should(should -> should.match(m -> m.field("content").query(keyword)))
                                )
                        )
                        .sort
                                (SortOptions.of(sort1 -> sort1.field(f1 -> f1.field("create_date").order(SortOrder.Desc))),
                                        SortOptions.of(sort2 -> sort2.field(f2 -> f2.field("id").order(SortOrder.Desc)))
                                )
                        .searchAfter(FieldValue.of(formattedDate), FieldValue.of(lastId))
        );

        return getRepostOnlyList(client, searchRequest);
    }

    @NotNull
    private List<RepostOnly> getRepostOnlyList(ElasticsearchClient client, SearchRequest searchRequest) throws IOException {
        SearchResponse<RepostDoc> response = client.search(searchRequest, RepostDoc.class);

        List<Long> ids = response.hits().hits().stream().map(hit -> Objects.requireNonNull(hit.source()).getId()).toList();

        List<Repost> realResult = repostRepository.findAllByIdInAndDeletedAtIsNullOrderByCreateDateDescIdDesc(ids);
        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfRelatedRepost(realResult);
        Map<Long, List<RepostMention>> map = repostMentions.stream()
                .collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));

        return realResult
                .stream()
                .map(repost -> new RepostOnly(repost, map.get(repost.getId())))
                .collect(Collectors.toList());
    }

}
