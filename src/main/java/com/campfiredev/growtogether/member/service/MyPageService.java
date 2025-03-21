package com.campfiredev.growtogether.member.service;

import com.campfiredev.growtogether.bootcamp.entity.ReviewLike;
import com.campfiredev.growtogether.bootcamp.repository.ReviewLikeRepository;
import com.campfiredev.growtogether.exception.custom.CustomException;
import com.campfiredev.growtogether.exception.response.ErrorCode;
import com.campfiredev.growtogether.member.dto.MyPageBookmarksDto;
import com.campfiredev.growtogether.member.dto.MyPageInfoDto;
import com.campfiredev.growtogether.member.dto.MyPageLikesDto;
import com.campfiredev.growtogether.member.entity.MemberEntity;
import com.campfiredev.growtogether.member.repository.MemberRepository;
import com.campfiredev.growtogether.skill.repository.SkillRepository;
import com.campfiredev.growtogether.study.entity.Bookmark;
import com.campfiredev.growtogether.study.repository.BookmarkRepository;
import com.campfiredev.growtogether.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MyPageService {

    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;
    private final StudyRepository studyRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReviewLikeRepository reviewLikeRepository;


     // 마이페이지 정보 조회 (프로필 + 찜한 부트캠프 후기 + 북마크한 스터디 게시글)
     public MyPageInfoDto getMyPageInfo(Long memberId) {
         MemberEntity member = memberRepository.findById(memberId)
                 .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

         // 사용자의 기술 스택 조회
         List<String> skills = skillRepository.findSkillNamesByMemberId(memberId);

         // 찜한 부트캠프 후기 + 북마크한 스터디 게시글 리스트 조회
         List<MyPageLikesDto> likedPosts = getMyLikedPosts(memberId);

         return MyPageInfoDto.builder()
                 .nickName(member.getNickName())
                 .profileImageUrl(member.getProfileImageUrl())
                 .points(member.getPoints())
                 .githubUrl(member.getGithubUrl())
                 .skills(skills)
                 .likedPostCount(likedPosts.size())
                 .likedPosts(likedPosts)
                 .build();


     }


    // 내가 참여 중인 스터디 개수 및 좋아요한 게시글 갯수 조회
    public MyPageBookmarksDto getMyBookmarks(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        int studyCount = studyRepository.countAllByMemberMemberId(memberId);
        int studyBookmarks = bookmarkRepository.countAllByMemberMemberId(memberId);
        int bootcampLikes = reviewLikeRepository.countAllByMemberMemberId(memberId);
        int totalLikedPosts = studyBookmarks + bootcampLikes;

        return MyPageBookmarksDto.builder()
                .studyCount(studyCount)
                .likedPostCount(totalLikedPosts)
                .build();
    }

    private List<MyPageLikesDto> getMyLikedPosts(Long memberId) {
        List<MyPageLikesDto> likedPosts = new ArrayList<>();

        // 1. 찜한 부트캠프 후기 조회
        List<ReviewLike> bootcampLikes = reviewLikeRepository.findByMemberMemberId(memberId);
        for (ReviewLike like : bootcampLikes) {
            if (like != null && like.getBootCampReview() != null) {
                likedPosts.add(MyPageLikesDto.builder()
                        .postId(like.getBootCampReview().getBootCampId())
                        .title(like.getBootCampReview().getTitle())
                        .type("부트캠프 리뷰")
                        .build());
            }
        }

        // 2. 북마크한 스터디 조회
        List<Bookmark> bookmarks = bookmarkRepository.findByMember_MemberId(memberId);
        for (Bookmark bookmark : bookmarks) {
            if (bookmark != null && bookmark.getStudy() != null) {
                likedPosts.add(MyPageLikesDto.builder()
                        .postId(bookmark.getStudy().getStudyId())
                        .title(bookmark.getStudy().getTitle())
                        .type("스터디")
                        .build());
            }
        }

        return likedPosts;
    }


}
