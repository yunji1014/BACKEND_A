package com.codit.backend_a.like.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
	name = "likes",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_likes_lecture_user",
		columnNames = {"lecture_id", "user_id"}
	)
)
@Getter
public class Like {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "lecture_id", nullable = false)
	private Long lectureId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	//찜
	public static Like create(Long userId, Long lectureId) {
		Like like = new Like();
		like.userId = userId;
		like.lectureId = lectureId;
		like.createdAt = LocalDateTime.now();
		return like;
	}


}