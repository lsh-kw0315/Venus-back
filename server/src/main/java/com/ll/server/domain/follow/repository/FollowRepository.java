package com.ll.server.domain.follow.repository;

import com.ll.server.domain.follow.entity.Follow;
import com.ll.server.domain.member.entity.Member;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.follower = :follower")
    Page<Follow> findFollowsByFollower(Member follower, Pageable pageable);

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.follower = :follower")
    List<Follow> findFollowsByFollower(Member follower);

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.followee = :followee")
    Page<Follow> findFollowsByFollowee(Member followee, Pageable pageable);

    List<Follow> findFollowsByFollowee(Member followee);

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.follower.nickname = :nickname")
    Page<Follow> findFollowsByFollower_Nickname(String nickname, Pageable pageable);

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.follower.nickname = :nickname")
    List<Follow> findFollowsByFollower_Nickname(String nickname);

    //구독자들 찾기
    @Query("select count(f) from Follow f where f.follower.nickname = :nickname")
    long countFollowsByFollower_Nickname(String Nickname);

    long countFollowsByFollower_Id(Long id);

    //페이지네이션 최초. 팔로잉하는 친구들 찾기
    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.follower.nickname = :nickname")
    List<Follow> findFollowsByFollower_Nickname(String nickname, Limit limit);


    //페이지네이션 2번째 이후. 팔로잉하는 친구들 찾기
    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.follower.nickname = :nickname and f.id > :lastId")
    List<Follow> findFollowsByFollower_NicknameAndIdGreaterThan(String nickname, Long lastId, Limit limit);
    //구독자들 찾기 끝


    //구독한 사람 찾기 영역
    @Query("select count(f) from Follow f where f.followee.nickname = :nickname")
    long countFollowsByFollowee_Nickname(String Nickname);

    long countFollowsByFollowee_Id(Long id);

    //페이지네이션 최초. 팔로우한 친구들 찾기
    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.followee.nickname = :nickname")
    List<Follow> findFollowsByFollowee_Nickname(String nickname, Limit limit);

    //페이지네이션 2번째 이후. 팔로우한 친구들 찾기
    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.followee.nickname = :nickname and f.id > :lastId")
    List<Follow> findFollowsByFollowee_NicknameAndIdGreaterThan(String nickname, Long lastId, Limit limit);
    //구독한 사람 찾기 끝

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.followee.nickname = :nickname")
    Page<Follow> findFollowsByFollowee_Nickname(String nickname, Pageable pageable);


    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.followee = :followee and f.follower = :follower")
    Follow findByFollowerAndFollowee(Member follower, Member followee);

    @Query("select f from Follow f join fetch f.followee join fetch f.follower where f.followee.nickname = :followeename and f.follower.nickname = :followername")
    Follow findByFollower_NicknameAndFollowee_Nickname(String followerName, String followeeName);
}
