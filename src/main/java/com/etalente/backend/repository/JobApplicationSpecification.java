package com.etalente.backend.repository;

import com.etalente.backend.model.JobApplication;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JobApplicationSpecification {

    public static Specification<JobApplication> withFilters(UUID candidateId, String search, String sort) {
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

            if (sort != null && !sort.trim().isEmpty()) {
                switch (sort) {
                    case "date":
                        query.orderBy(criteriaBuilder.desc(root.get("applicationDate")));
                        break;
                    case "company":
                        query.orderBy(criteriaBuilder.asc(root.get("jobPost").get("company")));
                        break;
                    case "status":
                        query.orderBy(criteriaBuilder.asc(root.get("status")));
                        break;
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
