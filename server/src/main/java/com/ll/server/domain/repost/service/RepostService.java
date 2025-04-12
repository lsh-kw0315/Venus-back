package com.ll.server.domain.repost.service;

import com.ll.server.domain.comment.dto.CommentDTO;
import com.ll.server.domain.comment.dto.CommentResponse;
import com.ll.server.domain.comment.dto.CommentWriteRequest;
import com.ll.server.domain.comment.entity.Comment;
import com.ll.server.domain.comment.repository.CommentRepository;
import com.ll.server.domain.like.dto.LikeDTO;
import com.ll.server.domain.like.dto.LikeResponse;
import com.ll.server.domain.like.entity.Like;
import com.ll.server.domain.like.repository.LikeRepository;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.member.repository.MemberRepository;
import com.ll.server.domain.member.service.MemberService;
import com.ll.server.domain.mention.commentmention.entity.CommentMention;
import com.ll.server.domain.mention.commentmention.repository.CommentMentionRepository;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.mention.repostmention.repository.RepostMentionRepository;
import com.ll.server.domain.news.news.entity.News;
import com.ll.server.domain.news.news.service.NewsService;
import com.ll.server.domain.notification.Notify;
import com.ll.server.domain.repost.dto.RepostDTO;
import com.ll.server.domain.repost.dto.RepostOnly;
import com.ll.server.domain.repost.dto.RepostUnderNews;
import com.ll.server.domain.repost.dto.RepostWriteRequest;
import com.ll.server.domain.repost.entity.Repost;
import com.ll.server.domain.repost.repository.RepostRepository;
import com.ll.server.global.aws.s3.S3Service;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import com.ll.server.global.response.exception.CustomRequestException;
import com.ll.server.global.security.util.AuthUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class RepostService {
    private final RepostRepository repostRepository;
    private final CommentRepository commentRepository;
    private final MemberService memberService;
    private final NewsService newsService;
    private final S3Service s3Service;
    private final MemberRepository memberRepository;
    private final LikeRepository likeRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final RepostMentionRepository repostMentionRepository;

    @Transactional
    @Notify
    public RepostDTO save(RepostWriteRequest request, MultipartFile imageFile) throws IOException {
        Member user = memberService.getMemberById(request.getWriterId());//AuthUtil.getCurrentMemberId()

        News news = newsService.getNews(request.getNewsId());

        List<Member> metionedMemberList = memberService.getMembersByNickName(request.getMentionedNames());


        // ✅ S3 업로드 로직 추가
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3Service.uploadFile(imageFile, "repost-images");
        }

        Repost repost =
                Repost.builder()
                        .member(user)
                        .news(news)
                        .content(request.getContent())
                        .imageUrl(imageUrl)
                        .build();

        repostRepository.save(repost);

        if (metionedMemberList != null && !metionedMemberList.isEmpty()) {
            for (Member mentionedMember : metionedMemberList) {
                repost.addMention(mentionedMember);
            }
        }

        news.addRepost(repost);

        return new RepostDTO(repost);
    }

    public List<RepostDTO> findByUserNickname(String nickname) {
        return repostRepository.findRepostsByMember_NicknameAndDeletedAtIsNull(nickname)
                .stream()
                .map(RepostDTO::new)
                .collect(Collectors.toList());
    }

    public Page<RepostOnly> findAll(Pageable pageable) {
        Page<Repost> result = repostRepository.findAllByDeletedAtIsNull(pageable);
        return getRepostOnlyPage(result);
    }

    @NotNull
    private Page<RepostOnly> getRepostOnlyPage(Page<Repost> result) {
        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfRelatedRepost(result.getContent());
        Map<Long, List<RepostMention>> map = repostMentions.stream().collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));
        return new PageImpl<>(
                result.getContent().stream()
                        .map(repost -> new RepostOnly(repost, map.get(repost.getId())))
                        .collect(Collectors.toList()),
                result.getPageable(),
                result.getTotalElements()
        );
    }

    public Page<RepostOnly> findByMember(Member member, Pageable pageable) {
        Page<Repost> result = repostRepository.findRepostsByMemberAndDeletedAtIsNull(member, pageable);
        return getRepostOnlyPage(result);
    }

    public List<RepostOnly> firstGetAll(int size) {
        List<Repost> reposts = repostRepository.findAllByDeletedAtIsNullOrderByCreateDateDescIdDesc(Limit.of(size));
        List<RepostMention> mentions = repostMentionRepository.getMentionsOfRelatedRepost(reposts);
        Map<Long, List<RepostMention>> map = mentions.stream().collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));

        return reposts.stream().map(repost -> new RepostOnly(repost, map.get(repost.getId()))).collect(Collectors.toList());
    }

    public List<RepostOnly> afterGetAll(int size, LocalDateTime lastTime, Long lastId) {
        List<Repost> reposts = repostRepository.findAllByDeletedAtIsNullAndCreateDateBeforeAndIdLessThanOrderByCreateDateDescIdDesc(lastTime, lastId, Limit.of(size));
        return getRepostOnlyList(reposts);
    }

    public Page<CommentDTO> getCommentPage(Long postId, Pageable pageable) {
        Repost repost = getRepost(postId);

        Page<Comment> comments = commentRepository.getCommentPage(repost, pageable);
        List<CommentMention> commentMentions = commentMentionRepository.getRelatedMentions(comments.getContent());
        Map<Long, List<CommentMention>> map = commentMentions.stream().collect(Collectors.groupingBy(commentMention -> commentMention.getComment().getId()));

        return new PageImpl<>(
                comments.stream()
                        .map(comment ->
                            new CommentDTO(comment, map.get(comment.getId()))
                        ).collect(Collectors.toList()),
                pageable,
                comments.getTotalElements()
        );
    }

    public List<CommentDTO> firstGetComment(Long postId, int size) {
        Repost repost = getRepost(postId);

        List<Comment> result = commentRepository.findCommentsByRepostAndDeletedAtIsNullOrderByCreateDateAscIdAsc(repost, Limit.of(size));
        return getCommentDTOS(result);
    }

    public List<CommentDTO> afterGetComment(Long postId, int size, LocalDateTime lastTime, long lastId) {
        Repost repost = getRepost(postId);
        List<Comment> result = commentRepository.findCommentsByRepostAndIdGreaterThanAndCreateDateAfterAndDeletedAtIsNullOrderByCreateDateAscIdAsc(repost, lastId, lastTime, Limit.of(size));
        return getCommentDTOS(result);
    }

    public List<CommentDTO> getAllComment(Long postId) {
        Repost repost = getRepost(postId);

        List<Comment> result = commentRepository.findCommentsByRepostAndDeletedAtIsNull(repost);
        return getCommentDTOS(result);
    }

    @NotNull
    private List<CommentDTO> getCommentDTOS(List<Comment> result) {
        List<CommentMention> commentMentions = commentMentionRepository.getRelatedMentions(result);
        Map<Long, List<CommentMention>> map = commentMentions.stream().collect(Collectors.groupingBy(commentMention -> commentMention.getComment().getId()));


        return result
                .stream()
                .map(comment -> new CommentDTO(comment,map.get(comment.getId()))).collect(Collectors.toList());
    }

    @Transactional
    public void deleteRepost(Long postId) {
        Repost target = getRepost(postId);

        checkWriter(target.getMember());

        target.delete();

    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        Repost repost = getRepost(postId);

        Comment target = getComment(commentId, repost);

        checkWriter(target.getMember());

        target.delete();
    }

    @Transactional
    public CommentDTO modifyComment(Long postId, Long commentId, String content) {
        Repost repost = getRepost(postId);

        Comment target = getComment(commentId, repost);

        checkWriter(target.getMember());

        target.setContent(content);
        List<CommentMention> commentMentions = commentMentionRepository.findCommentMentionsByComment(target);


        return new CommentDTO(target,commentMentions);
    }

    private void checkWriter(Member target) {
        Long currentMemberId = AuthUtil.getCurrentMemberId();
        Long targetMemberId = target.getId();
        if (!targetMemberId.equals(currentMemberId)) throw new CustomException(ReturnCode.NOT_AUTHORIZED);
    }

    private Comment getComment(Long commentId, Repost repost) {
        Comment c = commentRepository.findCommentByIdAndRepostAndDeletedAtIsNull(commentId,repost);
        if(c == null) throw new CustomException(ReturnCode.NOT_FOUND_ENTITY);
        return c;

    }

    @Transactional
    @Notify
    public CommentDTO addComment(Long postId, CommentWriteRequest request) {
        Repost repost = getRepost(postId);

        Member member = memberService.getMemberById(request.getWriterId());//AuthUtil.getCurrentMemberId());

        List<Member> mentionedMembers = memberService.getMembersByNickName(request.getMentionedNames());

        Comment comment = repost.addComment(member, mentionedMembers, request.getContent());

        return new CommentDTO(comment);
    }

    public Repost getRepost(Long postId) {
        return repostRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomRequestException(ReturnCode.NOT_FOUND_ENTITY));
    }

    public RepostDTO getRepostDTOById(Long postId) {
        Repost repost = getRepost(postId);

        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfOneRepost(repost);

        List<Comment> comments = commentRepository.getOneReposComment(repost);
        List<CommentMention> commentMentions = commentMentionRepository.getRelatedMentions(comments);
        Map<Long, List<CommentMention>> map = commentMentions.stream().collect(Collectors.groupingBy(commentMention -> commentMention.getComment().getId()));
        List<CommentDTO> commentDTOS = comments.stream().map(
                comment ->  new CommentDTO(comment, map.get(comment.getId()))
        ).toList();

        List<Like> likes = likeRepository.findLikesByRepostAndDeletedIsFalse(repost);
        List<LikeDTO> likeDTOS = likes.stream().map(LikeDTO::new).toList();

        return new RepostDTO(repost, repostMentions, new CommentResponse(commentDTOS), new LikeResponse(likeDTOS));
    }

    public List<LikeDTO> getAllLike(Long repostId) {
        Repost repost = getRepost(repostId);

        List<Like> result = likeRepository.findLikesByRepostAndDeletedIsFalse(repost);

        return result.stream().map(LikeDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public void deleteLike(Long repostId, Long userId) {
        Repost repost = getRepost(repostId);
        Member member = memberRepository.findById(userId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));

        Like result = likeRepository.findLikeByRepostAndMemberAndDeletedIsFalse(repost, member)
                .orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));

        result.setDeleted(true);
    }

    @Transactional
    @Notify
    public LikeDTO markLike(Long repostId, Long userId) {
        Repost repost = getRepost(repostId);

        Member user = memberService.getMemberById(userId);
        return new LikeDTO(repost.addLike(user));

    }

    @Transactional
    public void putPin(Long repostId) {
        Repost repost = getRepost(repostId);
        News news = repost.getNews();

        checkPublisher(news);

        Repost pinned = repostRepository.findRepostByNewsIdAndPinnedIsTrueAndDeletedAtIsNull(news.getId());
        //아무것도 찾지 못한 경우나 이미 지워진 경우
        if (pinned == null) {
            repost.setPinned(true);
            return;
        }

        //뭔가 있는 경우 대체
        pinned.setPinned(false);
        repost.setPinned(true);

    }

    private void checkPublisher(News news) {
        String currentUserNickname = AuthUtil.getCurrentMemberNickname();
        Collection<? extends GrantedAuthority> authorizations = AuthUtil.getAuth();

        if (news.getPublisher().equals(currentUserNickname) && authorizations != null) {
            for (GrantedAuthority auth : authorizations) {
                if (auth.getAuthority().equalsIgnoreCase("role_publisher")) {
                    return;
                }
            }
        }

        throw new CustomException(ReturnCode.NOT_AUTHORIZED);
    }

    @Transactional
    public void pullPin(Long repostId) {
        Repost repost = getRepost(repostId);
        News news = repost.getNews();

        checkPublisher(news);

        repost.setPinned(false);
    }

    public Page<RepostUnderNews> getNewsRepostCursorPagination(Long newsId, Pageable pageable) {
        Page<Repost> reposts = repostRepository.getNewsReposts(newsId, pageable);
        return new PageImpl<>(
                getRepostUnderNews(reposts.getContent()),
                reposts.getPageable(),
                reposts.getTotalElements()
        );
    }

    public List<RepostUnderNews> firstGetNewsRepost(Long newsId, int size) {
        //댓글 가져오는 것은 네이티브 쿼리의 Row Number를 이용해서 상위 몇 개 댓글만 나오게 할 수가 있다고 한다.
        //근데 이건 나중에 알아봐야할 듯..
        List<Repost> reposts = repostRepository.firstGetNewsReposts(newsId, Limit.of(size));
        return getRepostUnderNews(reposts);
    }

    @NotNull
    private List<RepostUnderNews> getRepostUnderNews(List<Repost> reposts) {
        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfRelatedRepost(reposts);
        Map<Long, List<RepostMention>> repostMentionMap = repostMentions.stream().collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));


        List<Comment> comments = commentRepository.getAllRepostComment(reposts);

        List<CommentMention> commentMentions = commentMentionRepository.getRelatedMentions(comments);
        Map<Long, List<CommentMention>> commentMentionMap = commentMentions.stream()
                .collect(Collectors.groupingBy(commentMention -> commentMention.getComment().getId()));

        Map<Long, List<CommentDTO>> commentMap = comments.stream().map(
                comment -> new CommentDTO(comment, commentMentionMap.get(comment.getId()))
        ).collect(Collectors.groupingBy(CommentDTO::getRepostId));

        List<Like> likes = likeRepository.getLikesOfRelatedReposts(reposts);
        Map<Long, List<LikeDTO>> likeMap = likes.stream().map(LikeDTO::new).collect(Collectors.groupingBy(LikeDTO::getRepostId));

        return reposts.stream()
                .map(repost -> new RepostUnderNews(repost,
                        new CommentResponse(commentMap.get(repost.getId())),
                        new LikeResponse(likeMap.get(repost.getId())),
                                repostMentionMap.get(repost.getId())
                )
                )
                .collect(Collectors.toList());
    }

    public List<RepostUnderNews> afterGetNewsRepost(Long newsId, int size, LocalDateTime lastTime, Long lastId) {
        List<Repost> reposts = repostRepository.afterGetNewsReposts(newsId, lastTime, lastId, Limit.of(size));
        return getRepostUnderNews(reposts);
    }

    public Page<RepostOnly> searchContent(String keyword, Pageable pageable) {
        Page<Repost> reposts = repostRepository.findByContentContainingAndDeletedAtIsNull(keyword,pageable);
        return new PageImpl<>(
                getRepostOnlyList(reposts.getContent()),
                reposts.getPageable(),
                reposts.getTotalElements()
        );
    }

    public List<RepostOnly> searchContentFirst(String keyword, int size){
        List<Repost> reposts = repostRepository.searchContentCursorFirst(keyword,Limit.of(size));
        return getRepostOnlyList(reposts);
    }

    public List<RepostOnly> searchContentAfter(String keyword, int size, Long lastId, LocalDateTime lastTime){
        List<Repost> reposts = repostRepository.searchContentCursorAfter(keyword,lastTime,lastId,Limit.of(size));
        return getRepostOnlyList(reposts);
    }

    @NotNull
    private List<RepostOnly> getRepostOnlyList(List<Repost> reposts) {
        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfRelatedRepost(reposts);
        Map<Long, List<RepostMention>> map = repostMentions.stream().collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));
        return reposts.stream().map(repost -> new RepostOnly(repost, map.get(repost.getId()))).toList();
    }

    public List<RepostOnly> getHotTopics() {
        LocalDateTime startOfDay = LocalDateTime.of(1970,1,1,0,0);
                //LocalDateTime.now().toLocalDate().atStartOfDay();
        List<Repost> hotReposts = repostRepository.findTodayshotReposts(startOfDay, PageRequest.of(0, 5));
        //top 5

        return getRepostOnlyList(hotReposts);

    }

    public Page<RepostOnly> findLikeReposts(Long memberId, Pageable pageable) {
        Page<Repost> reposts = repostRepository.findLikeRepost(memberId, pageable);
        List<RepostMention> repostMentions = repostMentionRepository.getMentionsOfRelatedRepost(reposts.getContent());
        Map<Long, List<RepostMention>> map = repostMentions.stream().collect(Collectors.groupingBy(repostMention -> repostMention.getRepost().getId()));
        List<RepostOnly> dtos = reposts.stream().map(repost -> new RepostOnly(repost, map.get(repost.getId()))).toList();

        return new PageImpl<>(dtos, reposts.getPageable(), reposts.getTotalElements());

    }
}
