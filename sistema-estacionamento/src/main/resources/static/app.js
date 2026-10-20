"use strict";

const $ = (selector, root = document) => root.querySelector(selector);
const state = {
  token: "",
  username: "",
  entries: [],
  vehicles: [],
  spots: [],
  view: "patio",
  role: "",
  profile: null,
  report: null,
  period: null,
  reportVersion: 0,
  epoch: 0,
  loading: false,
};
const labels = {
  CARRO: "Carro",
  MOTO: "Moto",
  CARRO_ELETRICO: "Carro elétrico",
  COMUM: "Comum",
  PCD: "PCD",
  ELETRICA: "Elétrica",
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Cartão de crédito",
  CARTAO_DEBITO: "Cartão de débito",
  PIX: "PIX",
  DIARIA: "Padrão",
  MENSAL: "Mensal",
};
const views = {
  patio: ["Visão geral", "Acompanhe o pátio e cuide de cada chegada."],
  veiculos: ["Veículos", "Um cadastro organizado para cada nova visita."],
  vagas: ["Mapa de vagas", "Veja a disponibilidade e organize seu espaço."],
  relatorios: ["Relatórios", "O movimento da operação, com todos os detalhes."],
  usuarios: ["Equipe", "As pessoas que cuidam da sua operação."],
  conta: ["Minha conta", "Seus dados de acesso e contato, sempre atualizados."],
};
const money = (value) =>
  Number(value ?? 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ],
  );
const dateTime = (value) =>
  value
    ? new Date(value).toLocaleString("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
      })
    : "—";
const localDay = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
const normalizePlate = (value) => value.trim().toUpperCase().replace(/-/g, "");
const emptyRow = (columns, title, detail = "") =>
  `<tr><td colspan="${columns}" class="empty"><strong>${escapeHtml(title)}</strong>${escapeHtml(detail)}</td></tr>`;
let editorHandler = null;
let toastTimer;

function toast(message) {
  clearTimeout(toastTimer);
  $("#toast").textContent = message;
  $("#toast").hidden = false;
  toastTimer = setTimeout(() => {
    $("#toast").hidden = true;
  }, 6000);
}

function resetSession(message = "") {
  state.epoch++;
  state.reportVersion++;
  state.token = "";
  state.username = "";
  state.role = "";
  state.profile = null;
  $("#account-form").reset();
  $("#password-form").reset();
  $("#account-data").disabled = true;
  $("#account-password").disabled = true;
  $("#manage-users").hidden = true;
  $("#user-rows").innerHTML = "";
  state.entries = [];
  state.vehicles = [];
  state.spots = [];
  state.report = null;
  state.period = null;
  state.loading = false;
  try {
    sessionStorage.removeItem("estacionamento.session");
  } catch {
    /* A sessão também funciona apenas em memória. */
  }
  document.querySelectorAll("dialog[open]").forEach((dialog) => dialog.close());
  $("#app").hidden = true;
  $("#login-screen").hidden = false;
  $("#login-error").textContent = message;
  $("#login-form").elements.senha.value = "";
  hidePassword();
  $("#report-rows").innerHTML = emptyRow(6, "Consulte um período");
  $("#report-revenue").textContent = "—";
  $("#report-prev").disabled = true;
  $("#report-next").disabled = true;
  $("#report-page").textContent = "Consulte um período para começar.";
}

async function api(path, { method = "GET", body, blob = false } = {}) {
  const epoch = state.epoch;
  const headers = { Accept: blob ? "application/pdf" : "application/json" };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let response;
  try {
    response = await fetch(new URL(`.${path}`, document.baseURI), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(30000),
    });
  } catch {
    throw new Error(
      "Não foi possível conectar. Confira se o estacionamento está em execução e tente novamente.",
    );
  }
  if (epoch !== state.epoch)
    throw new Error("A sessão foi encerrada. Entre novamente.");
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    if (response.status === 401 && path !== "/auth/login")
      resetSession("Sua sessão expirou. Entre novamente para continuar.");
    const error = new Error(
      problem.campos
        ? Object.values(problem.campos).join(" · ")
        : problem.mensagem || (response.status === 403 ? "Você não tem permissão para realizar esta operação." : "") ||
            "Não foi possível concluir a operação. Tente novamente.",
    );
    error.status = response.status;
    throw error;
  }
  if (response.status === 204) return null;
  return blob ? response.blob() : response.json();
}

