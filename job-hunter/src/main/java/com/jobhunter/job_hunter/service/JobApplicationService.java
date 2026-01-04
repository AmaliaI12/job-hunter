package com.jobhunter.job_hunter.service;

import com.jobhunter.job_hunter.model.ApplicationStatus;
import com.jobhunter.job_hunter.model.JobApplication;
import com.jobhunter.job_hunter.repository.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/** Clasa Service pentru logica de business
 * @author Ionescu Amalia
 * @version 4 Ianuarie 2026
 */
@Service
public class JobApplicationService {

    @Autowired
    private JobApplicationRepository repository;

    public List<JobApplication> getAllJobs() {
        return repository.findAll();
    }

    public void saveJob(JobApplication job) {
        repository.save(job);
    }

    public JobApplication getJobById(Long id) {
        Optional<JobApplication> optionalJob = repository.findById(id);
        return optionalJob.orElse(null);
    }

    public void deleteJob(Long id) {
        repository.deleteById(id);
    }

    public void updateJobPositions(List<Long> idList) {
        for (int i = 0; i < idList.size(); i++) {
            Long id = idList.get(i);
            final int currentPosition = i;
            repository.findById(id).ifPresent(job -> {
                job.setPosition(currentPosition);
                repository.save(job);
            });
        }
    }

    public List<JobApplication> filterByStatus(ApplicationStatus status) {
        if (status == null)
            return repository.findAll();
        return repository.findByStatus(status);
    }

    public List<JobApplication> sortBySalaryDesc() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "salaryOffer"));
    }

    public List<JobApplication> searchByCompany(String text) {
        return repository.findByCompanyName(text);
    }
    
    public List<JobApplication> sortByDateDesc() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "applicationDate"));
    }

    public Double getMaxSalary(List<JobApplication> jobs) {
        if (jobs == null || jobs.isEmpty())
            return 0.0;
        double max = 0.0;
        for (JobApplication job : jobs) {
            if (job.getSalaryOffer() != null && job.getSalaryOffer() > max) {
                max = job.getSalaryOffer();
            }
        }
        return max;
    }

    public String getLastApplicationDate(List<JobApplication> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return "N/A";
        }

        LocalDate mostRecent = null;
        for (JobApplication job : jobs) {
            LocalDate currentDate = job.getApplicationDate();
            if (currentDate != null) {
                if (mostRecent == null || currentDate.isAfter(mostRecent)) {
                    mostRecent = currentDate;
                }
            }
        }

        if (mostRecent != null)
            return mostRecent.toString();
        return "N/A";
    }

    public Map<String, Long> getStatusStatistics() {
        List<JobApplication> allJobs = getAllJobs();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> job.getStatus().toString(),
                        Collectors.counting()
                ));
    }
}