package com.ptit.schedule.specification;

import com.ptit.schedule.entity.Subject;
import org.springframework.data.jpa.domain.Specification;

public class SubjectSpecification {

    public static Specification<Subject> hasSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern = "%" + search.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("subjectCode")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("subjectName")), searchPattern)
            );
        };
    }

    public static Specification<Subject> hasSemester(String semester) {
        return (root, query, criteriaBuilder) ->
                semester == null || semester.trim().isEmpty()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("semester").get("semesterName"), semester);
    }

    public static Specification<Subject> hasClassYear(String classYear) {
        return (root, query, criteriaBuilder) ->
                classYear == null || classYear.trim().isEmpty()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("major").get("classYear"), classYear);
    }

    public static Specification<Subject> hasMajor(String majorCode) {
        return (root, query, criteriaBuilder) ->
                majorCode == null || majorCode.trim().isEmpty()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("major").get("majorCode"), majorCode);
    }

    public static Specification<Subject> hasProgramType(String programType) {
        return (root, query, criteriaBuilder) ->
                programType == null || programType.trim().isEmpty()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("programType"), programType);
    }

    public static Specification<Subject> hasAcademicYear(String academicYear) {
        return (root, query, criteriaBuilder) ->
                academicYear == null || academicYear.trim().isEmpty()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("semester").get("academicYear"), academicYear);
    }
}