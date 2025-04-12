package com.ll.server.global.security.custom;

import com.ll.server.domain.member.auth.interfaces.OAuth2UserInfo;
import com.ll.server.domain.member.entity.ConnectedProvider;
import com.ll.server.domain.member.entity.Member;
import com.ll.server.domain.member.enums.MemberRole;
import com.ll.server.domain.member.enums.Provider;
import com.ll.server.domain.member.repository.ConnectedProviderRepository;
import com.ll.server.domain.member.repository.MemberRepository;
import com.ll.server.global.response.enums.ReturnCode;
import com.ll.server.global.response.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j(topic = "CustomOAuth2UserService")
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ConnectedProviderRepository connectedProviderRepository;


    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, oAuth2User.getAttributes()); // Factory Pattern 적용

        Optional<Member> memberOptional = memberRepository.findMemberByProviderId(oAuth2UserInfo.getId());
        Optional<Member> emailDuplicate = memberRepository.findMemberByEmail(oAuth2UserInfo.getEmail());

        Member member = null;

        if(memberOptional.isPresent()){
            member = memberOptional.get();
        }else if(emailDuplicate.isPresent()){
            member = emailDuplicate.get();
            List<ConnectedProvider> connectedProviders = connectedProviderRepository.findConnectedProvidersByMember(member);

            if(connectedProviders.isEmpty()){
                String randomPassword = UUID.randomUUID().toString();
                String realPassword = passwordEncoder.encode(randomPassword);
                member.setPassword(realPassword);
            }

            ConnectedProvider connectedProvider = ConnectedProvider.builder()
                    .providerId(oAuth2UserInfo.getId())
                    .provider(Provider.valueOf(provider.toUpperCase()))
                    .member(member)
                    .build();

            connectedProviderRepository.save(connectedProvider);

        }else{
            member = createMember(oAuth2UserInfo, provider);
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name()));
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(oAuth2User, member, authorities);

        Authentication authentication = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return customOAuth2User;
    }

    protected Member createMember(OAuth2UserInfo oAuth2UserInfo, String provider) {
        // 소셜 로그인 사용자는 어차피 비밀번호로 로그인하지 않으므로, UUID를 비밀번호로
        String randomPassword = UUID.randomUUID().toString();
        String realPassword = passwordEncoder.encode(randomPassword);

        Member member = Member.builder()
                .email(oAuth2UserInfo.getEmail())
                .nickname(oAuth2UserInfo.getName())
                .password(realPassword)
                .role(MemberRole.USER)
                .build();
        memberRepository.save(member);

        ConnectedProvider connectedProvider = ConnectedProvider.builder()
                .providerId(oAuth2UserInfo.getId())
                .provider(Provider.valueOf(provider.toUpperCase()))
                .member(member)
                .build();
        connectedProviderRepository.save(connectedProvider);

        return member;
    }

    protected Member updateMember(Member existingMember, OAuth2UserInfo oAuth2UserInfo) {
        // OAuth2 정보와 일치하도록 기존 회원 정보 업데이트
        existingMember.setNickname(oAuth2UserInfo.getName());
        existingMember.setEmail(oAuth2UserInfo.getEmail());
        existingMember.setModifyDate(LocalDateTime.now());
        // 필요한 다른 정보들도 업데이트

        try {
            return memberRepository.save(existingMember);
        } catch (Exception e) {
            String errorMessage = String.format("OAuth2  : 회원 정보 업데이트에 실패했습니다: %s", e.getMessage());
            log.error(errorMessage);
            throw new CustomException(ReturnCode.INTERNAL_ERROR);
        }
    }
}
