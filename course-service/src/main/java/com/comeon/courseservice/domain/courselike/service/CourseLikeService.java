package com.comeon.courseservice.domain.courselike.service;

import com.comeon.courseservice.domain.common.exception.EntityNotFoundException;
import com.comeon.courseservice.domain.courselike.entity.CourseLike;
import com.comeon.courseservice.domain.course.repository.CourseRepository;
import com.comeon.courseservice.domain.courselike.repository.CourseLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseLikeService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;

    public Long updateCourseLike(Long courseId, Long userId) {
        CourseLike courseLike = courseLikeRepository.findByCourseIdAndUserIdFetchCourse(courseId, userId)
                .orElse(
                        CourseLike.builder()
                                .course(
                                        courseRepository.findById(courseId).orElseThrow(
                                                () -> new EntityNotFoundException("해당 식별값의 코스가 존재하지 않습니다. 요청한 코스 식별값 : " + courseId)
                                        )
                                )
                                .userId(userId)
                                .build()
                );

        if (Objects.isNull(courseLike.getId())) { // 좋아요가 없어서 새로 만들어진 경우
            return saveCourseLike(courseLike);
        } else { // 등록된 좋아요가 있는 경우
            // 좋아요가 등록된 코스의 count 1 감소
            removeCourseLike(courseLike);
            return null;
        }
    }

    private Long saveCourseLike(CourseLike courseLike) {
        return courseLikeRepository.save(courseLike).getId();
    }

    private void removeCourseLike(CourseLike courseLike) {
        // 좋아요가 등록된 코스의 count 1 감소
        courseLike.getCourse().decreaseLikeCount();
        courseLikeRepository.delete(courseLike);
    }
}