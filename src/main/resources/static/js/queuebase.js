const page = document.body.dataset.page;

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    if (response.status === 204) return null;
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        const detail = payload.details ? Object.values(payload.details).join(" ") : "";
        throw new Error([payload.message, detail].filter(Boolean).join(" ") || `Request failed (${response.status}).`);
    }
    return payload;
}

function notice(id, message, state = "") {
    const element = document.getElementById(id);
    if (!element) return;
    element.textContent = message;
    element.dataset.state = state;
}

function cell(row, value, className = "") {
    const element = document.createElement("td");
    element.textContent = value ?? "—";
    if (className) element.className = className;
    row.append(element);
    return element;
}

function actionButton(label, action, id, style = "button-quiet") {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `button button-small ${style}`;
    button.dataset.action = action;
    button.dataset.id = id;
    button.textContent = label;
    return button;
}

function statusBadge(status) {
    const badge = document.createElement("span");
    badge.className = `status-badge status-${status}`;
    badge.textContent = status;
    return badge;
}

async function loadDoctorsInto(select) {
    const doctors = await api("/doctors");
    select.replaceChildren(new Option("Select a doctor", ""));
    doctors.forEach(doctor => {
        select.add(new Option(`${doctor.name} · ${doctor.specialization}${doctor.active ? "" : " (inactive)"}`, doctor.id));
    });
    return doctors;
}

async function loadPatientsInto(select) {
    const patients = await api("/patients");
    select.replaceChildren(new Option("Select a patient", ""));
    patients.forEach(patient => select.add(new Option(`${patient.name} · ${patient.phone}`, patient.id)));
    return patients;
}

async function loadDoctorPage() {
    const form = document.getElementById("doctor-form");
    const rows = document.getElementById("doctor-rows");
    let doctors = [];
    const refresh = async () => {
        doctors = await api("/doctors");
        rows.replaceChildren();
        document.getElementById("doctor-list-count").textContent = `${doctors.length} total`;
        if (!doctors.length) {
            const row = rows.insertRow();
            const empty = cell(row, "No doctors registered yet.", "empty-row");
            empty.colSpan = 5;
            return;
        }
        doctors.forEach(doctor => {
            const row = rows.insertRow();
            cell(row, doctor.name);
            cell(row, doctor.specialization);
            cell(row, `${doctor.averageConsultationMinutes} min`);
            const state = cell(row, "");
            const badge = document.createElement("span");
            badge.className = `status-badge ${doctor.active ? "status-SERVING" : "status-CANCELLED"}`;
            badge.textContent = doctor.active ? "ACTIVE" : "INACTIVE";
            state.append(badge);
            const actions = cell(row, "", "action-cell");
            actions.append(actionButton("Edit", "edit", doctor.id), actionButton("Delete", "delete", doctor.id, "button-danger"));
        });
    };

    form.addEventListener("submit", async event => {
        event.preventDefault();
        const values = new FormData(form);
        const id = form.dataset.editId;
        const payload = {
            name: values.get("name").trim(),
            specialization: values.get("specialization").trim(),
            averageConsultationMinutes: Number(values.get("averageConsultationMinutes")),
            active: values.get("active") === "true"
        };
        try {
            await api(id ? `/doctors/${id}` : "/doctors", { method: id ? "PUT" : "POST", body: JSON.stringify(payload) });
            notice("doctor-notice", id ? "Doctor updated." : "Doctor added.", "success");
            form.reset();
            delete form.dataset.editId;
            document.getElementById("doctor-form-title").textContent = "Add doctor";
            document.getElementById("doctor-submit").textContent = "Save doctor";
            document.getElementById("doctor-cancel").hidden = true;
            await refresh();
        } catch (error) { notice("doctor-notice", error.message, "error"); }
    });
    form.addEventListener("reset", () => {
        delete form.dataset.editId;
        document.getElementById("doctor-form-title").textContent = "Add doctor";
        document.getElementById("doctor-submit").textContent = "Save doctor";
        document.getElementById("doctor-cancel").hidden = true;
    });
    rows.addEventListener("click", async event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;
        const doctor = doctors.find(item => String(item.id) === button.dataset.id);
        if (button.dataset.action === "edit" && doctor) {
            form.elements.name.value = doctor.name;
            form.elements.specialization.value = doctor.specialization;
            form.elements.averageConsultationMinutes.value = doctor.averageConsultationMinutes;
            form.elements.active.value = String(doctor.active);
            form.dataset.editId = doctor.id;
            document.getElementById("doctor-form-title").textContent = "Edit doctor";
            document.getElementById("doctor-submit").textContent = "Update doctor";
            document.getElementById("doctor-cancel").hidden = false;
        } else if (button.dataset.action === "delete" && window.confirm(`Delete ${doctor?.name ?? "this doctor"}?`)) {
            try { await api(`/doctors/${button.dataset.id}`, { method: "DELETE" }); await refresh(); }
            catch (error) { notice("doctor-notice", error.message, "error"); }
        }
    });
    try { await refresh(); } catch (error) { notice("doctor-notice", error.message, "error"); }
}

