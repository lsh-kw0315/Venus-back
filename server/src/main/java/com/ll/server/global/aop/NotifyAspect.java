package com.ll.server.global.aop;

import com.ll.server.domain.comment.dto.CommentDTO;
import com.ll.server.domain.follow.dto.FollowDTO;
import com.ll.server.domain.follow.entity.Follow;
import com.ll.server.domain.follow.repository.FollowRepository;
import com.ll.server.domain.like.dto.LikeDTO;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.member.repository.MemberRepository;
import com.ll.server.domain.mention.commentmention.dto.CommentMentionDTO;
import com.ll.server.domain.mention.repostmention.entity.RepostMention;
import com.ll.server.domain.news.news.dto.NewsDTO;
import com.ll.server.domain.news.news.dto.NewsResponse;
import com.ll.server.domain.notification.service.NotificationService;
import com.ll.server.domain.repost.dto.RepostDTO;
import com.ll.server.domain.repost.entity.Repost;
import com.ll.server.domain.repost.repository.RepostRepository;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import com.ll.server.global.sse.EmitterManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class NotifyAspect {
    private final NotificationService notificationService;
    private final FollowRepository followRepository;
    private final RepostRepository repostRepository;
    private final MemberRepository memberRepository;
    private final EmitterManager emitterManager;

    @Around("@annotation(com.ll.server.domain.notification.Notify)") //Notify 어노테이션이 붙은 메서드를 실행하면 이 놈이 대리로 실행 후 본인 할 일을 한다.
    public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = joinPoint.proceed(); //가로챌 대상이 되는 걸 proceed를 통해 호출한다. 비즈니스 로직을 여기서 실행했다고 보면 된다.


        if (obj instanceof RepostDTO repostDTO) {
            Long repostId = repostDTO.getRepostId();
            Repost repost = repostRepository.findById(repostId).get();

            Long followerId = repostDTO.getWriterId();
            String followerName = repostDTO.getNickname();

            Member follower = memberRepository.findById(followerId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));
            List<Follow> followList = followRepository.findFollowsByFollower(follower);

            String url = "http://localhost:8080/api/v1/repost/" + repostId;

            for (Follow follow : followList) {
                Member followee = follow.getFollowee();
                notificationService.saveNotification(followee, followerName + "님이 새 리포스트를 올렸습니다.", url);
            }

            List<RepostMention> repostMentions = repost.getMentions();
            String repostUser = repostDTO.getNickname();

            for (RepostMention mention : repostMentions) {
                Member mentionedUser = mention.getMember();
                if (mention.getMember().getId().equals(repostDTO.getWriterId())) continue;
                notificationService.saveNotification(mentionedUser, repostUser + "님이 당신을 멘션했습니다.", url);
            }

        }

        if (obj instanceof FollowDTO followDTO) {
            Follow follow = followRepository.findById(followDTO.getId()).get();

            Member follower = follow.getFollower();
            String followerName = followDTO.getFollower();
            String followeeName = followDTO.getFollowee();
            String url = "http://localhost:8080/api/v1/follows/followees?nickname=" + followerName;

            notificationService.saveNotification(follower, followeeName + "님이 팔로우하였습니다.", url);


        }

        if (obj instanceof CommentDTO commentDTO) {
            String url = "http://localhost:8080/api/v1/repost/" + commentDTO.getRepostId();

            Long repostWriterId = commentDTO.getRepostWriterId();
            Long commentWriterId = commentDTO.getCommentWriterId();

            Member repostWriter = memberRepository.findById(repostWriterId).get();

            if (!repostWriterId.equals(commentWriterId)) {
                notificationService.saveNotification(repostWriter, "내 리포스트에 " + commentDTO.getCommentWriterName() + "님이 댓글을 달았습니다.", url);
            }

            List<CommentMentionDTO> mentionList = commentDTO.getMentions();

            for (CommentMentionDTO mentionDTO : mentionList) {
                Long mentionedUserId = mentionDTO.getMentionUserId();

                if (mentionedUserId.equals(commentWriterId)) {
                    continue;
                }
                Member mentionedUser = memberRepository.findById(mentionedUserId).get();

                notificationService.saveNotification(mentionedUser, commentDTO.getCommentWriterName() + "님이 당신을 멘션했습니다.", url);
            }
        }

        if (obj instanceof LikeDTO LikeDTO) {
            Long repostId = LikeDTO.getRepostId();
            Long repostWriterId = LikeDTO.getRepostWriterId();

            String checkedUserName = LikeDTO.getCheckedUserName();
            Long checkedUserId = LikeDTO.getCheckedUserId();

            if (!repostId.equals(checkedUserId)) {
                Member user = memberRepository.findById(repostWriterId).get();
                String url = "http://localhost:8080/api/v1/reposts/" + repostId + "/likes";
                notificationService.saveNotification(user, checkedUserName + "님이 당신의 글에 좋아요를 눌렀습니다.", url);

            }
        }

        if (obj instanceof NewsDTO newsDTO) {
            createNotificationForNews(newsDTO);
        }

        if (obj instanceof NewsResponse newsResponse) {
            List<NewsDTO> newsDTOList = newsResponse.getNewsList();
            for (NewsDTO newsDTO : newsDTOList) {
                createNotificationForNews(newsDTO);
            }
        }

//        List<Notification> notifications = notificationService.findUnsentNotifications();
//
//        List<Long> successToSend = new ArrayList<>();
//        for (Notification notification : notifications) {
//            Long targetId = notification.getMember().getId();
//            NotificationDTO toSend = new NotificationDTO(notification);
//            if (emitterManager.sendNotification(targetId, toSend)) {
//                successToSend.add(toSend.getId());
//            }
//        }
//
//        if (!successToSend.isEmpty()) notificationService.sendNotifications(successToSend);

        return obj;
    }

    private void createNotificationForNews(NewsDTO NewsDTO) {
        Long newsId = NewsDTO.getId();
        String publisher = NewsDTO.getPublisherName();

        String url = "http://localhost:8080/api/v1/news/" + newsId;

        List<Follow> followeeList = followRepository.findFollowsByFollower_Nickname(publisher);

        for (Follow follow : followeeList) {
            Member followee = follow.getFollowee();
            notificationService.saveNotification(followee, publisher + " 언론사의 기사가 올라왔습니다.", url);
        }
    }
}
