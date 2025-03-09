package com.campfiredev.growtogether.study.service.notice;

import static com.campfiredev.growtogether.exception.response.ErrorCode.*;

import com.campfiredev.growtogether.exception.custom.CustomException;
import com.campfiredev.growtogether.study.dto.notice.NoticeCreateDto;
import com.campfiredev.growtogether.study.dto.notice.NoticeDetailsDto;
import com.campfiredev.growtogether.study.dto.notice.NoticeListDto;
import com.campfiredev.growtogether.study.dto.notice.NoticeUpdateDto;
import com.campfiredev.growtogether.study.entity.Study;
import com.campfiredev.growtogether.study.entity.notice.NoticeEntity;
import com.campfiredev.growtogether.study.repository.StudyRepository;
import com.campfiredev.growtogether.study.repository.notice.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

  private final NoticeRepository noticeRepository;
  private final StudyRepository studyRepository;

  /**
   * 공지 리스트 조회
   *
   * @param studyId  스터디 id
   * @param pageable pageable 객체
   * @return NoticeListDto
   */
  public NoticeListDto getNotices(Long studyId, Pageable pageable) {
    return NoticeListDto.fromEntityPage(noticeRepository.findByStudyId(studyId, pageable));
  }

  /**
   * 공지사항 내부
   *
   * @param noticeId 공지사항 ID
   * @return NoticeDetailsDto
   */
  public NoticeDetailsDto getNotice(Long noticeId) {
    return noticeRepository.findById(noticeId)
        .map(entity -> NoticeDetailsDto.fromEntity(entity))
        .orElseThrow(() -> new CustomException(NOTICE_NOT_FOUND));
  }

  /**
   * 공지 생성
   *
   * 로그인 개발 후 Long userId 파라미터 추가 예정 StudyEntity 개발 후 validateStudyLeader 메서드 추가 예정
   */
  public NoticeCreateDto.Response createNotice(Long studyId, NoticeCreateDto.Request request) {
    Study study = studyRepository.findById(studyId)
        .orElseThrow(() -> new CustomException(STUDY_NOT_FOUND));
    //로그인 후 추가 예정
    //validateStudyLeader(userId, studyId);

    return NoticeCreateDto.Response.fromEntity(noticeRepository.save(request.toEntity(study)));
  }

  /**
   * 공지 수정
   *
   * 로그인 개발 후 Long userId 파라미터 추가 예정 StudyEntity 개발 후 validateStudyLeader 메서드 추가 예정
   */
  public NoticeUpdateDto.Response updateNotice(Long noticeId, NoticeUpdateDto.Request request) {
    NoticeEntity noticeEntity = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(NOTICE_NOT_FOUND));

    //로그인 후 추가 예정
    //validateStudyLeader(userId, noticeEntity.getStudy().getId());

    noticeEntity.setTitle(request.getTitle());
    noticeEntity.setContent(request.getContent());

    return NoticeUpdateDto.Response.fromEntity(noticeEntity);
  }

  /**
   * 공지 삭제
   *
   * 로그인 개발 후 Long userId 파라미터 추가 예정 StudyEntity 개발 후 validateStudyLeader 메서드 추가 예정
   */
  public void deleteNotice(Long noticeId){
    NoticeEntity noticeEntity = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(NOTICE_NOT_FOUND));

    //StudyEntity 개발 후 추가 예정
    //validateStudyLeader(userId, noticeEntity.getStudy().getId());

    noticeRepository.deleteById(noticeId);
  }

  /**
   * 스터디 팀장인지 확인
   * StudyEntity 개발 후 추가 예정
   */
  private void validateStudyLeader(Long userId, Long studyId) {
//    StudyMemberEntity studyMemberEntity = studyMemberRepository.findByUserIdAndStudyId(userId, studyId)
//        .orElseThrow(() -> new CustomException(NOT_A_STUDY_MEMBER));
//
//    if(!LEADER.equals(studyMemberEntity.getStatus())){
//      throw new CustomException(NOT_A_STUDY_LEADER);
//    }
  }

}
