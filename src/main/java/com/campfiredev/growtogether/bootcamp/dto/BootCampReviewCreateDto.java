package com.campfiredev.growtogether.bootcamp.dto;

import com.campfiredev.growtogether.bootcamp.entity.BootCampReview;
import com.campfiredev.growtogether.bootcamp.type.ProgramCourse;
import com.campfiredev.growtogether.member.entity.MemberEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "부트캠프 리뷰 생성 요청 DTO")
public class BootCampReviewCreateDto {

    @Schema(description = "작성자 ID", example = "1")
    private Long memberId;

    @Schema(description = "리뷰 제목", example = "부트캠프 후기")
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title;

    @Schema(description = "리뷰 내용", example = "부트캠프에서 많은 것을 배웠어요!")
    @NotBlank(message = "내용은 필수 입력값입니다.")
    private String content;

   // @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
   // private String imageUrl;

    @Schema(description = "부트캠프 이름", example = "제로베이스")
    @NotBlank(message = "부트캠프 이름은 필수 입력값입니다.")
    private String bootCampName;

    @Schema(description = "학습 난이도 (1~5)", example = "4")
    @NotNull(message = "학습난이도는 필수 입력값입니다.")
    private Integer learningLevel;

    @Schema(description = "취업 지원 만족도 (1~5)", example = "5")
    @NotNull(message = "취업 지원 만족도는 필수 입력값입니다.")
    private Integer assistantSatisfaction;

    @Schema(description = "강의 만족도 (1~5)", example = "5")
    @NotNull(message = "강의 만족도는 필수 입력값입니다.")
    private Integer programSatisfaction;

    @Schema(description = "프로그램 과정", example = "BACKEND")
    @NotNull(message = "프로그램 과정은 필수 입력값 입니다.")
    private String programCourse;

    @Schema(description = "부트캠프 시작 날짜", example = "2024-06-01")
    @NotNull(message = "부트캠프 시작 날짜는 필수 입력값입니다.")
    private LocalDate bootCampStartDate;

    @Schema(description = "부트캠프 종료 날짜", example = "2024-06-30")
    @NotNull(message = "부트캠프 종료날짜는 필수 입력값입니다.")
    private LocalDate bootCampEndDate;

    @Schema(description = "배운 기술 목록", example = "[\n" +
            "        \"MySQL\",\n" +
            "        \"Spring Boot\"\n" +
            "    ]")
    private List<String> skillNames;


     // BootCampReviewCreateDto → BootCampReview 엔티티 변환
    public BootCampReview toEntity(MemberEntity member) {
        return BootCampReview.builder()
                .member(member)
                .title(this.title)
                .content(this.content)
               // .imageUrl(this.imageUrl)
                .bootCampName(this.bootCampName)
                .learningLevel(this.learningLevel)
                .assistantSatisfaction(this.assistantSatisfaction)
                .programSatisfaction(this.programSatisfaction)
                .programCourse(getProgramCourseEnum())
                .bootCampStartDate(this.bootCampStartDate)
                .bootCampEndDate(this.bootCampEndDate)
                .viewCount(0L) // 기본값 설정
                .likeCount(0)  // 기본값 설정
                .build();
    }

    // BootCampReview -> BootCampReviewCreateDTO
    public static BootCampReviewCreateDto fromEntity(BootCampReview review){

        List<String> skillNames = review.getBootCampSkills().stream()
                .map(skill -> skill.getSkill().getSkillName())
                .toList();

        return BootCampReviewCreateDto.builder()
                .memberId(review.getMember().getMemberId())
                .title(review.getTitle())
                .content(review.getContent())
            //    .imageUrl(review.getImageUrl())
                .bootCampName(review.getBootCampName())
                .learningLevel(review.getLearningLevel())
                .assistantSatisfaction(review.getAssistantSatisfaction())
                .programSatisfaction(review.getProgramSatisfaction())
                .programCourse(review.getProgramCourse().name())
                .bootCampStartDate(review.getBootCampStartDate())
                .bootCampEndDate(review.getBootCampEndDate())
                .skillNames(skillNames)
                .build();
    }

    public ProgramCourse getProgramCourseEnum(){
        return ProgramCourse.valueOf(programCourse.toUpperCase());
    }
}