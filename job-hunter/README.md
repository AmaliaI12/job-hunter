# Job Hunter

**Job Hunter** este o aplicatie web ce foloseste **Spring Boot 3**, **Thymeleaf** si **JPA**, care nu doar stocheaza datele, dar ofera si statistici vizuale si alerte inteligente.

## Tehnologii folosite

- Java Spring Boot
- Thymeleaf
- Bootstrap 5
- JavaScript (Vanilla + Libraries)


## 1. Arhitectura Aplicatiei (MVC)

Aplicatia urmeaza modelul clasic **Model-View-Controller (MVC)**, asigurand o separare clara a responsabilitatilor:

- **Model:** `JobApplication` (entitatea de baza)
- **View:** sabloane Thymeleaf (interfata HTML)
- **Controller:** `JobApplicationController` (gestioneaza cererile HTTP)
- **Service & Repository:** logica de business si pastrarea datelor

### Structura proiectului

```text
com.jobhunter.job_hunter
├── controller/
├── model/
├── repository/
├── service/
└── JobHunterApplication.java
```

## 2. Modelul de Date

Inima aplicatiei este clasa `JobApplication`. Nu am creat doar o structura de date simpla, ci am integrat Jakarta Validation pentru a asigura integritatea datelor direct de la sursa.

```java
@Entity
@Table(name = "JobsTable")
public class JobApplication {

    @NotBlank(message = "Titlul jobului este obligatoriu")
    private String jobTitle;

    @NotBlank(message = "Numele companiei este obligatoriu")
    private String companyName;

    @NotNull(message = "Data aplicarii este obligatorie")
    @PastOrPresent(message = "Data nu poate fi in viitor")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate applicationDate;

    @NotNull(message = "Trebuie sa selectezi un status")
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    // gettere si settere
}
```

In loc sa folosesc string-uri libere („Aplicat”, „aplicat”, „interviu”), am definit un enum strict `ApplicationStatus`. Acest lucru permite:

- gruparea usoara a statisticilor

- prevenirea erorilor de tastare

- coerenta la nivel de baza de date

```java
public enum ApplicationStatus {
    APLICAT,
    INTERVIU_HR,
    INTERVIU_TEHNIC,
    OFERTA_PRIMITA,
    RESPINS
}
```

## 3. Functionalitatea „Ghosting Detector”

Una dintre cele mai interesante functionalitati este detectarea lipsei de raspuns (_„ghosting”_). Daca au trecut mai mult de 14 zile de la aplicare si statusul este inca _APLICAT_, aplicatia marcheaza automat jobul ca fiind cu risc de ghosting.

```java
public boolean isGhosted() {
    if (this.applicationDate == null || this.status != ApplicationStatus.APLICAT) {
        return false;
    }
    long daysBetween = ChronoUnit.DAYS.between(this.applicationDate, LocalDate.now());
    return daysBetween > 14;
}
```

Acest boolean virtual (`isGhosted`) este utilizat ulterior in Thymeleaf pentru a afisa o alerta vizuala in tabel.

## 4. Logica de Business si Statistici (Streams API)

Service-ul (`JobApplicationService.java`) nu este doar un intermediar catre baza de date. Aici este implementata logica de calcul pentru dashboard.

Un exemplu relevant de utilizare a Java Streams este generarea datelor pentru un grafic de tip Pie Chart. Se grupeaza joburile dupa status si se numara intr-o singura linie de cod:

```java
public Map<String, Long> getStatusStatistics() {
    return getAllJobs().stream()
            .collect(Collectors.groupingBy(
                    job -> job.getStatus().toString(),
                    Collectors.counting()
            ));
}
```

Tot in Service sunt calculate si alte informatii, precum:

- salariul maxim

- ultima aplicare

## 5. Controller-ul

Clasa `JobApplicationController` gestioneaza rutele si filtrele. O provocare este gestionarea mai multor filtre simultan (cautare dupa nume, filtrare dupa status, sortare).

Aceasta problema a fost rezolvata prin verificari succesive in metoda `listJobs`:

