package com.jobhunter.job_hunter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Clasa pentru definirea entitatii JobApplication
 * @author Ionescu Amalia
 * @version 10 Decembrie 2025
 */
@Entity
@Table(name = "JobsTable")
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Titlul jobului este obligatoriu")
    @Column(nullable = false)
    private String jobTitle;

    @NotBlank(message = "Numele companiei este obligatoriu")
    @Column(nullable = false)
    private String companyName;

    @NotNull(message = "Data aplicarii este obligatorie")
    @PastOrPresent(message = "Data nu poate fi in viitor")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate applicationDate;

    @NotNull(message = "Trebuie sa selectezi un status")
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Min(value = 0, message = "Salariul trebuie sa fie pozitiv")
    private Double salaryOffer;

    @Size(max = 500, message = "Descrierea este prea lunga")
    private String notes;

    private String jobLink;

    private Integer position;

    public JobApplication() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "Titlul jobului este obligatoriu") String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(@NotBlank(message = "Titlul jobului este obligatoriu") String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public @NotBlank(message = "Numele companiei este obligatoriu") String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(@NotBlank(message = "Numele companiei este obligatoriu") String companyName) {
        this.companyName = companyName;
    }

    public @NotNull(message = "Data aplicarii este obligatorie") @PastOrPresent(message = "Data nu poate fi in viitor") LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(@NotNull(message = "Data aplicarii este obligatorie") @PastOrPresent(message = "Data nu poate fi in viitor") LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public @Min(value = 0, message = "Salariul nu poate fi negativ") Double getSalaryOffer() {
        return salaryOffer;
    }

    public void setSalaryOffer(@Min(value = 0, message = "Salariul nu poate fi negativ") Double salaryOffer) {
        this.salaryOffer = salaryOffer;
    }

    public @Size(max = 500, message = "Descrierea este prea lunga") String getNotes() {
        return notes;
    }

    public void setNotes(@Size(max = 500, message = "Descrierea este prea lunga") String notes) {
        this.notes = notes;
    }

    public String getJobLink() {
        return jobLink;
    }

    public void setJobLink(String jobLink) {
        this.jobLink = jobLink;
    }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public boolean isGhosted() {
        if (this.applicationDate == null || this.status != ApplicationStatus.APLICAT) {
            return false;
        }
        long daysBetween = ChronoUnit.DAYS.between(this.applicationDate, LocalDate.now());
        return daysBetween > 14;
    }

}