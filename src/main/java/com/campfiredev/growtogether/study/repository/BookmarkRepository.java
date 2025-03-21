package com.campfiredev.growtogether.study.repository;

import com.campfiredev.growtogether.study.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByMember_MemberIdAndStudy_StudyId(Long userId, Long studyId);
    int  countAllByMemberMemberId (Long memberId);

    List<Bookmark> findByMember_MemberId(Long memberId);

}

