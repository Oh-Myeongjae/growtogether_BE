package com.campfiredev.growtogether.member.service;

import com.campfiredev.growtogether.exception.custom.CustomException;
import com.campfiredev.growtogether.exception.response.ErrorCode;
import com.campfiredev.growtogether.mail.service.EmailService;
import com.campfiredev.growtogether.member.dto.KakaoUserDto;
import com.campfiredev.growtogether.member.dto.MemberLoginDto;
import com.campfiredev.growtogether.member.dto.MemberRegisterDto;
import com.campfiredev.growtogether.member.entity.MemberEntity;
import com.campfiredev.growtogether.member.repository.MemberRepository;
import com.campfiredev.growtogether.member.util.JwtUtil;
import com.campfiredev.growtogether.point.service.PointService;
import com.campfiredev.growtogether.skill.entity.SkillEntity;
import com.campfiredev.growtogether.skill.repository.SkillRepository;
import com.campfiredev.growtogether.study.repository.StudyRepository;
import com.campfiredev.growtogether.study.repository.join.JoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.campfiredev.growtogether.exception.response.ErrorCode.NOT_VALID_TOKEN;
import static com.campfiredev.growtogether.exception.response.ErrorCode.USER_NOT_FOUND;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PointService pointService;
    private final StringRedisTemplate redisTemplate;
    private final StudyRepository studyRepository;


    private final S3Service s3Service;

    private final JwtUtil jwtUtil;


    private static final String RESET_PASSWORD_PREFIX = "RESET_PASSWORD:";
    private static final long TOKEN_EXPIRATION_TIME = 5; // 5분
    private final JoinRepository joinRepository;

    @Transactional
    public MemberEntity register(MemberRegisterDto request, MultipartFile profileImage) {
        // 이메일 인증 여부 확인
        if (!emailService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 중복 검사
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByNickName(request.getNickName())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (memberRepository.existsByPhone(request.getPhone())) {
            throw new CustomException(ErrorCode.DUPLICATE_PHONE);
        }

        // 프로필 이미지 업로드 후 파일 키 저장
        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            profileImageUrl = s3Service.uploadFile(profileImage);
        }

        // 회원 저장
        MemberEntity member = memberRepository.save(MemberEntity.builder()
                .nickName(request.getNickName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .githubUrl(request.getGithubUrl())
                .profileImageUrl(profileImageUrl)
                .build());

        // 선택한 기술 스택 저장
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            List<SkillEntity> skills = skillRepository.findAllById(request.getSkills());
            for (SkillEntity skill : skills) {
                //       skillRepository.save(new SkillEntity(user, skill));
            }
        }

        return member;
    }

    public String userLogin(MemberLoginDto memberLoginDto) {
        MemberEntity memberEntity = memberRepository.findByEmail(memberLoginDto.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        if (!passwordEncoder.matches(memberLoginDto.password(), memberEntity.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀립니다.");
        }

        pointService.updatePoint(memberEntity, 1);
        return jwtUtil.generateAccessToken(memberEntity.getEmail(), memberEntity.getMemberId(), memberEntity.getNickName());
    }

    // 프로필 이미지 삭제
    @Transactional
    public void deleteProfileImage(Long memberId) {
        // member 찾기
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //  기존 프로필 이미지가 있는지 확인
        if (member.getProfileImageUrl() == null) {
            throw new IllegalArgumentException("이미 프로필 이미지가 없습니다.");
        }

        // S3에서 이미지 삭제
        s3Service.deleteFile(member.getProfileImageUrl());

        //DB에서 프로필 이미지 Key 제거
        member.setProfileImageUrl(null);
        memberRepository.save(member);
    }

    @Transactional
    public String updateProfileImage(Long memberId, MultipartFile profileImage) {
        // 사용자 찾기
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //  기존 프로필 이미지 삭제 (있다면)
        if (member.getProfileImageUrl() != null) {
            s3Service.deleteFile(member.getProfileImageUrl());
        }

        // 새로운 이미지 업로드
        String newImageKey = s3Service.uploadFile(profileImage);

        //  DB에 새로운 Key 저장
        member.setProfileImageUrl(newImageKey);
        memberRepository.save(member);

        return newImageKey;
    }

    public String getProfileImageUrl(Long memberId) {
        // 사용자 찾기
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")); // 예외 담당자가 수정 예정

        // 프로필 이미지 존재 여부 확인e
        if (member.getProfileImageUrl() == null) {
            throw new IllegalArgumentException("사용자의 프로필 이미지가 없습니다.");
        }

        // S3에서 파일 URL 반환
        return member.getProfileImageUrl();
    }

    public MemberEntity kakaoLogin(KakaoUserDto kakaoUserDto) {
        // 카카오 고유 ID
        String kakaoId = kakaoUserDto.getId();

        // DB 조회: 우리 서비스에 가입된 사용자인지 체크
        Optional<MemberEntity> memberOpt = memberRepository.findByKakaoId(kakaoId);

        // 가입된 사용자가 없으면 신규 회원으로 저장
        MemberEntity member = memberOpt.orElseGet(() ->
                memberRepository.save(MemberEntity.builder()
                        .kakaoId(kakaoId)
                        .email(kakaoUserDto.getKakaoAccount().getEmail())
                        .nickName(kakaoUserDto.getProperties().getNickname())
                        .build())
        );

        // 포인트 업데이트
        pointService.updatePoint(member, 1);
        return member;
    }


    public MemberEntity findByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    public MemberEntity findById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    // 비밀번호 재설정 이메일 전송
    public void sendPasswordResetEmail(String email) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));

        // 재설정 토큰 생성 (UUID)
        String token = UUID.randomUUID().toString();

        // Redis에 저장 (5분 후 만료)
        redisTemplate.opsForValue().set(RESET_PASSWORD_PREFIX + token, email, TOKEN_EXPIRATION_TIME, TimeUnit.MINUTES);

        // 비밀번호 재설정 URL 생성
        String resetUrl = "http://localhost:3000/reset-password?token=" + token; // 프론트엔드 URL

        // 이메일 전송
        emailService.sendPasswordResetEmail(email, resetUrl);
    }
    // 비밀번호 재설정 처리
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email = redisTemplate.opsForValue().get(RESET_PASSWORD_PREFIX + token);
        if (email == null) {
            throw new CustomException(NOT_VALID_TOKEN);
        }

        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 변경 및 저장
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        // 사용한 토큰 삭제
        redisTemplate.delete(RESET_PASSWORD_PREFIX + token);
    }

    // 이메일 찾기 (마스킹 처리 후 반환)
    public String findEmail(String email) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

        return maskEmail(member.getEmail());
    }

    // 이메일 마스킹 처리
    private String maskEmail(String email) {
        int atIndex = email.indexOf("@"); // @ 위치 찾기
        if (atIndex < 2) {  // 이메일이 너무 짧으면 최소한만 마스킹
            return "*".repeat(atIndex) + email.substring(atIndex);
        }

        String firstPart = email.substring(0, 2);  // 앞 2글자 유지
        String maskedPart = "*".repeat(Math.max(atIndex - 4, 2)); // 중간 부분 마스킹 (최소 2개)
        String lastPart = email.substring(atIndex - 2, atIndex); // @ 앞 2글자 유지
        String domain = email.substring(atIndex); // 도메인은 그대로

        return firstPart + maskedPart + lastPart + domain;
    }


}