async function loadPatientPage() {
    const form = document.getElementById("patient-form");
    const rows = document.getElementById("patient-rows");
    let patients = [];
    const refresh = async () => {
        patients = await api("/patients");
        rows.replaceChildren();
        document.getElementById("patient-list-count").textContent = `${patients.length} total`;
        if (!patients.length) {
            const row = rows.insertRow();
            const empty = cell(row, "No patients registered yet.", "empty-row");
            empty.colSpan = 5;
            return;
        }
        patients.forEach(patient => {
            const row = rows.insertRow();
            cell(row, patient.name);
            cell(row, patient.age);
            cell(row, patient.phone);
            cell(row, patient.gender);
            const actions = cell(row, "", "action-cell");
            actions.append(actionButton("Edit", "edit", patient.id), actionButton("Delete", "delete", patient.id, "button-danger"));
        });
    };
    form.addEventListener("submit", async event => {
        event.preventDefault();
        const values = new FormData(form);
        const id = form.dataset.editId;
        const payload = { name: values.get("name").trim(), age: Number(values.get("age")), phone: values.get("phone").trim(), gender: values.get("gender").trim() };
        try {
            await api(id ? `/patients/${id}` : "/patients", { method: id ? "PUT" : "POST", body: JSON.stringify(payload) });
            notice("patient-notice", id ? "Patient updated." : "Patient registered.", "success");
            form.reset();
            delete form.dataset.editId;
            document.getElementById("patient-form-title").textContent = "Register patient";
            document.getElementById("patient-submit").textContent = "Save patient";
            document.getElementById("patient-cancel").hidden = true;
            await refresh();
        } catch (error) { notice("patient-notice", error.message, "error"); }
    });
    form.addEventListener("reset", () => {
        delete form.dataset.editId;
        document.getElementById("patient-form-title").textContent = "Register patient";
        document.getElementById("patient-submit").textContent = "Save patient";
        document.getElementById("patient-cancel").hidden = true;
    });
    rows.addEventListener("click", async event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;
        const patient = patients.find(item => String(item.id) === button.dataset.id);
        if (button.dataset.action === "edit" && patient) {
            form.elements.name.value = patient.name;
            form.elements.age.value = patient.age;
            form.elements.phone.value = patient.phone;
            form.elements.gender.value = patient.gender;
            form.dataset.editId = patient.id;
            document.getElementById("patient-form-title").textContent = "Edit patient";
            document.getElementById("patient-submit").textContent = "Update patient";
            document.getElementById("patient-cancel").hidden = false;
        } else if (button.dataset.action === "delete" && window.confirm(`Delete ${patient?.name ?? "this patient"}?`)) {
            try { await api(`/patients/${button.dataset.id}`, { method: "DELETE" }); await refresh(); }
            catch (error) { notice("patient-notice", error.message, "error"); }
        }
    });
    try { await refresh(); } catch (error) { notice("patient-notice", error.message, "error"); }
}