```java
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
    } else {
        jobs = jobService.getAllJobs();
    }

    // adaugare atribute in Model pentru UI
    return "job-list";
}
```

Astfel, URL-ul devine dinamic, de exemplu:

```text
/jobs?statusFilter=INTERVIU_HR&sortBy=date
```

## 6. Exportul Datelor: Generarea CSV

Pentru a permite utilizatorului sa isi exporte datele, am implementat o functie de generare CSV direct in Controller. Fisierul este livrat direct in browser, fara a fi salvat pe server.

```java
@GetMapping("/export")
public void exportToCSV(HttpServletResponse response) throws IOException {
    response.setContentType("text/csv");
    response.setHeader("Content-Disposition", "attachment; filename=joburi.csv");

    PrintWriter writer = response.getWriter();
    writer.println("ID,Titlu Job,Companie,Data,Status,Salariu");

    for (JobApplication job : jobService.getAllJobs()) {
        // scrierea liniilor si gestionarea caracterelor speciale
    }
}
```

## Arhitectura Frontend: Dashboard-ul (`job-list.html`)

Pagina principală nu este doar un tabel static; este un dashboard interactiv. Am ales o abordare hibridă: HTML generat pe server pentru viteza, plus JavaScript pentru interactivitate dinamică.

### 6.1. Cardurile de Statistici

Scop: Oferirea unei viziuni de ansamblu instantanee asupra progresului.

Implementare: Folosim `th:text` pentru a injecta variabile calculate în backend (`totalJobs`, `maxSalary`).

Design Decision: Am folosit CSS Custom Properties (variabile CSS) precum `var(--text-muted)` pentru borduri și text.

Acest lucru permite schimbarea temei (Dark/Light) instantaneu, modificând doar valorile variabilelor în CSS, fără a schimba HTML-ul.

### 1.2. Vizualizare Date: Chart.js Integration

O reprezentare vizuală a distribuției statusurilor (Pie/Doughnut Chart).

Provocare: Cum trimitem datele din Java (Map<String, Long>) în JavaScript-ul care desenează graficul?

Soluție: Thymeleaf Inlining (th:inline="javascript").

```JavaScript
// Thymeleaf convertește automat Map-ul Java într-un obiect JSON valid
const statsData = [[${statusStats}]];
```

1.4. Modala de Detalii (Zero-Fetch Strategy)
Scop: Afișarea notițelor extinse fără a reîncărca pagina.

Design Decision: În loc să facem un request AJAX către server când utilizatorul apasă "Detalii" (ceea ce ar fi lent), am stocat toate datele necesare în Data Attributes (data-*) direct pe buton.

Implementare:

HTML

<button ... 
    th:data-title="${job.jobTitle}"
    th:data-notes="${job.notes}">
    Detalii
</button>
JavaScript: Un EventListener pe deschiderea modalei citește aceste atribute și populează câmpurile din modală instantaneu.

Avantaj: Viteză instantanee pentru utilizator.

Dezavantaj: HTML-ul este puțin mai mare, dar neglijabil pentru text.

1.5. Drag & Drop Reordering (Sortable.js)
Scop: Utilizatorul vrea să își prioritizeze vizual joburile.

Tehnologie: Librăria Sortable.js.

Persistență: Doar mutarea vizuală nu este suficientă. La finalul mutării (onEnd), declanșăm un fetch (POST) către endpoint-ul /jobs/reorder cu noua listă de ID-uri.

Snippet:

JavaScript

onEnd: function (evt) {
    // Colectează ordinea nouă a ID-urilor
    rows.forEach(row => ids.push(row.getAttribute('data-id')));
    // Trimite la server fără refresh
    fetch('/jobs/reorder', { body: JSON.stringify(ids) ... });
}

1.4. Modala de Detalii (Zero-Fetch Strategy)
Scop: Afișarea notițelor extinse fără a reîncărca pagina.

Design Decision: În loc să facem un request AJAX către server când utilizatorul apasă "Detalii" (ceea ce ar fi lent), am stocat toate datele necesare în Data Attributes (data-*) direct pe buton.

