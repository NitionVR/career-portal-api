package com.etalente.backend.controller;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/job-posts")
public class JobPostController {

    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;

    public JobPostController(JobPostRepository jobPostRepository, UserRepository userRepository) {
        this.jobPostRepository = jobPostRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<JobPost> createJobPost(@RequestBody JobPost jobPost, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        jobPost.setCreatedBy(currentUser);
        JobPost savedJobPost = jobPostRepository.save(jobPost);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedJobPost);
    }
}