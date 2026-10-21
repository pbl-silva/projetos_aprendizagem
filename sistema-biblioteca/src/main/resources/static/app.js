"use strict";
const $ = (selector) => document.querySelector(selector);
const state = {
  books: [],
  readers: [],
  loans: [],
  view: "inicio",
  version: 0,
  reportVersion: 0,
  report: null,
};
const categories = {
  FICCAO: "Ficção",
  NAO_FICCAO: "Não ficção",
  TECNICO: "Técnico",
  ROMANCE: "Romance",
  SUSPENSE: "Suspense",
  BIOGRAFIA: "Biografia",
};
const colors = {
  FICCAO: "#63767f",
  NAO_FICCAO: "#8b9879",
  TECNICO: "#8a684b",
  ROMANCE: "#934f49",
  SUSPENSE: "#3b5052",
  BIOGRAFIA: "#9b885f",
};
const views = {
  inicio: ["Visão geral", "Tudo em ordem para a próxima leitura."],
  acervo: ["Acervo", "Encontre o próximo livro a sair da estante."],
  leitores: ["Leitores", "Conheça quem dá vida às histórias."],
  emprestimos: [
    "Empréstimos",
    "Da retirada à devolução, acompanhe cada leitura.",
  ],
  relatorios: ["Relatórios", "Cada leitor tem uma história para contar."],
};
const esc = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const day = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const date = (value) =>
  value ? new Date(value + "T12:00:00").toLocaleDateString("pt-BR") : "—";
