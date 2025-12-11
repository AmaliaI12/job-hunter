package com.jobhunter.job_hunter.repository;

import com.jobhunter.job_hunter.model.JobApplication;
import com.jobhunter.job_hunter.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByStatus(ApplicationStatus status);
    List<JobApplication> findByCompanyName(String text);
}