async function enterApp(login) {
  state.epoch++;
  state.token = login.token;
  state.username = login.username;
  const currentUser = await api("/auth/me");
  state.role = currentUser.role;
  state.profile = currentUser;
  state.username = currentUser.username;
  $("#manage-users").hidden = state.role !== "ADMIN";
  try {
    sessionStorage.setItem(
      "estacionamento.session",
      JSON.stringify({ token: login.token, username: login.username }),
    );
  } catch {
    /* Armazenamento opcional. */
  }
  updateAccountHeader();
  $("#login-form").elements.senha.value = "";
  $("#login-screen").hidden = true;
  hidePassword();
  $("#app").hidden = false;
  $("#load-error").hidden = true;
  ["stat-free", "stat-active", "stat-occupancy", "stat-revenue"].forEach(
    (id) => {
      $(`#${id}`).textContent = "—";
    },
  );
  $("#patio-rows").innerHTML = emptyRow(5, "Carregando o pátio…");
  $("#vehicle-rows").innerHTML = emptyRow(5, "Carregando os veículos…");
  setView("patio");
  await refresh();
}

function setView(view) {
  if (view === "usuarios" && state.role !== "ADMIN") return;
  state.view = view;
  document.querySelectorAll(".view").forEach((section) => {
    section.hidden = section.id !== `view-${view}`;
  });
  document.querySelectorAll("[data-view]").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === view);
    if (button.dataset.view === view)
      button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  $("#page-title").textContent = views[view][0];
  $("#page-description").textContent = views[view][1];
  if (view === "relatorios" && !state.report)
    loadReport(0).catch(showLoadError);
  if (view === "usuarios") loadUsers().catch(showLoadError);
  if (view === "conta") loadAccount().catch(showLoadError);
}

function showLoadError(error) {
  if (!state.token) return;
  $("#load-error").textContent = error.message;
  $("#load-error").hidden = false;
}

async function refresh() {
  if (state.loading || !state.token) return;
  state.loading = true;
  const epoch = state.epoch;
  $("#refresh").disabled = true;
  $("#refresh").textContent = "Atualizando…";
  try {
    const day = localDay();
    const [entries, vehicles, spots, occupancy, revenue] = await Promise.all([
      api("/entradas/ativas"),
      api("/veiculos"),
      api("/vagas"),
      api("/relatorios/ocupacao"),
      api(
        `/relatorios/faturamento?${new URLSearchParams({ inicio: `${day}T00:00:00`, fim: `${day}T23:59:59.999999999` })}`,
      ),
    ]);
    if (epoch !== state.epoch) return;
    Object.assign(state, { entries, vehicles, spots });
    $("#stat-free").textContent = occupancy.vagasDisponiveis;
    $("#stat-active").textContent = occupancy.veiculosEstacionados;
    $("#stat-occupancy").textContent = occupancy.percentualOcupacao.replace(
      ".",
      ",",
    );
    $("#occupancy-bar").style.width =
      `${Math.min(100, Math.max(0, parseFloat(occupancy.percentualOcupacao)))}%`;
    $("#stat-revenue").textContent = money(revenue.faturamento);
    $("#sync-time").textContent =
      `Atualizado às ${new Date().toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}`;
    $("#active-count").textContent = entries.length;
    $("#load-error").hidden = true;
    renderEntries();
    renderVehicles();
    renderSpots();
    renderMini();
    if (state.view === "relatorios")
      await loadReport(state.report?.paginaAtual ?? 0, state.period);
    if (state.view === "usuarios") await loadUsers();
  } catch (error) {
    showLoadError(error);
  } finally {
    if (epoch === state.epoch) state.loading = false;
    $("#refresh").disabled = false;
    $("#refresh").textContent = "↻ Atualizar";
  }
}

