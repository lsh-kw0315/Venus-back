package com.ll.server.domain.repost.controller;

import com.ll.server.domain.comment.dto.CommentDTO;
import com.ll.server.domain.comment.dto.CommentInfinityScrollResponse;
import com.ll.server.domain.comment.dto.CommentModifyRequest;
import com.ll.server.domain.comment.dto.CommentWriteRequest;
import com.ll.server.domain.elasticsearch.repost.service.RepostDocService;
import com.ll.server.domain.like.dto.LikeDTO;
import com.ll.server.domain.like.dto.LikeResponse;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.member.service.MemberService;
import com.ll.server.domain.repost.dto.*;
import com.ll.server.domain.repost.service.RepostService;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import com.ll.server.global.response.response.ApiResponse;
import com.ll.server.global.response.response.CustomPage;
import com.ll.server.global.security.util.AuthUtil;
import com.ll.server.global.utils.MyConstant;
import com.ll.server.global.validation.PageLimitSizeValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reposts")
public class ApiV1RepostController {
    private final RepostService repostService;
    private final RepostDocService repostDocService;
    private final MemberService memberService;


    @Data
    private static class ClientPageRequest {
        private int page = 0;
        private int limit = 20;
    }

    //repost 영역
    //탭으로 repost 선택 시 호출됨. 검색 시에도 마찬가지.
    @GetMapping
    public ApiResponse<?> getAllRepost(@RequestParam(value = "keyword", defaultValue = "") String keyword,
                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        PageLimitSizeValidator.validateSize(page, size, MyConstant.PAGELIMITATION);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate", "id").descending());


        if (keyword.isBlank()) {
            Page<RepostOnly> result = repostService.findAll(pageable);
            return ApiResponse.of(CustomPage.of(result));
        }

        Page<RepostOnly> result = repostDocService.searchContent(keyword, pageable);
        return ApiResponse.of(CustomPage.of(result));

    }

    //인피니티스크롤 버전
    @SneakyThrows
    @GetMapping("/infinityTest")
    public ApiResponse<?> searchInfinity(@RequestParam(value = "keyword", defaultValue = "") String keyword,
                                         @RequestParam(value = "lastTime", required = false) LocalDateTime lastTime,
                                         @RequestParam(value = "lastId", required = false) Long lastId,
                                         @RequestParam(value = "size", defaultValue = "20") int size) {

        RepostInfinityScrollResponse result = null;
        //검색 안함
        if (keyword.isBlank()) {
            if (lastTime == null || lastId == null) {
                result = new RepostInfinityScrollResponse(repostService.firstGetAll(size));
                return ApiResponse.of(result);
            }

            result = new RepostInfinityScrollResponse(repostService.afterGetAll(size, lastTime, lastId));
            return ApiResponse.of(result);
        }

        //검색함
        if (lastTime == null || lastId == null) {
            result = new RepostInfinityScrollResponse(repostDocService.firstInfinitySearch(size, keyword));
            return ApiResponse.of(result);
        }

        result = new RepostInfinityScrollResponse(repostDocService.afterInfinitySearch(size, keyword, lastTime, lastId));
        return ApiResponse.of(result);
    }

    //repost 최초 상세 조회. 댓글에 페이지네이션을 적용 (전통적 페이지네이션)
    @GetMapping("/{repostId}")
    public ApiResponse<RepostDTO> getRepost(@PathVariable("repostId") Long id) {
        RepostDTO repost = repostService.getRepostDTOById(id);

        return ApiResponse.of(repost);
    }



    @GetMapping("/member/{memberId}")
    public ApiResponse<RepostOnly> getRepostById(@PathVariable("memberId") Long memberId,
                                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                                 @RequestParam(value = "size", defaultValue = "20") int size) {
        PageLimitSizeValidator.validateSize(page, size, MyConstant.PAGELIMITATION);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Member member = memberService.getMemberById(memberId);
        Page<RepostOnly> repostsByMember = repostService.findByMember(member, pageable);

        return ApiResponse.of(CustomPage.of(repostsByMember));
    }

    @GetMapping("/member/{memberId}/like")
    public ApiResponse<RepostDTO> getLikeRepostById(@PathVariable("memberId") Long memberId,
                                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        PageLimitSizeValidator.validateSize(page, size, MyConstant.PAGELIMITATION);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<RepostOnly> likeRepost = repostService.findLikeReposts(memberId, pageable);

        return ApiResponse.of(CustomPage.of(likeRepost));
    }

