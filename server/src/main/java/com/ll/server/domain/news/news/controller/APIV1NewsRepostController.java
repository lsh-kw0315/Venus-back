package com.ll.server.domain.news.news.controller;

import com.ll.server.domain.repost.dto.NewsRepostInfinityResponse;
import com.ll.server.domain.repost.dto.RepostUnderNews;
import com.ll.server.domain.repost.service.RepostService;
import com.ll.server.global.response.response.ApiResponse;
import com.ll.server.global.response.response.CustomPage;
import com.ll.server.global.utils.MyConstant;
import com.ll.server.global.validation.PageLimitSizeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news/{newsId}/reposts")
//뉴스 상세 조회에서 repost 목록을 열람 시 페이지네이션에 사용되는 엔드포인트
public class APIV1NewsRepostController {

    private final RepostService repostService;

    //뉴스의 리포스트 무한 스크롤 요청 시 (전통적인 페이지네이션으로 반환하는) 엔드포인트
    @GetMapping()
    public ApiResponse<?> getUnderReposts(@PathVariable("newsId") Long newsId,
                                          @RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size) {
        PageLimitSizeValidator.validateSize(page, size, MyConstant.PAGELIMITATION);
        Pageable pageable = PageRequest.of(page, size);

        CustomPage<RepostUnderNews> repostPage = CustomPage.of(repostService.getNewsRepostCursorPagination(newsId, pageable));

        return ApiResponse.of(repostPage);
    }


    @GetMapping("/infinityTest")
    public ApiResponse<?> getUnderRepostsInfinity(@PathVariable("newsId") Long newsId,
                                                  @RequestParam(value = "size", defaultValue = "20") int size,
                                                  @RequestParam(value = "lastTime",required = false) LocalDateTime lastTime,
                                                  @RequestParam(value = "lastId",required = false) Long lastRepostId) {

        List<RepostUnderNews> reposts=null;
        if(lastTime==null || lastRepostId == null) {
            reposts = repostService.firstGetNewsRepost(newsId,size);
        }else {
            reposts = repostService.afterGetNewsRepost(newsId, size, lastTime, lastRepostId);
        }
        NewsRepostInfinityResponse response = new NewsRepostInfinityResponse(reposts);
        return ApiResponse.of(response);
    }
}
