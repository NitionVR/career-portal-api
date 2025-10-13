package com.etalente.backend.repository;

import com.etalente.backend.model.JobApplication;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JobApplicationSpecification {

    public static Specification<JobApplication> withFilters(UUID candidateId, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("candidate").get("id"), candidateId));

            Optional.ofNullable(search)
                    .filter(s -> !s.trim().isEmpty())
                    .ifPresent(s -> {
                        String likePattern = "%" + s.toLowerCase() + "%";
                        Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("jobPost").get("title")), likePattern);
                        Predicate companyMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("jobPost").get("company")), likePattern);
                        predicates.add(criteriaBuilder.or(titleMatch, companyMatch));
                    });

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
