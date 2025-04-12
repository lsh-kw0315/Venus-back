package com.ll.server.domain.follow.service;

import com.ll.server.domain.follow.dto.*;
import com.ll.server.domain.follow.entity.Follow;
import com.ll.server.domain.follow.repository.FollowRepository;
import com.ll.server.domain.member.dto.MemberDto;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.member.repository.MemberRepository;
import com.ll.server.domain.notification.Notify;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {
    private final FollowRepository followRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Notify
    public FollowDTO save(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) return null;

        Member follower = memberRepository.findById(followerId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));
        Member followee = memberRepository.findById(followeeId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));

        Follow find = followRepository.findByFollowerAndFollowee(follower, followee);
        if (find != null) throw new CustomException(ReturnCode.ALREADY_EXIST);

        Follow follow = Follow.builder()
                .follower(follower)
                .followee(followee)
                .build();

        return new FollowDTO(followRepository.save(follow));
    }

    public Page<FollowDTO> findByFollower(Long followerId, Pageable pageable) {
        Member follower = memberRepository.findById(followerId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));
        Page<Follow> result = followRepository.findFollowsByFollower(follower, pageable);
        return new PageImpl<>(
                result.getContent().stream().map(FollowDTO::new).collect(Collectors.toList()),
                result.getPageable(),
                result.getTotalElements()
        );
    }

    public Page<FollowDTO> findByFollowee(Long followeeId, Pageable pageable) {
        Member follower = memberRepository.findById(followeeId).orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));
        Page<Follow> result = followRepository.findFollowsByFollowee(follower, pageable);
        return new PageImpl<>(
                result.getContent().stream().map(FollowDTO::new).collect(Collectors.toList()),
                result.getPageable(),
                result.getTotalElements()
        );
    }

    public Page<MemberDto> findFollowees(String followerName, Pageable pageable) {
        Page<Follow> result = followRepository.findFollowsByFollower_Nickname(followerName, pageable);

        return new PageImpl<>(result.getContent().stream().map(follow -> new MemberDto(follow.getFollowee())).collect(Collectors.toList()),
                result.getPageable(),
                result.getTotalElements()
        );
    }


    public FolloweeListResponse firstGetFolloweesInfinity(String followerName, int size) {
        List<Follow> result = followRepository.findFollowsByFollower_Nickname(followerName, Limit.of(size));
        long totalSize = followRepository.countFollowsByFollower_Nickname(followerName);

        long nextLastId = getNextLastId(result);

        FolloweeListResponse response = FolloweeListResponse.builder()
                .followees(result.stream().map(follow -> new MemberDto(follow.getFollowee())).collect(Collectors.toList()))
                .lastId(nextLastId)
                .totalCount(totalSize)
                .build();

        return response;
    }

    public FolloweeInfinityScroll afterGetFolloweesInfinity(String followerName, int size, Long lastId) {
        List<Follow> result = followRepository.findFollowsByFollower_NicknameAndIdGreaterThan(followerName, lastId, Limit.of(size));

        long nextLastId = getNextLastId(result);

        FolloweeInfinityScroll response = FolloweeInfinityScroll.builder()
                .followees(result.stream().map(follow -> new MemberDto(follow.getFollowee())).collect(Collectors.toList()))
                .lastId(nextLastId)
                .build();

        return response;
    }

    private long getNextLastId(List<Follow> result) {
        long nextLastId;

        if (result == null || result.isEmpty()) nextLastId = -1L;
        else nextLastId = result.getLast().getId();
        return nextLastId;
    }


    public Page<MemberDto> findFollowers(String followeeName, Pageable pageable) {
        Page<Follow> result = followRepository.findFollowsByFollowee_Nickname(followeeName, pageable);

        return new PageImpl<>(
                result.getContent().stream().map(follow -> new MemberDto(follow.getFollower())).collect(Collectors.toList()),
                result.getPageable(),
                result.getTotalElements()
        );
    }

    public FollowerListResponse firstGetFollowersInfinity(String followeeName, int size) {
        List<Follow> result = followRepository.findFollowsByFollowee_Nickname(followeeName, Limit.of(size));
        long totalSize = followRepository.countFollowsByFollowee_Nickname(followeeName);

        long nextLastId = getNextLastId(result);

        FollowerListResponse response = FollowerListResponse.builder()
                .followers(result.stream().map(follow -> new MemberDto(follow.getFollower())).collect(Collectors.toList()))
                .lastId(nextLastId)
                .totalCount(totalSize)
                .build();

        return response;
    }

    public FollowerInfinityScroll afterGetFollowersInfinity(String followeeName, int size, Long lastId) {
        List<Follow> result = followRepository.findFollowsByFollowee_NicknameAndIdGreaterThan(followeeName, lastId, Limit.of(size));

        long nextLastId = getNextLastId(result);

        FollowerInfinityScroll response = FollowerInfinityScroll.builder()
                .followers(result.stream().map(follow -> new MemberDto(follow.getFollower())).collect(Collectors.toList()))
                .lastId(nextLastId)
                .build();

        return response;
    }

    public FollowDTO findByFollowerNameAndFolloweeName(String followerName, String followeeName) {
        return new FollowDTO(followRepository.findByFollower_NicknameAndFollowee_Nickname(followerName, followeeName));
    }

    public FollowDTO findById(Long followId) {
        return new FollowDTO(followRepository.findById(followId).get());
    }

    @Transactional
    public void delete(Long id) {
        Follow target = followRepository.findById(id)
                .orElseThrow(() -> new CustomException(ReturnCode.NOT_FOUND_ENTITY));

        followRepository.deleteById(id);
    }

    public long getFollowerCount(String nickname) {
        return followRepository.countFollowsByFollowee_Nickname(nickname);
    }

    public long getFolloweeCount(String nickname) {
        return followRepository.countFollowsByFollower_Nickname(nickname);
    }
}