function vehicleFor(entry) {
  return state.vehicles.find((vehicle) => vehicle.id === entry.veiculoId);
}
function matches(vehicle, query, plate = "") {
  return `${normalizePlate(plate || vehicle?.placa || "")} ${vehicle?.marca || ""} ${vehicle?.modelo || ""}`
    .toLocaleLowerCase("pt-BR")
    .includes(query.trim().toLocaleLowerCase("pt-BR").replace(/-/g, ""));
}
function vehicleCell(vehicle, plate) {
  return `<strong class="plate">${escapeHtml(plate || vehicle?.placa)}</strong><small class="subtext">${escapeHtml(vehicle ? `${vehicle.marca} ${vehicle.modelo}` : "Veículo cadastrado")}</small>`;
}
function duration(value) {
  const minutes = Math.max(
    0,
    Math.floor((Date.now() - new Date(value).getTime()) / 60000),
  );
  if (minutes < 60) return `${minutes} min`;
  return `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
}
function renderEntries() {
  const entries = state.entries
    .filter((entry) =>
      matches(vehicleFor(entry), $("#patio-search").value, entry.placaVeiculo),
    )
    .sort((a, b) => b.id - a.id);
  $("#patio-rows").innerHTML = entries.length
    ? entries
        .map(
          (entry) =>
            `<tr><td>${vehicleCell(vehicleFor(entry), entry.placaVeiculo)}</td><td><span class="badge">${escapeHtml(entry.numeroVaga)}</span></td><td>${escapeHtml(dateTime(entry.dataHoraEntrada))}</td><td>${escapeHtml(duration(entry.dataHoraEntrada))}</td><td><div class="row-actions"><button class="row-action" data-action="saida" data-id="${entry.id}">Registrar saída →</button><button class="row-action danger" data-action="cancelar" data-id="${entry.id}" aria-label="Cancelar entrada ${escapeHtml(entry.placaVeiculo)}">Cancelar</button></div></td></tr>`,
        )
        .join("")
    : emptyRow(
        5,
        state.entries.length
          ? "Nenhum veículo encontrado"
          : "O pátio está livre",
        state.entries.length
          ? "Tente outra placa, marca ou modelo."
          : "Registre uma nova entrada para começar.",
      );
}
function renderVehicles() {
  const vehicles = state.vehicles.filter((vehicle) =>
    matches(vehicle, $("#vehicle-search").value),
  );
  $("#vehicle-rows").innerHTML = vehicles.length
    ? vehicles
        .map(
          (vehicle) =>
            `<tr><td>${vehicleCell(vehicle)}</td><td>${escapeHtml(labels[vehicle.tipoVeiculo])}</td><td>${escapeHtml(vehicle.cor || "—")}</td><td>${vehicle.pcd ? "Sim" : "Não"}</td><td><div class="row-actions"><button class="row-action" data-action="editar-veiculo" data-id="${vehicle.id}">Editar</button><button class="row-action danger" data-action="excluir-veiculo" data-id="${vehicle.id}">Excluir</button></div></td></tr>`,
        )
        .join("")
    : emptyRow(
        5,
        "Nenhum veículo encontrado",
        "Cadastre um veículo ou altere a busca.",
      );
}
function spotStatus(spot) {
  return state.entries.some((entry) => entry.vagaId === spot.id)
    ? "occupied"
    : spot.disponivel
      ? "free"
      : "maintenance";
}
const CAR_SVG =
  '<svg class="car" viewBox="0 0 54 100" aria-hidden="true"><rect x="2" y="16" width="8" height="18" rx="3" fill="#111"/><rect x="44" y="16" width="8" height="18" rx="3" fill="#111"/><rect x="2" y="66" width="8" height="18" rx="3" fill="#111"/><rect x="44" y="66" width="8" height="18" rx="3" fill="#111"/><rect x="6" y="3" width="42" height="94" rx="17" fill="currentColor"/><path d="M12 30q15-6 30 0l-3 14H15z" fill="#10161d" opacity=".85"/><rect x="14" y="50" width="26" height="22" rx="5" fill="#fff" opacity=".22"/><path d="M15 76h24l3 9q-15 5-30 0z" fill="#10161d" opacity=".85"/><rect x="11" y="6" width="9" height="4" rx="2" fill="#fff8c9"/><rect x="34" y="6" width="9" height="4" rx="2" fill="#fff8c9"/><rect x="11" y="90" width="9" height="3" rx="1.5" fill="#000" opacity=".35"/><rect x="34" y="90" width="9" height="3" rx="1.5" fill="#000" opacity=".35"/></svg>';
const MOTO_SVG =
  '<svg class="car" viewBox="0 0 34 100" aria-hidden="true"><rect x="13" y="3" width="8" height="24" rx="4" fill="#111"/><rect x="13" y="73" width="8" height="24" rx="4" fill="#111"/><rect x="2" y="24" width="30" height="5" rx="2.5" fill="#111"/><rect x="9" y="30" width="16" height="42" rx="8" fill="currentColor"/><rect x="12" y="40" width="10" height="14" rx="4" fill="#10161d" opacity=".8"/><circle cx="17" cy="62" r="4" fill="#fff" opacity=".25"/></svg>';
const spotTag = { PCD: "♿", ELETRICA: "⚡" };
function renderSpots() {
  const filter = $("#spot-filter").value;
  $("#spot-grid").innerHTML =
    state.spots
      .filter((spot) => !filter || spot.tipoVaga === filter)
      .map((spot) => {
        const status = spotStatus(spot);
        const name =
          status === "occupied"
            ? "Ocupada"
            : status === "free"
              ? "Disponível"
              : "Indisponível";
        const entry = state.entries.find((item) => item.vagaId === spot.id);
        const caption = entry
          ? `<span class="plate">${escapeHtml(entry.placaVeiculo)}</span>`
          : `<small>${name}</small>`;
        const tag = spotTag[spot.tipoVaga]
          ? `<span class="tag" aria-hidden="true">${spotTag[spot.tipoVaga]}</span>`
          : "";
        return `<button class="spot ${status}" data-type="${spot.tipoVaga}" data-action="editar-vaga" data-id="${spot.id}" title="${escapeHtml(labels[spot.tipoVaga])} · ${name}" aria-label="Vaga ${escapeHtml(spot.numero)}, ${escapeHtml(labels[spot.tipoVaga])}, ${name}">${tag}${spot.tipoVaga === "MOTO" ? MOTO_SVG : CAR_SVG}<strong>${escapeHtml(spot.numero)}</strong>${caption}</button>`;
      })
      .join("") || '<p class="empty">Nenhuma vaga deste tipo.</p>';
}
function renderMini() {
  $("#mini-lot").innerHTML = ["COMUM", "PCD", "MOTO", "ELETRICA"]
    .map((type) => {
      const spots = state.spots.filter((spot) => spot.tipoVaga === type);
      if (!spots.length) return "";
      const free = spots.filter((spot) => spotStatus(spot) === "free").length;
      return `<div class="lane"><div><b>${labels[type]}</b><small>${free} de ${spots.length} livres</small></div><div class="lane-cars">${spots.map((spot) => `<i class="mini ${spotStatus(spot)}" title="${escapeHtml(spot.numero)}">${type === "MOTO" ? MOTO_SVG : CAR_SVG}</i>`).join("")}</div></div>`;
    })
    .join("");
}

function openEditor(title, fields, submit, handler) {
  $("#editor-title").textContent = title;
  $("#editor-fields").innerHTML = fields;
  $("#editor-error").textContent = "";
  $("#editor-submit").textContent = submit;
  $("#editor-submit").disabled = false;
  editorHandler = handler;
  $("#editor").showModal();
}
const options = (values, selected) =>
  values
    .map(
      (value) =>
        `<option value="${value}" ${value === selected ? "selected" : ""}>${labels[value]}</option>`,
    )
    .join("");
function vehicleFields(vehicle = {}) {
  return `<div class="form-grid"><label>Placa<input name="placa" value="${escapeHtml(vehicle.placa)}" ${vehicle.id ? "readonly" : ""} required pattern="[A-Za-z]{3}-?[0-9][A-Za-z0-9][0-9]{2}" maxlength="8" placeholder="ABC1D23" title="Ex.: ABC1234 ou ABC1D23" style="text-transform:uppercase"></label><label>Tipo<select name="tipoVeiculo">${options(["CARRO", "MOTO", "CARRO_ELETRICO"], vehicle.tipoVeiculo || "CARRO")}</select></label><label>Marca<input name="marca" value="${escapeHtml(vehicle.marca)}" required maxlength="50" placeholder="Ex.: Volkswagen"></label><label>Modelo<input name="modelo" value="${escapeHtml(vehicle.modelo)}" required maxlength="50" placeholder="Ex.: Polo"></label><label class="full">Cor<input name="cor" value="${escapeHtml(vehicle.cor)}" maxlength="30" placeholder="Ex.: Prata"></label><label class="checkbox full"><input name="pcd" type="checkbox" ${vehicle.pcd ? "checked" : ""}> Veículo de pessoa com deficiência (somente carro)</label></div>`;
}
function vehiclePayload(form) {
  const data = new FormData(form);
  return {
    placa: normalizePlate(data.get("placa")),
    tipoVeiculo: data.get("tipoVeiculo"),
    marca: data.get("marca").trim(),
    modelo: data.get("modelo").trim(),
    cor: data.get("cor").trim(),
    pcd: data.has("pcd"),
  };
}
function entryEditor() {
  const availableVehicles = state.vehicles.filter(
    (vehicle) => !state.entries.some((entry) => entry.veiculoId === vehicle.id),
  );
  openEditor(
    "Registrar nova entrada",
    `<p class="notice">A vaga será selecionada automaticamente de acordo com o tipo do veículo e a disponibilidade.</p><label style="margin-bottom:20px">Veículo<select name="veiculoId" id="entry-vehicle"><option value="">Cadastrar um novo veículo</option>${availableVehicles.map((vehicle) => `<option value="${vehicle.id}">${escapeHtml(vehicle.placa)} — ${escapeHtml(vehicle.marca)} ${escapeHtml(vehicle.modelo)}</option>`).join("")}</select></label><fieldset id="new-vehicle-fields" style="border:0;padding:0;margin:0">${vehicleFields()}</fieldset>`,
    "Registrar entrada",
    async (form) => {
      const selected = form.elements.veiculoId.value;
      let vehicle;
      if (selected) vehicle = { id: Number(selected) };
      else {
        const payload = vehiclePayload(form);
        // Buscar pela placa evita duplicar o cadastro em uma nova tentativa após falta de vaga.
        try {
          vehicle = await api(
            `/veiculos/placa/${encodeURIComponent(payload.placa)}`,
          );
        } catch (error) {
          if (error.status !== 404) throw error;
          vehicle = await api("/veiculos", { method: "POST", body: payload });
        }
      }
      const entry = await api("/entradas", {
        method: "POST",
        body: { veiculoId: vehicle.id },
      });
      $("#editor").close();
      toast(
        `Entrada registrada: ${entry.placaVeiculo} · vaga ${entry.numeroVaga}.`,
      );
      await refresh();
    },
  );
  $("#entry-vehicle").addEventListener("change", (event) => {
    $("#new-vehicle-fields").disabled = Boolean(event.target.value);
    $("#new-vehicle-fields").hidden = Boolean(event.target.value);
  });
}
function vehicleEditor(vehicle) {
  openEditor(
    vehicle ? "Editar veículo" : "Cadastrar veículo",
    vehicleFields(vehicle),
    "Salvar veículo",
    async (form) => {
      await api(vehicle ? `/veiculos/${vehicle.id}` : "/veiculos", {
        method: vehicle ? "PUT" : "POST",
        body: vehiclePayload(form),
      });
      $("#editor").close();
      toast("Dados do veículo salvos.");
      await refresh();
    },
  );
}
function exitEditor(entry) {
  if (!entry) return;
  openEditor(
    "Registrar saída",
    `<div class="notice"><strong>${escapeHtml(entry.placaVeiculo)}</strong> · vaga ${escapeHtml(entry.numeroVaga)}<br>Entrada: ${escapeHtml(dateTime(entry.dataHoraEntrada))}<br>Confira o valor e o pagamento antes de liberar o veículo.</div><div class="form-grid"><label>Modalidade<select name="modalidade">${options(["DIARIA", "MENSAL"], "DIARIA")}</select></label><label>Forma de pagamento<select name="tipoPagamento">${options(["PIX", "DINHEIRO", "CARTAO_CREDITO", "CARTAO_DEBITO"], "PIX")}</select></label></div><div class="receipt-total" id="exit-preview" role="status">Calculando prévia…</div><p class="muted" style="margin-top:12px">O valor definitivo é recalculado ao confirmar, conforme o tempo de permanência.</p>`,
    "Confirmar saída e emitir recibo",
    async (form) => {
      const receipt = await api("/saidas", {
        method: "POST",
        body: {
          entradaId: entry.id,
          modalidade: form.elements.modalidade.value,
          tipoPagamento: form.elements.tipoPagamento.value,
        },
      });
      $("#editor").close();
      showReceipt(receipt);
      toast("Saída registrada e vaga liberada.");
      await refresh();
    },
  );
  let previewVersion = 0;
  const previewElement = $("#exit-preview");
  const updatePreview = async () => {
    const version = ++previewVersion;
    $("#editor-submit").disabled = true;
    $("#editor-error").textContent = "";
    previewElement.textContent = "Calculando prévia…";
    try {
      const preview = await api(
        `/saidas/previa/${entry.id}?modalidade=${$("#editor-form").elements.modalidade.value}`,
      );
      if (version !== previewVersion || !previewElement.isConnected) return;
      previewElement.innerHTML = `<span>Total previsto<small class="subtext">Desconto: ${money(preview.desconto)}</small></span><strong>${money(preview.valorFinal)}</strong>`;
      $("#editor-submit").disabled = false;
    } catch (error) {
      if (version !== previewVersion || !previewElement.isConnected) return;
      previewElement.textContent = "Prévia indisponível";
      $("#editor-error").textContent =
        `${error.message} Feche a janela e tente novamente.`;
    }
  };
  $("#editor-form").elements.modalidade.addEventListener(
    "change",
    updatePreview,
  );
  updatePreview();
}
function spotEditor(spot) {
  if (!spot) return;
  const occupied = spotStatus(spot) === "occupied";
  openEditor(
    `Vaga ${spot.numero}`,
    `${occupied ? '<p class="notice">Esta vaga está ocupada. Registre a saída do veículo para liberá-la ou mudar sua classificação.</p>' : ""}<div class="form-grid"><label>Tipo de vaga<select name="tipoVaga" ${occupied ? "disabled" : ""}>${options(["COMUM", "PCD", "MOTO", "ELETRICA"], spot.tipoVaga)}</select></label><label>Disponibilidade<select name="disponivel" ${occupied ? "disabled" : ""}><option value="true" ${spot.disponivel ? "selected" : ""}>Disponível</option><option value="false" ${!spot.disponivel ? "selected" : ""}>Indisponível</option></select></label></div>`,
    "Salvar vaga",
    async (form) => {
      if (occupied) return;
      await api(`/vagas/${spot.id}`, {
        method: "PUT",
        body: {
          tipoVaga: form.elements.tipoVaga.value,
          disponivel: form.elements.disponivel.value === "true",
        },
      });
      $("#editor").close();
      toast("Vaga atualizada.");
      await refresh();
    },
  );
  $("#editor-submit").disabled = occupied;
}
function confirmEditor(title, message, action, handler) {
  openEditor(
    title,
    `<p class="notice">${escapeHtml(message)}</p>`,
    action,
    async () => {
      await handler();
      $("#editor").close();
      toast("Operação concluída.");
      await refresh();
    },
  );
}
function showReceipt(receipt) {
  const lines = [
    ["Recibo", receipt.numeroRecibo],
    ["Placa", receipt.placa],
    ["Veículo", `${receipt.marca} ${receipt.modelo}`],
    ["Vaga", receipt.numeroVaga],
    ["Entrada", dateTime(receipt.dataHoraEntrada)],
    ["Saída", dateTime(receipt.dataHoraSaida)],
    ["Permanência", `${receipt.tempoEstacionadoMinutos} min`],
    ["Modalidade", receipt.modalidade],
    ["Pagamento", receipt.tipoPagamento],
    ["Valor base", money(receipt.valorBase)],
    ["Desconto", money(receipt.desconto)],
  ];
  $("#receipt-content").innerHTML =
    `<dl class="receipt-list">${lines.map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${escapeHtml(value)}</dd></div>`).join("")}</dl><div class="receipt-total"><span>Total pago</span><strong>${money(receipt.valorFinal)}</strong></div>`;
  $("#receipt-dialog").showModal();
}

function selectedPeriod() {
  const start = $("#report-start").value;
  const end = $("#report-end").value;
  if (!start || !end || start > end)
    throw new Error(
      "Informe um período válido. A data inicial deve ser anterior ou igual à final.",
    );
  return { inicio: `${start}T00:00:00`, fim: `${end}T23:59:59.999999999` };
}
async function loadReport(page = 0, period = null) {
  period = period || selectedPeriod();
  const version = ++state.reportVersion;
  $("#report-prev").disabled = true;
  $("#report-next").disabled = true;
  try {
    const query = new URLSearchParams({
      ...period,
      page: String(page),
      size: "10",
      sort: "dataHoraSaida,desc",
    });
    const [report, revenue] = await Promise.all([
      api(`/relatorios/saidas?${query}`),
      api(`/relatorios/faturamento?${new URLSearchParams(period)}`),
    ]);
    if (version !== state.reportVersion || !state.token) return;
    if (page > 0 && report.conteudo.length === 0)
      return loadReport(page - 1, period);
    state.report = report;
    state.period = period;
    $("#report-revenue").textContent = money(revenue.faturamento);
    $("#report-rows").innerHTML = report.conteudo.length
      ? report.conteudo
          .map(
            (receipt) =>
              `<tr><td><strong class="plate">${escapeHtml(receipt.placa)}</strong><small class="subtext">${escapeHtml(receipt.numeroRecibo)}</small></td><td>${escapeHtml(dateTime(receipt.dataHoraEntrada))}</td><td>${escapeHtml(dateTime(receipt.dataHoraSaida))}</td><td>${escapeHtml(receipt.tipoPagamento)}</td><td>${money(receipt.valorFinal)}</td><td><button class="row-action" data-action="recibo" data-id="${receipt.id}">Ver recibo</button></td></tr>`,
          )
          .join("")
      : emptyRow(
          6,
          "Nenhuma saída neste período",
          "Escolha outras datas para consultar o histórico.",
        );
    $("#report-page").textContent =
      `${report.totalElementos} saída(s) · Página ${report.totalPaginas ? report.paginaAtual + 1 : 0} de ${report.totalPaginas}`;
    $("#report-prev").disabled = report.paginaAtual === 0;
    $("#report-next").disabled = report.ultimaPagina;
    $("#load-error").hidden = true;
  } catch (error) {
    if (version === state.reportVersion && state.report) {
      $("#report-prev").disabled = state.report.paginaAtual === 0;
      $("#report-next").disabled = state.report.ultimaPagina;
    }
    throw error;
  }
}

$("#login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = $("#login-submit");
  if (button.disabled) return;
  button.disabled = true;
  $("#login-error").textContent = "";
  const credentials = {
    username: form.elements.username.value.trim(),
    senha: form.elements.senha.value,
  };
  try {
    button.textContent = "Entrando…";
    form.setAttribute("aria-busy", "true");
    await enterApp(await api("/auth/login", { method: "POST", body: credentials }));
  } catch (error) {
    $("#login-error").textContent = error.message;
  } finally {
    button.disabled = false;
    button.innerHTML = '<span>Entrar</span><span aria-hidden="true">→</span>';
    form.removeAttribute("aria-busy");
  }
});
function hidePassword() {
  $("#login-password").type = "password";
  $("#password-toggle").textContent = "Mostrar";
  $("#password-toggle").setAttribute("aria-label", "Mostrar senha");
  $("#password-toggle").setAttribute("aria-pressed", "false");
  $("#caps-lock").hidden = true;
}
$("#password-toggle").addEventListener("click", () => {
  const show = $("#login-password").type === "password";
  $("#login-password").type = show ? "text" : "password";
  $("#password-toggle").textContent = show ? "Ocultar" : "Mostrar";
  $("#password-toggle").setAttribute("aria-label", show ? "Ocultar senha" : "Mostrar senha");
  $("#password-toggle").setAttribute("aria-pressed", String(show));
});
for (const eventName of ["keydown", "keyup"]) {
  $("#login-password").addEventListener(eventName, event => {
    $("#caps-lock").hidden = !event.getModifierState("CapsLock");
  });
}
$("#login-password").addEventListener("blur", () => { $("#caps-lock").hidden = true; });

