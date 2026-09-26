const state = {
    modo: null,
    categoria: null
};

const categorias = [
    ["assedio", "Assédio", "Registro identificado das partes envolvidas."],
    ["aluno", "Aluno", "Denúncia relacionada a aluno."],
    ["professor", "Professor", "Denúncia relacionada a professor."],
    ["tecnico", "Técnico", "Denúncia relacionada a técnico."],
    ["infraestrutura", "Infraestrutura", "Problemas físicos e de estrutura."],
    ["outros", "Outros", "Assuntos que não se enquadram nas demais opções."]
];

const fields = {
    assedio: `
        ${field("assediado", "Nome do Assediado", "text")}
        ${field("assediador", "Nome do Assediador", "text")}
        ${selectField("ocupacao", "Ocupação do Assediador", ["Aluno", "Professor", "Técnico", "Outros"])}
        ${field("local", "Local do Assédio", "text")}
        ${field("data", "Data do Acontecimento", "date")}
        ${field("hora", "Horário aproximado", "time")}
        ${textareaField("relato", "Relato do Fato")}
    `,
    aluno: `
        ${field("curso", "Nome do Curso", "text")}
        ${field("aluno", "Nome do Aluno Denunciado", "text")}
        ${textareaField("relato", "Relato do Fato")}
    `,
    professor: `
        ${field("curso", "Nome do Curso", "text")}
        ${field("professor", "Nome do Professor Denunciado", "text")}
        ${field("disciplina", "Disciplina", "text")}
        ${textareaField("relato", "Relato do Fato")}
    `,
    tecnico: `
        ${field("tecnico", "Nome do Técnico", "text")}
        ${textareaField("relato", "Relato do Fato")}
    `,
    infraestrutura: `
        ${selectField("area", "Área", ["Área Comum", "Ar Condicionado", "Banheiros", "Bebedouros", "Pintura", "Piso", "Restaurante", "Sala de Aula", "Outros"])}
        ${textareaField("relato", "Relato do Fato")}
    `,
    outros: `
        ${textareaField("relato", "Relato do Fato")}
    `
};

function field(id, label, type) {
    return `
        <div class="field">
            <label for="${id}">${label}</label>
            <input id="${id}" name="${id}" type="${type}" required>
        </div>`;
}

function textareaField(id, label) {
    return `
        <div class="field">
            <label for="${id}">${label}</label>
            <textarea id="${id}" name="${id}" maxlength="5000" required></textarea>
        </div>`;
}

function selectField(id, label, options) {
    return `
        <div class="field">
            <label for="${id}">${label}</label>
            <select id="${id}" name="${id}" required>
                <option value="">Selecione...</option>
                ${options.map(o => `<option value="${o}">${o}</option>`).join("")}
            </select>
        </div>`;
}