async function loadTokenPage() {
    const doctorSelect = document.getElementById("token-doctor");
    const patientSelect = document.getElementById("token-patient");
    try { await Promise.all([loadDoctorsInto(doctorSelect), loadPatientsInto(patientSelect)]); }
    catch (error) { notice("token-notice", error.message, "error"); }
    document.getElementById("token-form").addEventListener("submit", async event => {
        event.preventDefault();
        const payload = {
            doctorId: Number(doctorSelect.value),
            patientId: Number(patientSelect.value),
            priority: document.getElementById("token-priority").checked
        };
        try {
            const token = await api("/tokens", { method: "POST", body: JSON.stringify(payload) });
            document.getElementById("result-number").textContent = String(token.tokenNumber).padStart(2, "0");
            document.getElementById("result-patient").textContent = token.patient.name;
            document.getElementById("result-doctor").textContent = token.doctor.name;
            document.getElementById("result-wait").textContent = `${token.estimatedWaitMinutes ?? 0} minutes`;
            document.getElementById("result-status").textContent = token.status;
            document.getElementById("result-priority").hidden = !token.priority;
            document.getElementById("token-result").hidden = false;
            notice("token-notice", "Token added to today's queue.", "success");
        } catch (error) { notice("token-notice", error.message, "error"); }
    });
}

async function loadQueuePage() {
    const doctorSelect = document.getElementById("queue-doctor");
    const rows = document.getElementById("queue-rows");
    let selectedDoctor;
    const refresh = async () => {
        selectedDoctor = Number(doctorSelect.value);
        if (!selectedDoctor) return;
        const [queue, current, doctor] = await Promise.all([
            api(`/doctors/${selectedDoctor}/queue`),
            api(`/doctors/${selectedDoctor}/current-token`),
            api(`/doctors/${selectedDoctor}`)
        ]);
        document.getElementById("current-token-number").textContent = current ? String(current.tokenNumber).padStart(2, "0") : "—";
        document.getElementById("current-patient").textContent = current ? current.patient.name : "No patient currently serving";
        document.getElementById("current-doctor").textContent = doctor.name;
        document.getElementById("queue-count").textContent = `${queue.length} waiting`;
        rows.replaceChildren();
        if (!queue.length) {
            const row = rows.insertRow();
            const empty = cell(row, "No waiting tokens for this doctor today.", "empty-row");
            empty.colSpan = 5;
            return;
        }
        queue.forEach(token => {
            const row = rows.insertRow();
            cell(row, String(token.tokenNumber).padStart(2, "0"));
            cell(row, token.patient.name);
            const priority = cell(row, "");
            if (token.priority) {
                const badge = document.createElement("span");
                badge.className = "priority-badge";
                badge.textContent = "PRIORITY";
                priority.append(badge);
            } else { priority.textContent = "Normal"; }
            cell(row, `${token.estimatedWaitMinutes ?? 0} min`);
            const actions = cell(row, "", "action-cell");
            if (!token.priority) actions.append(actionButton("Mark priority", "priority", token.id, "button-quiet"));
        });
    };
    try {
        const doctors = await loadDoctorsInto(doctorSelect);
        const activeDoctor = doctors.find(doctor => doctor.active);
        if (activeDoctor) { doctorSelect.value = activeDoctor.id; await refresh(); }
    } catch (error) { notice("queue-notice", error.message, "error"); }
    doctorSelect.addEventListener("change", () => refresh().catch(error => notice("queue-notice", error.message, "error")));
    document.getElementById("next-patient").addEventListener("click", async () => {
        if (!doctorSelect.value) return notice("queue-notice", "Choose a doctor first.", "error");
        try {
            await api(`/doctors/${doctorSelect.value}/next`, { method: "POST" });
            notice("queue-notice", "Queue advanced.", "success");
            await refresh();
        } catch (error) { notice("queue-notice", error.message, "error"); }
    });
    rows.addEventListener("click", async event => {
        const button = event.target.closest("button[data-action='priority']");
        if (!button) return;
        try { await api(`/tokens/${button.dataset.id}/priority`, { method: "POST" }); await refresh(); }
        catch (error) { notice("queue-notice", error.message, "error"); }
    });
}