    @DeleteMapping("/{repostId}")
    public ApiResponse<String> deletePost(@PathVariable("repostId") Long id) {
        repostService.deleteRepost(id);

        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ApiResponse<RepostDTO> write(
            @RequestPart("request") RepostWriteRequest request, // JSON 데이터,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile // 이미지 파일 (선택 사항)
    ) throws IOException {
        return ApiResponse.of(repostService.save(request, imageFile));
    }

    //comment 영역

    //상세 조회 이후 댓글의 페이지네이션. (typical)
    @GetMapping("/{repostId}/comments")
    public ApiResponse<?> getPageComment(@PathVariable("repostId") Long postId,
                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "20") int size) {
        PageLimitSizeValidator.validateSize(page, size, MyConstant.PAGELIMITATION);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate", "id").ascending());

        Page<CommentDTO> result = repostService.getCommentPage(postId, pageable);
        return ApiResponse.of(CustomPage.of(result));
    }

    //상세 조회 이후 댓글의 페이지네이션. (cursor)
    @GetMapping("/{repostId}/comments/infinityTest")
    public ApiResponse<?> getInfinityComment(@PathVariable("repostId") Long postId,
                                             @RequestParam(value = "lastId", required = false) Long lastCommentId,
                                             @RequestParam(value = "lastTime", required = false) LocalDateTime lastTime,
                                             @RequestParam(value = "size", defaultValue = "20") int size) {
        CommentInfinityScrollResponse result = null;
        if(lastTime == null || lastCommentId == null) {
            result = new CommentInfinityScrollResponse(repostService.firstGetComment(postId,size));
        }else {
            result = new CommentInfinityScrollResponse(repostService.afterGetComment(postId, size, lastTime, lastCommentId));
        }
        return ApiResponse.of(result);
    }


    @DeleteMapping("/{repostId}/comments/{commentId}")
    public ApiResponse<String> deleteComment(@PathVariable("repostId") Long postId,
                                             @PathVariable("commentId") Long commentId) {
        repostService.deleteComment(postId, commentId);
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    @PatchMapping("/{repostId}/comments/{commentId}")
    public ApiResponse<CommentDTO> modifyComment(@PathVariable("repostId") Long postId,
                                                 @PathVariable("commentId") Long commentId,
                                                 @RequestBody CommentModifyRequest request) {
        return ApiResponse.of(repostService.modifyComment(postId, commentId, request.getContent()));
    }

    @PostMapping("/{repostId}/comments")
    public ApiResponse<CommentDTO> addComment(@PathVariable("repostId") Long postId,
                                              @RequestBody CommentWriteRequest request) {
        return ApiResponse.of(repostService.addComment(postId, request));
    }

    //like 영역
    @GetMapping("/{repostId}/likes")
    public ApiResponse<LikeResponse> getLikes(@PathVariable("repostId") Long repostId) {
        List<LikeDTO> likes = repostService.getAllLike(repostId);
        return ApiResponse.of(new LikeResponse(likes));
    }

    @DeleteMapping("/{repostId}/likes")
    public ApiResponse<String> deleteLike(@PathVariable("repostId") Long repostId) {

        repostService.deleteLike(repostId, AuthUtil.getCurrentMemberId());
        return ApiResponse.of(ReturnCode.SUCCESS);
    }

    @PostMapping("/{repostId}/likes")
    public ApiResponse<LikeDTO> markLike(@PathVariable("repostId") Long repostId) {
        return ApiResponse.of(repostService.markLike(repostId, AuthUtil.getCurrentMemberId()));
    }

    @PatchMapping("/{repostId}")
    public ApiResponse<?> setPinned(@PathVariable("repostId") Long repostId,
                                    @RequestBody RepostPinRequest request) {
        if (request == null) throw new CustomException(ReturnCode.WRONG_PARAMETER);

        if (request.isPinned()) {
            repostService.putPin(repostId);
        } else {
            repostService.pullPin(repostId);
        }

        return ApiResponse.of(ReturnCode.SUCCESS);

    }

    @GetMapping("/search")
    public ApiResponse<CustomPage<?>> searchByContent(@RequestParam("keyword") String keyword,
                                            @RequestParam(value = "page",defaultValue = "0")int page,
                                            @RequestParam(value = "size",defaultValue = "20")int size) {
        PageLimitSizeValidator.validateSize(page,size,MyConstant.PAGELIMITATION);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.of(CustomPage.of(repostService.searchContent(keyword, pageable)));
    }

    @GetMapping("/searchInfinity")
    public ApiResponse<RepostInfinityScrollResponse> searchByContentCursor(@RequestParam("keyword") String keyword,
                                                      @RequestParam(value = "size",defaultValue = "20") int size,
                                                      @RequestParam(value = "lastId",required = false)Long lastId,
                                                      @RequestParam(value = "lastTime",required = false)LocalDateTime lastTime) {

        List<RepostOnly> list=null;
        if(lastTime == null || lastId == null){
            list = repostService.searchContentFirst(keyword, size);
        }else{
            list = repostService.searchContentAfter(keyword, size, lastId, lastTime);
        }

        return ApiResponse.of(new RepostInfinityScrollResponse(list));
    }

    @GetMapping("/hot")
    public ApiResponse<List<RepostOnly>> getHotTopics() {
        return ApiResponse.of(repostService.getHotTopics());
    }
}