function updateAccountHeader() {
  const name = state.profile?.nome || state.username;
  $("#user-name").textContent = name;
  $("#user-initial").textContent = name.slice(0, 1).toUpperCase();
  $("#account-initial").textContent = name.slice(0, 1).toUpperCase();
  $("#account-name").textContent = name;
  $("#account-role").textContent = state.role === "ADMIN" ? "Administrador" : "Funcionário";
  $("#account-created").textContent = dateTime(state.profile?.dataCriacao);
}
function renderAccount() {
  updateAccountHeader();
  const form = $("#account-form");
  for (const field of ["username", "nome", "email", "telefone"]) {
    form.elements[field].value = state.profile?.[field] || "";
  }
  form.elements.senhaAtual.value = "";
  $("#password-form").reset();
  $("#account-error").textContent = "";
  $("#password-error").textContent = "";
  $("#account-data").disabled = false;
  $("#account-password").disabled = false;
}
async function loadAccount() {
  const epoch = state.epoch;
  $("#account-data").disabled = true;
  $("#account-password").disabled = true;
  const profile = await api("/auth/me");
  if (epoch !== state.epoch) return;
  state.profile = profile;
  renderAccount();
  $("#load-error").hidden = true;
}
function applyAccountSession(result) {
  state.epoch++;
  state.loading = false;
  state.token = result.token;
  state.username = result.usuario.username;
  state.role = result.usuario.role;
  state.profile = result.usuario;
  try {
    sessionStorage.setItem("estacionamento.session", JSON.stringify({ token: state.token, username: state.username }));
  } catch { /* A sessão permanece em memória quando o armazenamento não está disponível. */ }
  renderAccount();
}
$("#account-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  if (form.dataset.saving === "true") return;
  const body = Object.fromEntries(new FormData(form));
  form.dataset.saving = "true";
  form.inert = true;
  $("#account-error").textContent = "";
  try {
    applyAccountSession(await api("/auth/me", { method: "PUT", body }));
    toast("Seus dados foram atualizados.");
  } catch (error) { $("#account-error").textContent = error.message; }
  finally { form.dataset.saving = "false"; form.inert = false; }
});
$("#password-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  if (form.dataset.saving === "true") return;
  $("#password-error").textContent = "";
  if (form.elements.novaSenha.value !== form.elements.confirmacaoSenha.value) {
    $("#password-error").textContent = "A confirmação da nova senha não confere.";
    return;
  }
  const body = Object.fromEntries(new FormData(form));
  form.dataset.saving = "true"; form.inert = true;
  try {
    applyAccountSession(await api("/auth/me/senha", { method: "PUT", body }));
    toast("Senha atualizada. Os outros acessos anteriores foram encerrados.");
  } catch (error) { $("#password-error").textContent = error.message; }
  finally { form.dataset.saving = "false"; form.inert = false; }
});

