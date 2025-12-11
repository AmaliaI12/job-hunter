package com.jobhunter.job_hunter.controller;

import com.jobhunter.job_hunter.model.ApplicationStatus;
import com.jobhunter.job_hunter.model.JobApplication;
import com.jobhunter.job_hunter.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/** Clasa pentru gestionarea cererilor web legate de Job Applications
 * @author Ionescu Amalia
 * @version 10 Decembrie 2025
 */

@Controller
@RequestMapping("/jobs")
public class JobApplicationController {

    @Autowired
    private JobApplicationService jobService;

    @GetMapping
    public String listJobs(Model model,
                           @RequestParam(required = false) ApplicationStatus statusFilter,
                           @RequestParam(required = false) String sortBy,
                           @RequestParam(required = false) String keyword) {

        List<JobApplication> jobs;

        if (keyword != null && !keyword.isEmpty()) {
            jobs = jobService.searchByCompany(keyword);
        } else if (statusFilter != null) {
            jobs = jobService.filterByStatus(statusFilter);
        } else if ("salary".equals(sortBy)) {
            jobs = jobService.sortBySalaryDesc();
        } else if ("date".equals(sortBy)) {
            jobs = jobService.sortByDateDesc();
        } else {
            jobs = jobService.getAllJobs();
        }

        model.addAttribute("jobs", jobs);
        model.addAttribute("keyword", keyword);

        model.addAttribute("totalJobs", jobs.size());
        model.addAttribute("maxSalary", jobService.getMaxSalary(jobs));
        model.addAttribute("lastApplication", jobService.getLastApplicationDate(jobs));
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("statusStats", jobService.getStatusStatistics());

        return "job-list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("job", new JobApplication());
        return "job-form";
    }

    @PostMapping("/save")
    public String saveJob(@Valid @ModelAttribute("job") JobApplication job,
                          BindingResult result,
                          Model model) {
        if (result.hasErrors()) {
            return "job-form";
        }

        jobService.saveJob(job);
        return "redirect:/jobs";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        JobApplication job = jobService.getJobById(id);
        model.addAttribute("job", job);
        return "job-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteJob(@PathVariable("id") Long id) {
        jobService.deleteJob(id);
        return "redirect:/jobs";
    }

    @PostMapping("/reorder")
    @ResponseBody
    public void reorderJobs(@RequestBody List<Long> sortedIds) {
        jobService.updateJobPositions(sortedIds);
    }


    @GetMapping("/export")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=joburi_mele.csv";
        response.setHeader(headerKey, headerValue);

        List<JobApplication> listJobs = jobService.getAllJobs();

        PrintWriter writer = response.getWriter();

        writer.println("ID,Titlu Job,Companie,Data Aplicarii,Status,Salariu,Link");

        for (JobApplication job : listJobs) {
            writer.println(
                    job.getId() + "," +
                            escapeSpecialCharacters(job.getJobTitle()) + "," +
                            escapeSpecialCharacters(job.getCompanyName()) + "," +
                            job.getApplicationDate() + "," +
                            job.getStatus() + "," +
                            (job.getSalaryOffer() != null ? job.getSalaryOffer() : "0") + "," +
                            (job.getJobLink() != null ? job.getJobLink() : "")
            );
        }
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}