Implementare:

HTML

<button ... 
    th:data-title="${job.jobTitle}"
    th:data-notes="${job.notes}">
    Detalii
</button>
JavaScript: Un EventListener pe deschiderea modalei citește aceste atribute și populează câmpurile din modală instantaneu.

Avantaj: Viteză instantanee pentru utilizator.

Dezavantaj: HTML-ul este puțin mai mare, dar neglijabil pentru text.

1.5. Drag & Drop Reordering (Sortable.js)
Scop: Utilizatorul vrea să își prioritizeze vizual joburile.

Tehnologie: Librăria Sortable.js.

Persistență: Doar mutarea vizuală nu este suficientă. La finalul mutării (onEnd), declanșăm un fetch (POST) către endpoint-ul /jobs/reorder cu noua listă de ID-uri.

Snippet:

JavaScript

onEnd: function (evt) {
    // Colectează ordinea nouă a ID-urilor
    rows.forEach(row => ids.push(row.getAttribute('data-id')));
    // Trimite la server fără refresh
    fetch('/jobs/reorder', { body: JSON.stringify(ids) ... });
}
2. Formularul de Adăugare/Editare (job-form.html)
Pagina de formular este construită pentru a gestiona atât crearea ("Add New") cât și modificarea ("Edit") folosind același fișier HTML.

2.1. Form Binding & Validation
Thymeleaf Object: <form th:object="${job}"> leagă formularul de obiectul Java.

Input Hidden ID: <input type="hidden" th:field="*{id}" /> este crucial. Dacă ID-ul există, Spring știe să facă Update. Dacă e null, face Insert.

Feedback Erori:

HTML

<input type="text" th:field="*{jobTitle}" class="form-control" />
<small th:if="${#fields.hasErrors('jobTitle')}" th:errors="*{jobTitle}" style="color: var(--danger-color);"></small>
Dacă validarea @NotBlank din Java eșuează, Spring retrimite pagina, iar th:if afișează automat mesajul de eroare sub câmpul problematic.

2.2. Dropdown Dinamic din Enum
O tehnică avansată Thymeleaf pentru a nu hardcoda opțiunile "APLICAT", "INTERVIU...", etc. în HTML.

HTML

<option th:each="s : ${T(com.jobhunter.job_hunter.model.ApplicationStatus).values()}"
        th:value="${s}" 
        th:text="${s}"></option>
Explicație: T(...) permite accesarea claselor Java statice direct din HTML. Astfel, dacă adaugi un status nou în Java (ApplicationStatus.java), acesta apare automat în dropdown fără a modifica HTML-ul.

3. UX & Interactivitate Globală
3.1. Dark Mode System
Am implementat un sistem de teme robust care ține minte preferința utilizatorului.

CSS Variables: Toate culorile sunt definite prin variabile (ex: --bg-card).

HTML Attribute: Tema curentă este stocată în <html data-theme="dark">.

Local Storage:

La încărcare: Scriptul citește localStorage.getItem('theme').

La click: Scriptul schimbă atributul și salvează noua valoare.

Icon Management: Funcția updateIcon(theme) schimbă dinamic textul butonului între ☼ și ☾.

3.2. Filtrare și Căutare
Formularul de filtrare folosește metoda GET.

Selectarea unei opțiuni din dropdown declanșează onchange="this.form.submit()".

Aceasta reîncarcă pagina cu parametrii în URL: /jobs?statusFilter=RESPINS.

De ce GET? Permite utilizatorului să dea Bookmark la o căutare specifică (ex: "Toate interviurile la Google").


## Concluzie si Dezvoltari Ulterioare

Aplicatia Job Hunter demonstreaza cum Spring Boot poate transforma o aplicatie CRUD banala intr-un instrument analitic util. Prin utilizarea validarilor, a Stream-urilor Java si a unei arhitecturi curate, rezultatul este un cod usor de intretinut si extensibil.

Planuri de viitor:

- adaugarea autentificarii (Spring Security)

- incarcarea CV-urilor (file upload)

- trimiterea automata de emailuri de follow-up