async function loadUsers() {
  const epoch = state.epoch;
  $("#user-rows").innerHTML = emptyRow(3, "Carregando a equipe…");
  const users = await api("/usuarios");
  if (epoch !== state.epoch || state.role !== "ADMIN") return;
  $("#user-rows").innerHTML = users.map(user => `<tr><td><strong>${escapeHtml(user.username)}</strong>${user.username === state.username ? '<small class="account-profile">Sua conta</small>' : ''}</td><td><span class="badge">${user.role === 'ADMIN' ? 'Administrador' : 'Funcionário'}</span></td><td>${escapeHtml(dateTime(user.dataCriacao))}</td></tr>`).join('') || emptyRow(3, "Nenhuma conta cadastrada");
  $("#load-error").hidden = true;
}
function userEditor() {
  if (state.role !== "ADMIN") return;
  openEditor("Criar conta da equipe", `<p class="notice">Funcionários acessam a operação. Administradores também podem criar contas para a equipe.</p><div class="form-grid"><label>Usuário<input name="username" required minlength="3" maxlength="50" autocomplete="off" autocapitalize="none" spellcheck="false" placeholder="Ex.: ana.silva"></label><label>Perfil<select name="role"><option value="OPERADOR">Funcionário</option><option value="ADMIN">Administrador</option></select></label><label class="full">Senha inicial<input name="senha" type="password" required minlength="6" maxlength="72" autocomplete="new-password" placeholder="Pelo menos 6 caracteres"></label></div>`, "Criar conta", async form => {
    await api("/auth/registrar", { method: "POST", body: { username: form.elements.username.value.trim(), senha: form.elements.senha.value, role: form.elements.role.value } });
    form.elements.senha.value = "";
    $("#editor").close();
    toast("Conta criada. Informe o usuário e a senha à pessoa da equipe.");
    await loadUsers();
  });
}
$("#logout").addEventListener("click", () => resetSession());
$("#refresh").addEventListener("click", refresh);
$("#patio-search").addEventListener("input", renderEntries);
$("#vehicle-search").addEventListener("input", renderVehicles);
$("#spot-filter").addEventListener("change", renderSpots);
$("#print-receipt").addEventListener("click", () => window.print());
document
  .querySelectorAll("[data-view]")
  .forEach((button) =>
    button.addEventListener("click", () => setView(button.dataset.view)),
  );