const money = (value) =>
  Number(value ?? 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
const normalize = (value) =>
  String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
const late = (loan) =>
  loan.status === "ATIVO" && loan.dataDevolucaoPrevista < day();
const empty = (columns, title, detail = "") =>
  `<tr><td colspan="${columns}" class="empty"><strong>${esc(title)}</strong>${esc(detail)}</td></tr>`;
let submitAction, toastTimer;
function toast(message) {
  clearTimeout(toastTimer);
  $("#toast").textContent = message;
  $("#toast").hidden = false;
  toastTimer = setTimeout(() => ($("#toast").hidden = true), 6000);
}
function showError(error) {
  $("#error").textContent = error.message;
  $("#error").hidden = false;
}
async function api(path, { method = "GET", body, blob = false } = {}) {
  let response;
  try {
    response = await fetch(new URL("./api/" + path, document.baseURI), {
      method,
      headers: {
        Accept: blob ? "application/pdf" : "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(30000),
    });
  } catch {
    throw new Error(
      "Não foi possível conectar à biblioteca. Confira se o sistema está aberto e tente novamente.",
    );
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(
      problem.errors?.join(" · ") ||
        problem.message ||
        "Não foi possível concluir esta operação.",
    );
  }
  if (response.status === 204) return null;
  return blob ? response.blob() : response.json();
}
function setView(view) {
  state.view = view;
  document
    .querySelectorAll(".view")
    .forEach((el) => (el.hidden = el.id !== "view-" + view));
  document.querySelectorAll("[data-view]").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === view);
    if (button.dataset.view === view)
      button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  $("#page-title").textContent = views[view][0];
  $("#page-description").textContent = views[view][1];
}
async function refresh() {
  const version = ++state.version;
  $("#refresh").disabled = true;
  try {
    const [books, readers, loans] = await Promise.all([
      api("livros"),
      api("usuarios"),
      api("emprestimos"),
    ]);
    if (version !== state.version) return;
    Object.assign(state, { books, readers, loans });
    $("#error").hidden = true;
    render();
    $("#sync-time").textContent =
      "Atualizado às " +
      new Date().toLocaleTimeString("pt-BR", {
        hour: "2-digit",
        minute: "2-digit",
      });
    if ($("#report-reader").value) await loadReport();
  } catch (error) {
    showError(error);
  } finally {
    if (version === state.version) $("#refresh").disabled = false;
  }
}
function statusBadge(loan) {
  return loan.status === "DEVOLVIDO"
    ? '<span class="badge returned">Devolvido</span>'
    : late(loan)
      ? '<span class="badge late">Em atraso</span>'
      : '<span class="badge">Em andamento</span>';
}
function loanRows(loans) {
  return loans.length
    ? loans
        .map(
          (loan) =>
            `<tr><td><span class="title">${esc(loan.livro?.titulo)}</span><small class="subtext">${esc(loan.usuario?.nome)}</small></td><td>${date(loan.dataEmprestimo)}</td><td>${date(loan.dataDevolucaoPrevista)}</td><td>${statusBadge(loan)}</td><td>${loan.status === "ATIVO" ? `<button class="row-button" data-action="return" data-id="${loan.id}">Registrar devolução ↗</button>` : `<span class="subtext">${date(loan.dataDevolucaoReal)} · Multa ${money(loan.multaCalculada)}</span>`}</td></tr>`,
        )
        .join("")
    : empty(
        5,
        "Nenhum empréstimo por aqui",
        "Os próximos capítulos aparecem aqui após uma retirada.",
      );
}
function render() {
  $("#stat-books").textContent = state.books.length;
  $("#stat-available").textContent = state.books.filter(
    (b) => b.disponivel,
  ).length;
  $("#stat-active").textContent = state.loans.filter(
    (l) => l.status === "ATIVO",
  ).length;
  $("#stat-late").textContent = state.loans.filter(late).length;
  $("#recent-rows").innerHTML = loanRows(
    state.loans
      .filter((l) => l.status === "ATIVO")
      .sort((a, b) =>
        a.dataDevolucaoPrevista.localeCompare(b.dataDevolucaoPrevista),
      )
      .slice(0, 5),
  );
  renderBooks();
  renderReaders();
  renderLoans();
  const selected = $("#report-reader").value;
  $("#report-reader").innerHTML =
    '<option value="">Escolha um leitor</option>' + readerOptions();
  if (state.readers.some((r) => String(r.id) === selected))
    $("#report-reader").value = selected;
  else clearReport();
}
function renderBooks() {
  const query = normalize($("#book-search").value),
    category = $("#book-category").value;
  const books = state.books.filter(
    (b) =>
      normalize(`${b.titulo} ${b.autor} ${b.isbn}`).includes(query) &&
      (!category || b.categoria === category) &&
      (!$("#book-available").checked || b.disponivel),
  );
  $("#book-grid").innerHTML = books.length
    ? books
        .map(
          (b) =>
            `<article class="book-card"><div class="book-cover"><div class="book-object" style="--book-color:${colors[b.categoria] || "#723e3c"}"><strong>${esc(b.titulo)}</strong><small>${esc(b.autor)}</small></div></div><div class="book-info"><h2>${esc(b.titulo)}</h2><p>${esc(b.autor)} · ${esc(b.anoPublicacao || "Ano não informado")}</p><span class="badge ${b.disponivel ? "" : "returned"}">${b.disponivel ? "Disponível" : "Emprestado"} · ${esc(categories[b.categoria])}</span><div class="row-actions"><button class="row-button" data-action="loan" data-id="${b.id}" ${b.disponivel ? "" : "disabled"}>Emprestar ↗</button><button class="row-button" data-action="book" data-id="${b.id}">Editar</button><button class="row-button danger" data-action="delete-book" data-id="${b.id}" aria-label="Excluir ${esc(b.titulo)}">Excluir</button></div></div></article>`,
        )
        .join("")
    : '<p class="empty"><strong>Nenhum livro encontrado</strong>Cadastre um livro ou altere os filtros para explorar o acervo.</p>';
}
function renderReaders() {
  const query = normalize($("#reader-search").value);
  const readers = state.readers.filter((r) =>
    normalize(`${r.nome} ${r.email} ${r.cpf}`).includes(query),
  );
  $("#reader-rows").innerHTML = readers.length
    ? readers
        .map(
          (r) =>
            `<tr><td><span class="title">${esc(r.nome)}</span><small class="subtext">${esc(r.email)}</small></td><td>${esc(r.cpf)}</td><td><span class="badge">${r.tipoUsuario === "PREMIUM" ? "Premium" : "Comum"}</span></td><td>${date(r.dataCadastro)}</td><td><div class="row-actions"><button class="row-button" data-action="reader" data-id="${r.id}">Editar</button><button class="row-button" data-action="reader-report" data-id="${r.id}">Histórico</button><button class="row-button danger" data-action="delete-reader" data-id="${r.id}">Excluir</button></div></td></tr>`,
        )
        .join("")
    : empty(
        5,
        "Nenhum leitor encontrado",
        "Cadastre um leitor para começar uma nova história.",
      );
}
function renderLoans() {
  const query = normalize($("#loan-search").value),
    filter = $("#loan-filter").value;
  $("#loan-rows").innerHTML = loanRows(
    state.loans
      .filter(
        (l) =>
          normalize(`${l.livro?.titulo} ${l.usuario?.nome}`).includes(query) &&
          (!filter || (filter === "ATRASADO" ? late(l) : l.status === filter)),
      )
      .sort((a, b) => b.id - a.id),
  );
}
const categoryOptions = (selected) =>
  Object.entries(categories)
    .map(
      ([value, label]) =>
        `<option value="${value}" ${value === selected ? "selected" : ""}>${label}</option>`,
    )
    .join("");
const readerOptions = () =>
  state.readers
    .map(
      (r) =>
        `<option value="${r.id}">${esc(r.nome)} — ${r.tipoUsuario === "PREMIUM" ? "Premium" : "Comum"}</option>`,
    )
    .join("");
function openEditor(title, fields, button, action) {
  $("#editor-title").textContent = title;
  $("#editor-fields").innerHTML = fields;
  $("#editor-error").textContent = "";
  $("#editor-submit").textContent = button;
  $("#editor-submit").disabled = false;
  submitAction = action;
  $("#editor").showModal();
}
function editBook(book) {
  openEditor(
    book ? "Editar livro" : "Cadastrar livro",
    `<div class="form-grid"><label class="full">Título<input name="titulo" value="${esc(book?.titulo)}" required maxlength="200"></label><label class="full">Autor<input name="autor" value="${esc(book?.autor)}" required maxlength="255"></label><label>ISBN<input name="isbn" value="${esc(book?.isbn)}" required maxlength="20" placeholder="9780132350884"></label><label>Ano de publicação<input name="anoPublicacao" type="number" min="1" max="9999" value="${esc(book?.anoPublicacao)}"></label><label class="full">Categoria<select name="categoria">${categoryOptions(book?.categoria)}</select></label></div>`,
    "Salvar livro",
    async (form) => {
      const body = Object.fromEntries(new FormData(form));
      body.anoPublicacao = body.anoPublicacao
        ? Number(body.anoPublicacao)
        : null;
      await api("livros" + (book ? "/" + book.id : ""), {
        method: book ? "PUT" : "POST",
        body,
      });
      $("#editor").close();
      toast("Livro salvo no acervo.");
      await refresh();
    },
  );
}
function editReader(reader) {
  openEditor(
    reader ? "Editar leitor" : "Cadastrar leitor",
    `<div class="form-grid"><label class="full">Nome<input name="nome" value="${esc(reader?.nome)}" required minlength="3" maxlength="100" autocomplete="name"></label><label class="full">E-mail<input name="email" type="email" value="${esc(reader?.email)}" required maxlength="255" autocomplete="email"></label><label>CPF<input name="cpf" value="${esc(reader?.cpf)}" inputmode="numeric" pattern="[0-9]{11}" required maxlength="11" title="Informe os 11 dígitos, sem pontuação"></label><label>Categoria<select name="tipoUsuario"><option value="COMUM" ${reader?.tipoUsuario === "COMUM" ? "selected" : ""}>Comum</option><option value="PREMIUM" ${reader?.tipoUsuario === "PREMIUM" ? "selected" : ""}>Premium</option></select></label></div>`,
    "Salvar leitor",
    async (form) => {
      await api("usuarios" + (reader ? "/" + reader.id : ""), {
        method: reader ? "PUT" : "POST",
        body: Object.fromEntries(new FormData(form)),
      });
      $("#editor").close();
      toast("Dados do leitor salvos.");
      await refresh();
    },
  );
}
function newLoan(bookId) {
  const books = state.books.filter((b) => b.disponivel);
  if (!books.length || !state.readers.length) {
    toast(
      !books.length
        ? "Cadastre um livro disponível antes de emprestar."
        : "Cadastre um leitor antes de emprestar.",
    );
    return;
  }
  openEditor(
    "Novo empréstimo",
    `<p class="notice">Leitor comum: até 3 livros por 7 dias.<br>Leitor premium: até 5 livros por 14 dias.<br>A disponibilidade e o prazo serão confirmados no registro.</p><div class="form-grid"><label class="full">Livro<select name="livroId" required><option value="">Selecione um livro</option>${books.map((b) => `<option value="${b.id}" ${b.id === bookId ? "selected" : ""}>${esc(b.titulo)} — ${esc(b.autor)}</option>`).join("")}</select></label><label class="full">Leitor<select name="usuarioId" required><option value="">Selecione um leitor</option>${readerOptions()}</select></label></div>`,
    "Registrar empréstimo",
    async (form) => {
      await api("emprestimos", {
        method: "POST",
        body: {
          livroId: Number(form.elements.livroId.value),
          usuarioId: Number(form.elements.usuarioId.value),
        },
      });
      $("#editor").close();
      toast("Empréstimo registrado. Boa leitura!");
      await refresh();
    },
  );
}
function returnLoan(loan) {
  if (!loan) return;
  openEditor(
    "Registrar devolução",
    `<p class="notice"><strong>${esc(loan.livro?.titulo)}</strong><br>Leitor: ${esc(loan.usuario?.nome)}<br>Devolução prevista: ${date(loan.dataDevolucaoPrevista)}<br>Multa calculada no momento: ${money(loan.multaCalculada)}<br>O valor será confirmado pelo sistema ao registrar a devolução.</p>`,
    "Confirmar devolução",
    async () => {
      const result = await api(`emprestimos/${loan.id}/devolver`, {
        method: "PUT",
      });
      $("#editor").close();
      toast("Devolução registrada. Multa: " + money(result.multaCalculada));
      await refresh();
    },
  );
}
function confirmDelete(kind, item) {
  if (!item) return;
  openEditor(
    kind === "livros" ? "Excluir livro?" : "Excluir leitor?",
    `<p class="notice">Confirma a exclusão de <strong>${esc(item.titulo || item.nome)}</strong>? Cadastros com empréstimos vinculados podem ter a exclusão bloqueada.</p>`,
    "Excluir cadastro",
    async () => {
      await api(`${kind}/${item.id}`, { method: "DELETE" });
      $("#editor").close();
      toast("Cadastro excluído.");
      await refresh();
    },
  );
}
function clearReport() {
  state.reportVersion++;
  state.report = null;
  $("#report-content").hidden = true;
  $("#report-empty").hidden = false;
  $("#export-report").disabled = true;
}
async function loadReport() {
  const id = $("#report-reader").value;
  if (!id) {
    clearReport();
    return;
  }
  const version = ++state.reportVersion;
  state.report = null;
  $("#export-report").disabled = true;
  try {
    const [stats, loans] = await Promise.all([
      api(`relatorios/usuario/${id}/estatisticas`),
      api(`emprestimos/usuario/${id}`),
    ]);
    if (version !== state.reportVersion) return;
    state.report = id;
    $("#report-content").hidden = false;
    $("#report-empty").hidden = true;
    $("#report-stats").innerHTML = [
      ["Empréstimos realizados", stats.totalEmprestimos],
      ["Empréstimos ativos", stats.emprestimosAtivos],
      ["Devolvidos no prazo", stats.livrosNoPrazo],
      ["Multas acumuladas", money(stats.multasAcumuladas)],
    ]
      .map(
        ([label, value]) =>
          `<article><span>${label}</span><strong>${esc(value)}</strong></article>`,
      )
      .join("");
    $("#report-rows").innerHTML = loanRows(loans);
    $("#export-report").disabled = false;
    $("#error").hidden = true;
  } catch (error) {
    if (version === state.reportVersion) {
      $("#report-content").hidden = true;
      $("#report-empty").hidden = false;
      showError(error);
    }
  }
}
$("#editor-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  if (form.dataset.saving === "true") return;
  form.dataset.saving = "true";
  form.inert = true;
  $("#editor-submit").disabled = true;
  $("#editor-error").textContent = "";
  try {
    await submitAction(form);
  } catch (error) {
    $("#editor-error").textContent = error.message;
  } finally {
    form.dataset.saving = "false";
    form.inert = false;
    $("#editor-submit").disabled = false;
  }
});
document.querySelectorAll("[data-close]").forEach((button) =>
  button.addEventListener("click", () => {
    if ($("#editor-form").dataset.saving !== "true") $("#editor").close();
  }),
);
$("#editor").addEventListener("cancel", (event) => {
  if ($("#editor-form").dataset.saving === "true") event.preventDefault();
});
document
  .querySelectorAll("[data-view]")
  .forEach((button) =>
    button.addEventListener("click", () => setView(button.dataset.view)),
  );
