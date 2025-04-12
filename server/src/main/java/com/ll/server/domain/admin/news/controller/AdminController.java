package com.ll.server.domain.admin.news.controller;

import com.ll.server.domain.news.news.dto.NewsDTO;
import com.ll.server.domain.news.news.dto.NewsOnly;
import com.ll.server.domain.news.news.dto.NewsUpdateRequest;
import com.ll.server.domain.news.news.entity.News;
import com.ll.server.domain.news.news.service.NewsService;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.response.ApiResponse;
import com.ll.server.global.response.response.CustomPage;
import com.ll.server.global.validation.PageLimitSizeValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final NewsService newsService;

    @Data
    private static class NewsGetRequest {
        //requestBody가 들어오지 않을 때 default page, limit 입니다
        private int page = 0;
        private int limit = 20;
    }

    @GetMapping("/news")
    public ApiResponse<?> newsGetAll(NewsGetRequest request) {
        PageLimitSizeValidator.validateSize(request.getPage(), request.getLimit(), 50);
        Pageable pageable = PageRequest.of(request.getPage(), request.getLimit());
        //Page<NewsDTO> news = newsService.getAll(pageable).map(newsService::convertToDTO);
        Page<NewsOnly> news = newsService.getAll(pageable);
        return ApiResponse.of(CustomPage.of(news));
    }

    @GetMapping("/news/{id}")
    public ApiResponse<NewsDTO> newsGetById(@PathVariable Long id) {
//        NewsDTO newsDTO = newsService.getById(id);
        News news = newsService.getNews(id);

        NewsDTO newsDTO = new NewsDTO(news);

        return ApiResponse.of(newsDTO);
    }

    @PatchMapping("/news/{id}")
    public ApiResponse<NewsOnly> newsUpdate(@PathVariable Long id, @RequestBody NewsUpdateRequest request) {
        NewsOnly newsDTO = newsService.updateNews(id, request);
        //NewsDTO newsDTO = newsService.convertToDTO(news);

        return ApiResponse.of(newsDTO);
    }

    @DeleteMapping("/news/{id}")
    public ApiResponse<String> newsDelete(@PathVariable Long id) {

        return ApiResponse.of(ReturnCode.SUCCESS_ADMIN);
    }
}