document.querySelectorAll("[data-close]").forEach((button) =>
  button.addEventListener("click", () => {
    if (
      button.closest("dialog") === $("#editor") &&
      $("#editor-submit").disabled &&
      $("#editor-form").dataset.saving === "true"
    )
      return;
    button.closest("dialog").close();
  }),
);
$("#editor").addEventListener("cancel", (event) => {
  if ($("#editor-form").dataset.saving === "true") event.preventDefault();
});
$("#editor-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = $("#editor-submit");
  if (button.disabled || !editorHandler) return;
  const form = event.currentTarget;
  form.dataset.saving = "true";
  button.disabled = true;
  form.inert = true;
  $("#editor-error").textContent = "";
  try {
    await editorHandler(form);
  } catch (error) {
    $("#editor-error").textContent = error.message;
  } finally {
    form.dataset.saving = "false";
    form.inert = false;
    button.disabled = false;
  }
});
document.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action]");
  if (!button || button.disabled) return;
  const id = Number(button.dataset.id);
  try {
    switch (button.dataset.action) {
      case "novo-usuario":
        userEditor();
        break;
      case "nova-entrada":
        entryEditor();
        break;
      case "novo-veiculo":
        vehicleEditor();
        break;
      case "mapa":
        setView("vagas");
        break;
      case "editar-veiculo":
        vehicleEditor(state.vehicles.find((vehicle) => vehicle.id === id));
        break;
      case "saida":
        exitEditor(state.entries.find((entry) => entry.id === id));
        break;
      case "editar-vaga":
        spotEditor(state.spots.find((spot) => spot.id === id));
        break;
      case "cancelar":
        confirmEditor(
          "Cancelar entrada?",
          "Use apenas para uma entrada registrada por engano. O registro será removido e a vaga será liberada, sem cobrança.",
          "Cancelar entrada",
          () => api(`/entradas/${id}`, { method: "DELETE" }),
        );
        break;
      case "excluir-veiculo":
        confirmEditor(
          "Excluir veículo?",
          "O cadastro será excluído. Veículos com histórico de entradas não podem ser removidos.",
          "Excluir veículo",
          () => api(`/veiculos/${id}`, { method: "DELETE" }),
        );
        break;
      case "recibo":
        button.disabled = true;
        showReceipt(await api(`/saidas/recibo/${id}`));
        break;
    }
  } catch (error) {
    toast(error.message);
  } finally {
    button.disabled = false;
  }
});
$("#report-form").addEventListener("submit", (event) => {
  event.preventDefault();
  loadReport(0).catch(showLoadError);
});
$("#report-prev").addEventListener("click", () => {
  if (state.report)
    loadReport(state.report.paginaAtual - 1, state.period).catch(showLoadError);
});
$("#report-next").addEventListener("click", () => {
  if (state.report)
    loadReport(state.report.paginaAtual + 1, state.period).catch(showLoadError);
});
$("#export-pdf").addEventListener("click", async (event) => {
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const period = selectedPeriod();
    const pdf = await api(`/relatorios/pdf?${new URLSearchParams(period)}`, {
      blob: true,
    });
    const url = URL.createObjectURL(pdf);
    const link = document.createElement("a");
    link.href = url;
    link.download = `relatorio-estacionamento-${$("#report-start").value}-${$("#report-end").value}.pdf`;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    toast("Relatório exportado.");
  } catch (error) {
    showLoadError(error);
  } finally {
    button.disabled = false;
  }
});

$("#today").textContent = new Date().toLocaleDateString("pt-BR", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});
$("#report-start").value = localDay();
$("#report-end").value = localDay();
setInterval(() => {
  if (state.token && !document.hidden) refresh();
}, 60000);
try {
  const saved = JSON.parse(sessionStorage.getItem("estacionamento.session"));
  if (saved?.token && typeof saved.username === "string")
    enterApp(saved).catch(error => resetSession(error.message));
} catch {
  /* Sem sessão salva: permanece na tela de login. */
}