async function loadHistoryPage() {
    const doctorSelect = document.getElementById("history-doctor");
    const dateInput = document.getElementById("history-date");
    const rows = document.getElementById("history-rows");
    let currentPage = 0;
    let pageCount = 0;
    dateInput.value = new Date().toISOString().slice(0, 10);
    const refresh = async () => {
        if (!doctorSelect.value) return;
        const date = dateInput.value;
        const result = await api(`/doctors/${doctorSelect.value}/history?date=${encodeURIComponent(date)}&page=${currentPage}&size=20`);
        pageCount = result.totalPages;
        document.getElementById("history-count").textContent = `${result.totalElements} tokens`;
        document.getElementById("history-page").textContent = `Page ${result.totalPages ? currentPage + 1 : 0} of ${result.totalPages}`;
        document.getElementById("history-title").textContent = `Tokens for ${date}`;
        document.getElementById("history-previous").disabled = currentPage <= 0;
        document.getElementById("history-next").disabled = currentPage + 1 >= pageCount;
        rows.replaceChildren();
        if (!result.content.length) {
            const row = rows.insertRow();
            const empty = cell(row, "No history for this doctor and date.", "empty-row");
            empty.colSpan = 7;
            return;
        }
        result.content.forEach(token => {
            const row = rows.insertRow();
            cell(row, String(token.tokenNumber).padStart(2, "0"));
            cell(row, token.patient.name);
            cell(row, token.doctor.name);
            cell(row, token.priority ? "Priority" : "Normal");
            const status = cell(row, "");
            status.append(statusBadge(token.status));
            cell(row, `${token.estimatedWaitMinutes ?? 0} min`);
            cell(row, token.createdAt ? new Date(token.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—");
        });
    };
    try { await loadDoctorsInto(doctorSelect); }
    catch (error) { notice("history-notice", error.message, "error"); }
    document.getElementById("history-search").addEventListener("click", () => { currentPage = 0; refresh().catch(error => notice("history-notice", error.message, "error")); });
    doctorSelect.addEventListener("change", () => { currentPage = 0; refresh().catch(error => notice("history-notice", error.message, "error")); });
    document.getElementById("history-previous").addEventListener("click", () => { currentPage = Math.max(0, currentPage - 1); refresh(); });
    document.getElementById("history-next").addEventListener("click", () => { if (currentPage + 1 < pageCount) currentPage++; refresh(); });
}

async function loadDashboard() {
    document.getElementById("today-date").textContent = new Date().toLocaleDateString(undefined, { weekday: "long", year: "numeric", month: "long", day: "numeric" });
    try {
        const [doctors, patients, tokens] = await Promise.all([
            api("/doctors"), api("/patients"), api("/tokens?page=0&size=8&sort=createdAt,desc")
        ]);
        document.getElementById("doctor-count").textContent = doctors.length;
        document.getElementById("patient-count").textContent = patients.length;
        document.getElementById("active-doctor-count").textContent = doctors.filter(doctor => doctor.active).length;
        document.getElementById("token-count").textContent = tokens.totalElements;
        const rows = document.getElementById("recent-token-rows");
        rows.replaceChildren();
        tokens.content.forEach(token => {
            const row = rows.insertRow();
            cell(row, String(token.tokenNumber).padStart(2, "0"));
            cell(row, token.patient.name);
            cell(row, token.doctor.name);
            const status = cell(row, "");
            status.append(statusBadge(token.status));
            cell(row, `${token.estimatedWaitMinutes ?? 0} min`);
        });
        if (!tokens.content.length) {
            const row = rows.insertRow();
            const empty = cell(row, "No tokens have been generated yet.", "empty-row");
            empty.colSpan = 5;
        }
    } catch (error) { notice("dashboard-notice", error.message, "error"); }
}

if (page === "dashboard") loadDashboard();
if (page === "doctors") loadDoctorPage();
if (page === "patients") loadPatientPage();
if (page === "token") loadTokenPage();
if (page === "queue") loadQueuePage();
if (page === "history") loadHistoryPage();