package com.ll.server.domain.elasticsearch.news.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.ll.server.domain.elasticsearch.news.doc.NewsDoc;
import com.ll.server.domain.news.news.dto.NewsOnly;
import com.ll.server.domain.news.news.repository.NewsRepository;
import com.ll.server.global.config.ElasticSearchClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NewsDocService {
    private final NewsRepository newsRepository;

    @SneakyThrows
    public Page<NewsOnly> search(String keyword, boolean hasTitle, boolean hasContent, boolean hasPublisher, String category, Pageable page) {
        ElasticsearchClient client = new ElasticSearchClientConfig().createElasticsearchClient();

        BoolQuery boolQuery = createBoolQuery(keyword, hasTitle, hasContent, hasPublisher, category);
        SearchResponse<NewsDoc> result = client.search(
                SearchRequest.of(
                        s -> s.index("news")
                                .from((int) page.getOffset())
                                .size(page.getPageSize())
                                .sort(SortOptions.of(so1 -> so1.field(f -> f.field("published_at").order(SortOrder.Desc))),
                                        SortOptions.of(so2 -> so2.field(f -> f.field("id").order(SortOrder.Desc)))
                                )
                                .query(
                                        q -> q.bool(boolQuery)
                                )
                ), NewsDoc.class
        );

        long totalHits = Objects.requireNonNull(result.hits().total()).value();

        List<NewsOnly> newsOnlyList = result.hits().hits().stream()
                .map(hit -> new NewsOnly(Objects.requireNonNull(hit.source())))
                .toList();

        //NewsDTO 빌더로 하나하나 다 넣음
        return new PageImpl<>(
                newsOnlyList,
                page,
                totalHits
        );

    }

    @SneakyThrows
    public List<NewsOnly> firstInfinitySearch(String keyword, boolean hasTitle, boolean hasContent, boolean hasPublisher, String category, int size) {
        ElasticsearchClient client = new ElasticSearchClientConfig().createElasticsearchClient();

        BoolQuery boolQuery = createBoolQuery(keyword, hasTitle, hasContent, hasPublisher, category);

        SearchResponse<NewsDoc> result = client.search(
                SearchRequest.of(
                        s -> s.index("news")
                                .size(size)
                                .sort(SortOptions.of(so1 -> so1.field(f -> f.field("published_at").order(SortOrder.Desc)))
                                        , SortOptions.of(so2 -> so2.field(f -> f.field("id").order(SortOrder.Desc)))
                                )
                                .query(
                                        q -> q.bool(boolQuery)
                                )
                ), NewsDoc.class
        );


        return result.hits().hits().stream()
                .map(hit -> new NewsOnly(Objects.requireNonNull(hit.source())))
                .collect(Collectors.toList());
    }

    private BoolQuery createBoolQuery(String keyword, boolean hasTitle, boolean hasContent, boolean hasPublisher, String category) {
        BoolQuery.Builder bqBuilder = new BoolQuery.Builder();

        String realCategory = category.toUpperCase();

        bqBuilder.mustNot(mn -> mn.exists(ex -> ex.field("deleted_at")));

        boolean hasShould = hasTitle || hasContent || hasPublisher;

        if (hasTitle) {
            bqBuilder.should(s -> s.match(m -> m.field("title").query(keyword)));
        }

        if (hasContent) {
            bqBuilder.should(s -> s.match(m -> m.field("content").query(keyword)));
        }

        if (hasPublisher) {
            bqBuilder.should(s -> s.match(m -> m.field("publisher").query(keyword)));
        }

        if (!category.isBlank()) {
            bqBuilder.filter(f -> f.term(t -> t.field("category").value(realCategory)));
        }

        if (hasShould) {
            bqBuilder.minimumShouldMatch("1");
        }
        BoolQuery result = bqBuilder.build();
        log.info(result.toString());

        return result;
    }

    @SneakyThrows
    public List<NewsOnly> afterInfinitySearch(String keyword, boolean hasTitle, boolean hasContent, boolean hasPublisher, String category, int size, LocalDateTime lastTime, Long lastId) {
        ElasticsearchClient client = new ElasticSearchClientConfig().createElasticsearchClient();
        BoolQuery boolQuery = createBoolQuery(keyword, hasTitle, hasContent, hasPublisher, category);

        ZonedDateTime zonedDateTime = lastTime.atZone(ZoneId.of("UTC"));

        // 원하는 형식으로 변환
        String formattedDate = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        SearchRequest sq =
                SearchRequest.of(
                        s -> s.index("news")
                                .size(size)
                                .sort(SortOptions.of(so1 -> so1.field(f -> f.field("published_at").order(SortOrder.Desc)))
                                        , SortOptions.of(so2 -> so2.field(f -> f.field("id").order(SortOrder.Desc)))
                                )
                                .query(
                                        q -> q.bool(boolQuery)
                                )
                                .searchAfter(FieldValue.of(formattedDate), FieldValue.of(lastId))
                );

        log.info(sq.toString());
        SearchResponse<NewsDoc> result = client.search(
                sq, NewsDoc.class
        );


        return result.hits().hits().stream()
                .map(hit -> new NewsOnly(Objects.requireNonNull(hit.source())))
                .collect(Collectors.toList());
    }

}
