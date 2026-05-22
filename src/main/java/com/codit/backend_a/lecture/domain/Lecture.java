package com.codit.backend_a.lecture.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.codit.backend_a.common.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(nullable = false, length = 200)
	private String title;

	private String description;

	@Column(nullable = false)
	private Integer price;

	@Column(nullable = false)
	private Integer capacity;

	@Column(nullable = false)
	private Integer currentCount;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false,  length = 20)
	private LectureStatus status;

	@Column(nullable = false)
	private Integer likeCount;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	//생성 팩토리 - 강의 생성
	public static Lecture create(Long creatorId, String title, String description,
		Integer price, Integer capacity, LocalDate startDate,
		LocalDate endDate) {
		Lecture lecture = new Lecture();
		lecture.creatorId = creatorId;
		lecture.title = title;
		lecture.description = description;
		lecture.price = price;
		lecture.capacity = capacity;
		lecture.currentCount = 0;
		lecture.startDate = startDate;
		lecture.endDate = endDate;
		lecture.status = LectureStatus.DRAFT; //기본 생성값 DRAFT
		lecture.likeCount = 0;
		lecture.createdAt = LocalDateTime.now();
		lecture.updatedAt = LocalDateTime.now();
		return lecture;
	}

	//도메인 메서드 -> 상태 바뀌었을 때.
	// DRAFT → OPEN
	public void open(){
		if(status != LectureStatus.DRAFT){
			throw new BusinessException("이미 OPEN된 강의입니다.");
		}
		this.status = LectureStatus.OPEN;
		this.updatedAt = LocalDateTime.now();
	}

	//OPEN -> CLOSED
	public void close(){
		if(status != LectureStatus.OPEN){
			throw new BusinessException("강의를 닫을 수 없는 상태입니다. 현재 강의 상태: " + this.status);
		}
		this.status = LectureStatus.CLOSED;
		this.updatedAt = LocalDateTime.now();
	}

	//도메인 메서드: 정원 관리
	//수강 신청시 호출.. (비관적 락 트랜잭션 안에서만 사용)
	public void applyLectureCount(){
		if(this.currentCount >= this.capacity){
			throw new  BusinessException("정원이 마감되었습니다.");
		}
		this.currentCount += 1;
		this.updatedAt = LocalDateTime.now();
	}


	//수강 취소시 호출
	public void cancelLectureCount(){
		if(this.currentCount <= 0){
			throw new BusinessException("현재 수강 인원이 0명 이하입니다.");
		}
		this.currentCount -= 1;
		this.updatedAt = LocalDateTime.now();
	}

	//도메인 메서드 -> 찜하기
	public void inLikeCount(){
		this.likeCount += 1;
		this.updatedAt = LocalDateTime.now();
	}

	public void outLikeCount(){
		if(this.likeCount > 0)
			this.likeCount -= 1;
		this.updatedAt = LocalDateTime.now();
	}

	//도메인 메서드 -> 오픈 이후 강의 수정
	public void update(String title, String description, Integer price, Integer capacity,
		LocalDate startDate, LocalDate endDate) {
		//정원을 더 줄이면 문제!
		if(this.status == LectureStatus.OPEN && capacity < this.currentCount){
			throw new BusinessException(
				"정원은 현재 신청 인원(" + this.currentCount + "명) 이상이어야 합니다."
			);
		}
		if(endDate.isBefore(startDate)){
			throw new BusinessException("종료일은 시작일 이후여야 합니다.");
		}
		this.title = title;
		this.description = description;
		this.price = price;
		this.capacity = capacity;
		this.startDate = startDate;
		this.endDate = endDate;
		this.updatedAt = LocalDateTime.now();

	}

	//삭제 검증 (강의 종료일 이후에만 삭제 가능.)
	public void isCanDelete(){
		if(this.status == LectureStatus.OPEN){
			throw new BusinessException("모집중인 강의는 삭제할 수 없습니다.");
		}
		if(this.status == LectureStatus.CLOSED && LocalDate.now().isBefore(this.endDate)){
			throw new BusinessException(
				"강의 종료일(" + this.endDate + ") 이후에만 삭제할 수 있습니다."
			);
		}
	}


	//본인 강의인지 검증
	public void isOwner(Long requestUserId){
		if(!this.creatorId.equals(requestUserId)){
			throw new BusinessException("본인의 강의만 수정/삭제할 수 있습니다.");
		}
	}

	//조회 헬퍼
	public boolean isOpen(){
		return this.status == LectureStatus.OPEN;
	}

	public boolean isClosed(){
		return this.status == LectureStatus.CLOSED;
	}

	public boolean isFull(){
		return this.currentCount >= this.capacity;
	}

}