document.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action]");
  if (!button || button.disabled) return;
  const id = Number(button.dataset.id);
  switch (button.dataset.action) {
    case "catalog":
      setView("acervo");
      break;
    case "all-loans":
      setView("emprestimos");
      break;
    case "book":
      editBook(state.books.find((b) => b.id === id));
      break;
    case "reader":
      editReader(state.readers.find((r) => r.id === id));
      break;
    case "loan":
      newLoan(id);
      break;
    case "return":
      returnLoan(state.loans.find((l) => l.id === id));
      break;
    case "delete-book":
      confirmDelete(
        "livros",
        state.books.find((b) => b.id === id),
      );
      break;
    case "delete-reader":
      confirmDelete(
        "usuarios",
        state.readers.find((r) => r.id === id),
      );
      break;
    case "reader-report":
      setView("relatorios");
      $("#report-reader").value = String(id);
      loadReport();
      break;
  }
});
$("#refresh").addEventListener("click", refresh);
$("#book-search").addEventListener("input", renderBooks);
$("#book-category").addEventListener("change", renderBooks);
$("#book-available").addEventListener("change", renderBooks);
$("#reader-search").addEventListener("input", renderReaders);
$("#loan-search").addEventListener("input", renderLoans);
$("#loan-filter").addEventListener("change", renderLoans);
$("#report-reader").addEventListener("change", loadReport);
$("#export-report").addEventListener("click", async (event) => {
  const button = event.currentTarget,
    id = state.report;
  if (!id) return;
  button.disabled = true;
  try {
    const pdf = await api(`relatorios/usuario/${id}/emprestimos`, {
      blob: true,
    });
    const url = URL.createObjectURL(pdf),
      link = document.createElement("a");
    link.href = url;
    link.download = `emprestimos-leitor-${id}.pdf`;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    toast("Relatório exportado.");
  } catch (error) {
    showError(error);
  } finally {
    button.disabled = !state.report;
  }
});
$("#book-category").insertAdjacentHTML("beforeend", categoryOptions());
$("#today").textContent = new Date().toLocaleDateString("pt-BR", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});
$("#recent-rows").innerHTML = empty(5, "Carregando a biblioteca…");
refresh();
