package com.codit.backend_a.enrollment.domain;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="enrollments",
		uniqueConstraints = @UniqueConstraint(
			name = "uk_enrollment_lecture_user",
			columnNames = {"lecture_id", "user_id"}
		))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {
	//정책들
	//결제 후 취소 가능 일자
	public static final int CANCEL_DEADLINE_DAYS = 14;
	//미결제 자동 만료 기한
	public static final int PAYMENT_DEADLINE_HOURS = 24;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "lecture_id", nullable = false)
	private Long lectureId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EnrollmentStatus status;

	private Integer waitlistOrder; //null 허용이기 때문에 Integer.

	@Column(nullable = false)
	private LocalDateTime enrolledAt;
	private LocalDateTime paidAt;
	private LocalDateTime cancelledAt;

	//일반 신청 - 자리 있으므로 PENDING상태로 생성
	public static Enrollment generalCreate(Long lectureId, Long userId) {
		Enrollment enrollment = new Enrollment();
		enrollment.lectureId = lectureId;
		enrollment.userId = userId;
		enrollment.status = EnrollmentStatus.PENDING;
		enrollment.enrolledAt = LocalDateTime.now();
		return enrollment;
	}

	//대기열 등록 - 자리가 없어 WAITLISTED 상태로 생성
	public static Enrollment createQueue(Long lectureId, Long userId, int order) {
		Enrollment enrollment = new Enrollment();
		enrollment.lectureId = lectureId;
		enrollment.userId = userId;
		enrollment.status = EnrollmentStatus.WAITLISTED;
		enrollment.waitlistOrder = order;
		enrollment.enrolledAt = LocalDateTime.now();
		return enrollment;
	}

	//도메인 메서드 -> 상태 바뀜을 중심으로
	//결제 확정: PENDING → CONFIRMED
	public void confirm(){
		if (this.status != EnrollmentStatus.PENDING) {
			throw new BusinessException("결제 대기 상태에서만 확정 가능합니다. 현재 상태는: " + this.status);
		}
		this.status = EnrollmentStatus.CONFIRMED;
		this.paidAt = LocalDateTime.now(); //전환 시각
	}

	//수강 취소 -> 3가지 경우 고려하기
	//1. PENDING/WAITLISTED -> 즉시 취소
	//2. CONFIRMED -> paid_at 기준 14일 이내만 가능
	//3. CANCELLED -> 무시..
	public void cancel(){
		switch (this.status) {
			case PENDING, WAITLISTED -> {
				this.status = EnrollmentStatus.CANCELLED;
				this.cancelledAt = LocalDateTime.now();
			} case CONFIRMED -> {
				if(!isCancelDeadline()) {
					throw new BusinessException(
						"취소 가능 기간(" + CANCEL_DEADLINE_DAYS + "일)이 지났습니다."
					);
				}
				this.status = EnrollmentStatus.CANCELLED;
				this.cancelledAt = LocalDateTime.now();
			}
			case CANCELLED -> {}
		}
	}

	//대기자에서 수강자로(대기빠짐)
	public void waitOut() {
		if(this.status != EnrollmentStatus.WAITLISTED) {
			throw new BusinessException(
				"대기 중 상태에서만 변경이 가능합니다."
			);
		}
		this.status = EnrollmentStatus.PENDING;
		this.waitlistOrder = null;
	}

	//배치동작(시스템 만료)
	public void expireBySystem() {
		if(this.status != EnrollmentStatus.PENDING) return;
		this.status = EnrollmentStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
	}


	//24시간 미결제 자동 만료(배치)
	public boolean isPaymentEnd() {
		return this.status == EnrollmentStatus.PENDING
			&& LocalDateTime.now().isAfter(
				this.enrolledAt.plusHours(PAYMENT_DEADLINE_HOURS)
		);
	}

	public boolean isCancelDeadline() {
		return this.paidAt != null
			&& LocalDateTime.now().isBefore(
				this.paidAt.plusDays(CANCEL_DEADLINE_DAYS)
		);
	}

}
