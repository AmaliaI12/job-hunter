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

/** Clasa pentru gestionarea interactiunii dintre browser si baza de date
 * @author Ionescu Amalia
 * @version 4 Ianuarie 2026
 */

@Controller
@RequestMapping("/jobs") // Toate link-urile din clasa vor incepe cu /jobs
public class JobApplicationController {

    @Autowired // Creeaza automat Service-ul
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

        // Statistici
        model.addAttribute("totalJobs", jobs.size());
        model.addAttribute("maxSalary", jobService.getMaxSalary(jobs));
        model.addAttribute("lastApplication", jobService.getLastApplicationDate(jobs));

        model.addAttribute("selectedStatus", statusFilter);

        // Datele pentru pie chart
        model.addAttribute("statusStats", jobService.getStatusStatistics());

        return "job-list";
    }

    // Formular adaugare
    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("job", new JobApplication());
        return "job-form";
    }

    // Salvare job
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
    @ResponseBody // nu returneaza o pagina html
    public void reorderJobs(@RequestBody List<Long> sortedIds) {
        jobService.updateJobPositions(sortedIds);
    }


    @GetMapping("/export")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        // tip fisier = csv
        response.setContentType("text/csv");
        String headerKey = "Content-Disposition";
        // Deschide fereastra "Save As"
        String headerValue = "attachment; filename=joburi.csv";
        response.setHeader(headerKey, headerValue);

        List<JobApplication> listJobs = jobService.getAllJobs();

        PrintWriter writer = response.getWriter();

        writer.println("ID,Titlu Job,Companie,Data Aplicarii,Status,Salariu,Link,Descriere");

        for (JobApplication job : listJobs) {
            writer.println(
                    job.getId() + "," +
                            escapeSpecialCharacters(job.getJobTitle()) + "," +
                            escapeSpecialCharacters(job.getCompanyName()) + "," +
                            job.getApplicationDate() + "," +
                            job.getStatus() + "," +
                            (job.getSalaryOffer() != null ? job.getSalaryOffer() : "0") + "," +
                            (job.getJobLink() != null ? job.getJobLink() : "")  + "," +
                            (job.getNotes() != null ? job.getNotes() : "")
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