# Job Hunter

**Job Hunter** este o aplicație web de tip *Application Tracking System (ATS)* personal, dezvoltată folosind **Spring Boot 3**, **Thymeleaf** și **MS SQL Server** care are ca scop facilitarea procesului de căutare a unui loc de muncă. Aplicația oferă o imagine de ansamblu asupra oportunităților, statistici în timp real și alerte inteligente pentru situațiile de lipsă de răspuns.

## Tehnologii Utilizate

* **Backend:** Java 21, Spring Boot 3.4 (Web, Data JPA, Validation).
* **Database:** Microsoft SQL Server (MSSQL).
* **Frontend:** Thymeleaf (Server-Side Rendering), Bootstrap 5, CSS3 Custom Properties.
* **JavaScript:** Vanilla JS, Chart.js (Vizualizare date), Sortable.js (Drag & Drop).
* **Build Tool:** Maven.

---

## Backend: Arhitectură și Logică de Business

Aplicația este construită pe o arhitectură **MVC (Model-View-Controller)**, cu o separare strictă a logicii de business de interfața utilizator.

### 1. Modelul de Date și Validare

Entitățile sunt protejate împotriva datelor inconsistente folosind **Jakarta Validation**.

* **Enum Strict:** Statusul aplicației este gestionat printr-un `enum` (`ApplicationStatus`) pentru a garanta consistența datelor (APLICAT, INTERVIU_HR, OFERTA_PRIMITA etc.) și a preveni erorile de tip "magic strings".
* **Validări:** `@NotBlank`, `@PastOrPresent` asigură integritatea datelor înainte de a ajunge în baza de date.

### 2. Algoritmul "Ghosting Detector"

O funcționalitate cheie implementată în clasa de model este detectarea automată a joburilor unde compania nu a răspuns.

* **Logica:** Dacă au trecut mai mult de **14 zile** de la aplicare și statusul a rămas neschimbat (*APLICAT*), jobul este marcat intern ca fiind cu risc de "ghosting".
* **Implementare:** Utilizarea `ChronoUnit.DAYS` pentru calculul diferenței de timp între `LocalDate.now()` și data aplicării.

```java
public boolean isGhosted() {
    if (this.applicationDate == null || this.status != ApplicationStatus.APLICAT) {
        return false;
    }
    long daysBetween = ChronoUnit.DAYS.between(this.applicationDate, LocalDate.now());
    return daysBetween > 14;
}
```

### 3. Statistici Avansate cu Java Streams

Stratul de Service (`JobApplicationService`) utilizează **Java Streams API** pentru a procesa datele în memorie și a genera statistici pentru dashboard, reducând încărcarea pe baza de date pentru operațiuni de agregare simple.

* **Exemplu:** Gruparea joburilor după status pentru generarea graficelor.

```java
public Map<String, Long> getStatusStatistics() {
    return getAllJobs().stream()
            .collect(Collectors.groupingBy(
                    job -> job.getStatus().toString(),
                    Collectors.counting()
            ));
}
```

### 4. Filtrare Dinamică și Export

Controller-ul gestionează cereri complexe cu parametri opționali, permițând utilizatorului să combine filtrele:

* **Filtrare dupa mai multe criterii:** Căutare după nume (`keyword`), filtrare după `status` și sortare (`sortBy`) într-un singur request.
* **Export CSV:** Funcționalitate nativă de generare a rapoartelor CSV direct din `HttpServletResponse`, fără stocare intermediară pe disc.

---

## Gestionarea Bazei de Date (MS SQL Server)

Persistența datelor este asigurată de **Spring Data JPA** conectat la o instanță **Microsoft SQL Server**.

* **Configurare Dialect:** Optimizat pentru MSSQL folosind dialectul specific Hibernate.
* **Schema Generation:** Tabelele sunt generate și actualizate automat (`hibernate.ddl-auto=update`), mapând Enum-urile Java la coloane de tip `VARCHAR` pentru lizibilitate directă în baza de date.
* **Interogări:** Utilizarea `JpaRepository` pentru operațiuni CRUD standard și interogări derivate din numele metodelor (ex: `findByCompanyNameContainingIgnoreCase`).

---

## Frontend & UX

Interfața este randată pe server folosind **Thymeleaf**, completată de JavaScript pentru interactivitate dinamică.

### 1. **Dashboard Interactiv:**

* Injectarea datelor din Java direct în JavaScript (`th:inline="javascript"`) pentru a alimenta graficele **Chart.js**.
* Carduri de statistici calculate în backend (Total Joburi, Rata de răspuns).

### 2. **Zero-Fetch Modals:** 

Detaliile joburilor sunt încărcate instantaneu folosind `data-attributes` HTML populate de Thymeleaf, eliminând necesitatea unor request-uri AJAX suplimentare la deschiderea modalei.

### 3. **Dropdown Dinamic:**

Elementele `<option>` sunt generate iterând direct peste valorile `ApplicationStatus.values()` din Java, asigurând sincronizarea automată între Backend și Frontend.

### 4. **Drag & Drop (Sortable.js):** 

Permite prioritizarea vizuală a joburilor, cu persistența noii ordini printr-un apel asincron către API-ul `/jobs/reorder`.

### 5. **Sistem Dark Mode:** 

Implementat folosind variabile CSS și `localStorage` pentru persistența preferințelor utilizatorului.

### 6. **Responsive Design:** 

Interfața se adaptează pentru desktop și mobil, folosind sistemul Grid din Bootstrap 5.

---

## Structura Proiectului

```text
com.jobhunter.job_hunter
├── controller/       # Gestionare Request-uri HTTP & Export CSV
├── model/            # Entități JPA, Enums & Validări
├── repository/       # Interfețe Spring Data JPA (MSSQL)
├── service/          # Logică de business (Streams, Calcule)
└── JobHunterApplication.java
```

## Planuri de Viitor

* [ ] Implementarea **Spring Security** pentru autentificare și autorizare.
* [ ] Stocarea CV-urilor (File Upload) în baza de date sau Cloud Storage.
* [ ] Trimiterea automată de email-uri de follow-up (JavaMailSender).
* [ ] Migrarea către o arhitectură REST API + React/Angular (opțional).


## ⚙️ Configurare și Rulare

1. **Cerințe:**
* Java 21 JDK
* Microsoft SQL Server (instanță locală sau Docker)
* Maven


2. **Configurare Bază de Date:**
Actualizează fișierul `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=JobDatabase;...
spring.datasource.username=userul_tau
spring.datasource.password=parola_ta
```

3. **Rulare:**
```bash
mvn spring-boot:run
```

Aplicația va fi disponibilă la `http://localhost:8080/jobs`.