function show(id) {
    document.querySelectorAll(".screen").forEach(s => s.classList.remove("active"));
    document.getElementById(id).classList.add("active");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderCategories() {
    const grid = document.getElementById("category-grid");
    const visible = state.modo === "anonimo"
        ? categorias.filter(c => c[0] !== "assedio")
        : categorias;

    grid.innerHTML = visible.map(([id, title, desc]) => `
        <button class="category-card" data-category="${id}">
            <strong>${title}</strong>
            <small>${desc}</small>
        </button>
    `).join("");

    grid.querySelectorAll("[data-category]").forEach(btn => {
        btn.addEventListener("click", () => openForm(btn.dataset.category));
    });

    document.getElementById("mode-description").textContent =
        state.modo === "anonimo"
            ? "Modo anônimo: a opção Assédio não está disponível."
            : "Modo identificado: escolha o tipo de registro.";
}

function openForm(category) {
    state.categoria = category;
    document.getElementById("form-title").textContent =
        categorias.find(c => c[0] === category)[1];
    document.getElementById("form-fields").innerHTML = fields[category];
    const emailInput = document.getElementById("email");
    const emailBox = document.getElementById("email-box");

    emailBox.style.display =
        state.modo === "identificado" ? "block" : "none";

    emailInput.required = state.modo === "identificado";
    emailInput.value = "";

    document.getElementById("result").classList.add("hidden");
    document.getElementById("complaint-form").style.display = "block";
    show("form-screen");
}

document.querySelectorAll("[data-mode]").forEach(btn => {
    btn.addEventListener("click", () => {
        state.modo = btn.dataset.mode;
        if (state.modo === "anonimo") {
            show("anonymous-warning");
        } else {
            renderCategories();
            show("category-screen");
        }
    });
});

document.getElementById("confirm-anonymous").addEventListener("click", () => {
    renderCategories();
    show("category-screen");
});

document.querySelectorAll("[data-back]").forEach(btn => {
    btn.addEventListener("click", () => {
        if (document.getElementById("form-screen").classList.contains("active")) {
            renderCategories();
            show("category-screen");
        } else {
            show("welcome");
        }
    });
});

document.getElementById("complaint-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = event.currentTarget;
    const data = {};
    new FormData(form).forEach((value, key) => {
        if (key !== "email") data[key] = value;
    });

    const payload = {
        modo: state.modo,
        categoria: state.categoria,
        dados: data,
        email: document.getElementById("email").value.trim()
    };

    const submit = form.querySelector("button[type=submit]");
    const result = document.getElementById("result");
    submit.disabled = true;
    submit.textContent = "Enviando...";

    try {
        const response = await fetch("/api/denuncias", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const body = await response.json();

        if (!response.ok || !body.ok) {
            throw new Error(body.erro || "Erro ao enviar.");
        }

        form.style.display = "none";
        result.classList.remove("hidden");
        result.innerHTML = `
            <strong>Denúncia registrada com sucesso.</strong>
            <p>Guarde seu protocolo: <strong>${body.protocolo}</strong></p>
            <p>O acompanhamento deverá ser realizado pelo portal da ouvidoria.</p>
            <button class="btn primary" onclick="location.reload()">Fazer novo registro</button>
        `;
    } catch (error) {
        alert(error.message);
    } finally {
        submit.disabled = false;
        submit.textContent = "Enviar denúncia";
    }
});


// ============================================================
// ACOMPANHAMENTO POR PROTOCOLO
// ============================================================

const trackingForm = document.getElementById("tracking-form");
const trackingInput = document.getElementById("tracking-protocol");
const trackingResult = document.getElementById("tracking-result");
const trackingSubmit = trackingForm.querySelector("button[type=submit]");

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function showTrackingResult(message, isError = false) {
    trackingResult.classList.remove("hidden");
    trackingResult.classList.toggle("error", isError);
    trackingResult.innerHTML = message;
}

document.getElementById("open-tracking").addEventListener("click", () => {
    trackingInput.value = "";
    trackingResult.classList.add("hidden");
    trackingResult.classList.remove("error");
    show("tracking-screen");
});

document.getElementById("back-from-tracking").addEventListener("click", () => {
    show("welcome");
});

trackingForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const protocolo = trackingInput.value.trim();
    if (!protocolo) {
        showTrackingResult("Informe o número do protocolo.", true);
        return;
    }

    trackingSubmit.disabled = true;
    trackingSubmit.textContent = "Consultando...";
    trackingResult.classList.add("hidden");
    trackingResult.classList.remove("error");

    try {
        const response = await fetch(
            `/api/denuncias/${encodeURIComponent(protocolo)}`,
            { headers: { "Accept": "application/json" } }
        );
        const body = await response.json();

        if (!response.ok || !body.ok) {
            throw new Error(body.erro || "Protocolo não encontrado.");
        }

        const data = body.criado_em
            ? new Date(body.criado_em).toLocaleString("pt-BR")
            : "Não informado";

        showTrackingResult(`
            <strong>Denúncia localizada.</strong>
            <p><strong>Protocolo:</strong> ${escapeHtml(body.protocolo)}</p>
            <p><strong>Status:</strong> ${escapeHtml(body.status)}</p>
            <p><strong>Categoria:</strong> ${escapeHtml(body.categoria)}</p>
            <p><strong>Recebida em:</strong> ${escapeHtml(data)}</p>
            <p>O acompanhamento detalhado deverá continuar pelo portal da Ouvidoria.</p>
        `);
    } catch (error) {
        showTrackingResult(escapeHtml(error.message), true);
    } finally {
        trackingSubmit.disabled = false;
        trackingSubmit.textContent = "Consultar protocolo";
    }
});
