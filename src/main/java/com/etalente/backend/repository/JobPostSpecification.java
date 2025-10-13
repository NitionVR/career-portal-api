package com.etalente.backend.repository;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobPostSpecification {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Specification<JobPost> withFilters(
            String search,
            String skillSearch,
            List<String> experienceLevels,
            List<String> jobTypes,
            List<String> workTypes) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // General search (title, description)
            Optional.ofNullable(search)
                    .filter(s -> !s.trim().isEmpty())
                    .ifPresent(s -> {
                        String likePattern = "%" + s.toLowerCase() + "%";
                        Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern);
                        Predicate descriptionMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);
                        predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch));
                    });

            // Skill search (within JSONB skills array) - FIXED
            Optional.ofNullable(skillSearch)
                    .filter(s -> !s.trim().isEmpty())
                    .ifPresent(s -> {
                        // Use PostgreSQL jsonb_path_exists with correct syntax
                        // Flag "i" for case-insensitive matching
                        String jsonPath = "$[*].name ? (@ like_regex \"" + s + "\" flag \"i\")";
                        predicates.add(criteriaBuilder.isTrue(
                                criteriaBuilder.function(
                                        "jsonb_path_exists",
                                        Boolean.class,
                                        root.get("skills"),
                                        criteriaBuilder.literal(jsonPath)
                                )
                        ));
                    });

            // Experience Levels
            Optional.ofNullable(experienceLevels)
                    .filter(list -> !list.isEmpty())
                    .ifPresent(list -> {
                        List<Predicate> experiencePredicates = new ArrayList<>();
                        for (String level : list) {
                            experiencePredicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("experienceLevel")), level.toLowerCase()));
                        }
                        predicates.add(criteriaBuilder.or(experiencePredicates.toArray(new Predicate[0])));
                    });

            // Job Types
            Optional.ofNullable(jobTypes)
                    .filter(list -> !list.isEmpty())
                    .ifPresent(list -> {
                        List<Predicate> jobTypePredicates = new ArrayList<>();
                        for (String type : list) {
                            jobTypePredicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("jobType")), type.toLowerCase()));
                        }
                        predicates.add(criteriaBuilder.or(jobTypePredicates.toArray(new Predicate[0])));
                    });

            // Work Types (remote)
            Optional.ofNullable(workTypes)
                    .filter(list -> !list.isEmpty())
                    .ifPresent(list -> {
                        List<Predicate> workTypePredicates = new ArrayList<>();
                        for (String type : list) {
                            workTypePredicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("remote")), type.toLowerCase()));
                        }
                        predicates.add(criteriaBuilder.or(workTypePredicates.toArray(new Predicate[0])));
                    });

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<JobPost> isPublic() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), JobPostStatus.OPEN);
    }

}
