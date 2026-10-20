"use strict";

const state = {
  token: sessionStorage.getItem("pagueMais.token"),
  user: JSON.parse(sessionStorage.getItem("pagueMais.user") || "null"),
  view: "dashboard",
  cache: {},
  cart: [],
  categoryFilter: null
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const content = $("#content");
const titles = {dashboard:"Dashboard",produtos:"Produtos",categorias:"Categorias",conta:"Minha conta",carrinho:"Carrinho",clientes:"Clientes",pedidos:"Pedidos",pagamentos:"Pagamentos"};
const money = value => new Intl.NumberFormat("pt-BR", {style:"currency", currency:"BRL"}).format(value || 0);
const date = value => value ? new Intl.DateTimeFormat("pt-BR", {dateStyle:"short", timeStyle:"short"}).format(new Date(value)) : "—";
const safe = value => String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"})[char]);
const isAdmin = () => state.user?.papel === "ADMIN";
const paymentMethodLabel = value => ({
  PIX:"PIX",
  CARTAO_CREDITO:"Cartão de crédito",
  CARTAO_DEBITO:"Cartão de débito",
  BOLETO:"Boleto"
})[value] || value || "—";

function cartStorageKey() {
  return `pagueMais.cart.${state.user?.clienteId || state.user?.id || "guest"}`;
}

function loadCart() {
  try {
    const stored = JSON.parse(localStorage.getItem(cartStorageKey()) || "[]");
    state.cart = Array.isArray(stored) ? stored.filter(item => Number(item.produtoId) && Number(item.quantidade) > 0) : [];
  } catch {
    state.cart = [];
  }
  updateCartBadge();
}

function saveCart() {
  localStorage.setItem(cartStorageKey(), JSON.stringify(state.cart));
  updateCartBadge();
}

function updateCartBadge() {
  const badge = $("#cart-badge");
  if (!badge) return;
  const count = state.cart.reduce((sum, item) => sum + item.quantidade, 0);
  badge.textContent = count;
  badge.hidden = count === 0;
}

function toast(message, type = "success") {
  const element = $("#toast");
  element.textContent = message;
  element.className = `toast ${type} show`;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => element.className = "toast", 3200);
}

function setBusy(button, busy) {
  if (!button) return;
  button.disabled = busy;
  button.dataset.label ||= button.innerHTML;
  button.innerHTML = busy ? "Aguarde…" : button.dataset.label;
}

async function api(path, options = {}) {
  const headers = {Accept:"application/json", ...options.headers};
  if (options.body && !(options.body instanceof FormData)) headers["Content-Type"] = "application/json";
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, {...options, headers});
  if (response.status === 401 && !path.startsWith("auth/")) {
    logout("Sua sessão expirou. Entre novamente.");
    throw new Error("Sessão expirada");
  }
  const body = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    const details = body?.detalhes ? Object.values(body.detalhes).join(" ") : "";
    throw new Error(details || body?.mensagem || `Erro ${response.status}`);
  }
  return body;
}

function showAuth(view = "login") {
  $("#auth-screen").hidden = false;
  $("#app-shell").hidden = true;
  $("#login-view").hidden = view !== "login";
  $("#register-view").hidden = view !== "register";
  if (view === "login") prepareEmptyLoginForm();
}

function clearLoginForm() {
  const form = $("#login-form");
  if (!form) return;
  form.reset();
  $("#login-email").value = "";
  $("#login-password").value = "";
}

let loginEditingStarted = false;

function prepareEmptyLoginForm() {
  loginEditingStarted = false;
  clearLoginForm();
  $("#login-email").readOnly = true;
  $("#login-password").readOnly = true;

  // Alguns navegadores restauram credenciais alguns instantes depois do carregamento.
  [0, 100, 500].forEach(delay => setTimeout(() => {
    if (!state.token && !loginEditingStarted) clearLoginForm();
  }, delay));
}

function showApp() {
  if (!state.user?.papel) {
    logout("A aplicação foi atualizada. Entre novamente.");
    return;
  }
  $("#auth-screen").hidden = true;
  $("#app-shell").hidden = false;
  $("#user-name").textContent = state.user?.nome || "Usuário";
  $("#user-email").textContent = state.user?.email || "";
  $("#user-role").textContent = isAdmin() ? "Administrador" : "Cliente";
  $("#user-avatar").textContent = (state.user?.nome || "U").charAt(0).toUpperCase();
  $$('[data-admin-only]').forEach(element => element.hidden = !isAdmin());
  $$('[data-customer-only]').forEach(element => element.hidden = isAdmin());
  loadCart();
  navigate(location.hash.slice(1) || "dashboard");
}

function logout(message) {
  state.token = null;
  state.user = null;
  state.cache = {};
  sessionStorage.removeItem("pagueMais.token");
  sessionStorage.removeItem("pagueMais.user");
  showAuth();
  if (message) toast(message, "error");
}

function loading() {
  content.innerHTML = '<div class="loading"><div><div class="spinner"></div>Carregando dados…</div></div>';
}

async function getAll(entity, force = false) {
  if (!force && state.cache[entity]) return state.cache[entity];
  const paths = {
    produtos:"produtos/todos",
    categorias:"categorias/todas",
    clientes:"clientes/todos",
    pedidos:isAdmin() ? "pedidos/todos" : "pedidos/meus",
    pagamentos:isAdmin() ? "pagamentos/todos" : "pagamentos/meus"
  };
  state.cache[entity] = await api(paths[entity]);
  return state.cache[entity];
}

async function navigate(view) {
  if (!titles[view]) view = "dashboard";
  if (!isAdmin() && view === "clientes") view = "dashboard";
  if (isAdmin() && ["carrinho", "conta"].includes(view)) view = "dashboard";
  state.view = view;
  location.hash = view;
  $("#page-title").textContent = titles[view];
  $$(".nav-item").forEach(item => item.classList.toggle("active", item.dataset.view === view));
  $(".sidebar").classList.remove("open");
  loading();
  try {
    if (view === "dashboard") await renderDashboard();
    else if (view === "conta") await renderAccount();
    else if (view === "carrinho") await renderCart();
    else if (view === "categorias" && !isAdmin()) await renderCustomerCategories();
    else await renderTable(view);
  } catch (error) {
    content.innerHTML = `<div class="panel empty"><strong>Não foi possível carregar</strong>${safe(error.message)}</div>`;
    toast(error.message, "error");
  }
}

async function renderAccount() {
  const account = await api("clientes/me");
  content.innerHTML = `
    <div class="account-layout">
      <article class="panel account-card">
        <div class="panel-head"><div><h2>Dados pessoais</h2><p>Atualize as informações usadas em seus pedidos e entregas.</p></div></div>
        <div class="panel-body">
          <form id="account-form" class="form-grid">
            ${field("nome","Nome completo",account.nome,"text",true,'autocomplete="name"')}
            ${field("email","E-mail",account.email,"email",true,'autocomplete="email"')}
            ${field("cpf","CPF (somente números)",account.cpf || "","text",false,'inputmode="numeric" pattern="\\d{11}" maxlength="11"')}
            ${field("telefone","Telefone (somente números)",account.telefone || "","tel",false,'inputmode="numeric" pattern="\\d{10,11}" maxlength="11" autocomplete="tel"')}
            ${field("endereco","Endereço",account.endereco || "","text",false,'autocomplete="street-address"',true)}
            <p class="form-help full">Se você alterar o e-mail, será necessário entrar novamente com o novo endereço.</p>
            <div class="form-actions"><button class="button primary" type="submit">Salvar alterações</button></div>
          </form>
        </div>
      </article>
      <aside class="panel account-summary">
        <div class="account-avatar">${safe((account.nome || "C").charAt(0).toUpperCase())}</div>
        <h2>${safe(account.nome)}</h2>
        <p>${safe(account.email)}</p>
        <span class="pill">CONTA ATIVA</span>
      </aside>
    </div>`;

  $("#account-form").addEventListener("submit", async event => {
    event.preventDefault();
    const button = event.submitter;
    const previousEmail = state.user.email;
    const data = Object.fromEntries(new FormData(event.target));
    setBusy(button, true);
    try {
      const updated = await api("clientes/me", {method:"PUT", body:JSON.stringify(data)});
      state.cache = {};
      if (updated.email !== previousEmail) {
        state.token = null;
        state.user = null;
        sessionStorage.removeItem("pagueMais.token");
        sessionStorage.removeItem("pagueMais.user");
        showAuth();
        toast("Cadastro atualizado. Entre novamente com o novo e-mail.");
        return;
      }
      state.user.nome = updated.nome;
      state.user.email = updated.email;
      sessionStorage.setItem("pagueMais.user", JSON.stringify(state.user));
      $("#user-name").textContent = updated.nome;
      $("#user-email").textContent = updated.email;
      $("#user-avatar").textContent = updated.nome.charAt(0).toUpperCase();
      toast("Cadastro atualizado com sucesso.");
      await renderAccount();
    } catch (error) {
      toast(error.message, "error");
      setBusy(button, false);
    }
  });
}

async function renderDashboard() {
  if (!isAdmin()) {
    await renderCustomerDashboard();
    return;
  }
  const [produtos, categorias, clientes, pedidos, pagamentos] = await Promise.all([
    getAll("produtos", true), getAll("categorias", true), getAll("clientes", true), getAll("pedidos", true), getAll("pagamentos", true)
  ]);
  const faturamento = pagamentos.filter(item => item.status === "APROVADO").reduce((sum, item) => sum + Number(item.valor), 0);
  const estoqueBaixo = produtos.filter(item => item.ativo && item.estoque <= 5).sort((a,b) => a.estoque - b.estoque).slice(0,5);
  content.innerHTML = `
    <div class="metric-grid">
      ${metric("Produtos", produtos.length, `${categorias.length} categorias`)}
      ${metric("Clientes", clientes.length, `${clientes.filter(i=>i.ativo).length} ativos`)}
      ${metric("Pedidos", pedidos.length, `${pedidos.filter(i=>i.status === "PENDENTE").length} pendentes`)}
      ${metric("Receita aprovada", money(faturamento), `${pagamentos.filter(i=>i.status === "APROVADO").length} pagamentos`)}
    </div>
    <div class="section-grid">
      <article class="panel">
        <div class="panel-head"><h2>Atenção ao estoque</h2><button class="link-button" data-go="produtos">Ver produtos</button></div>
        <div class="panel-body stock-list">
          ${estoqueBaixo.length ? estoqueBaixo.map(item => `<div class="stock-item"><div class="product-glyph">${safe(item.nome.charAt(0))}</div><div><strong>${safe(item.nome)}</strong><small>${safe(item.descricao || "Sem descrição")}</small></div><span class="pill ${item.estoque === 0 ? "off" : "warn"}">${item.estoque} un.</span></div>`).join("") : '<div class="empty"><strong>Estoque saudável</strong>Nenhum produto com 5 unidades ou menos.</div>'}
        </div>
      </article>
      <article class="panel">
        <div class="panel-head"><h2>Ações rápidas</h2></div>
        <div class="panel-body quick-actions">
          <button class="quick-action" data-new="produto">Novo produto <svg class="ico"><use href="#i-arrow"/></svg></button>
          <button class="quick-action" data-new="cliente">Novo cliente <svg class="ico"><use href="#i-arrow"/></svg></button>
          <button class="quick-action" data-new="pedido">Criar pedido <svg class="ico"><use href="#i-arrow"/></svg></button>
          <button class="quick-action" data-new="pagamento">Registrar pagamento <svg class="ico"><use href="#i-arrow"/></svg></button>
        </div>
      </article>
    </div>`;
  bindContentActions();
}

async function renderCustomerDashboard() {
  const [pedidos, pagamentos] = await Promise.all([
    getAll("pedidos", true), getAll("pagamentos", true)
  ]);
  const pagos = new Set(pagamentos.filter(item => item.status !== "RECUSADO").map(item => item.pedidoId));
  const aguardando = pedidos.filter(item => item.status === "PENDENTE" && !pagos.has(item.id));
  const quantidadeCarrinho = state.cart.reduce((sum, item) => sum + item.quantidade, 0);
  const recentes = pedidos.slice(0, 5);
  content.innerHTML = `
    <div class="metric-grid">
      ${metric("Itens no carrinho", quantidadeCarrinho, quantidadeCarrinho === 1 ? "1 produto" : `${quantidadeCarrinho} produtos`)}
      ${metric("Meus pedidos", pedidos.length, `${pedidos.filter(i=>i.status === "PENDENTE").length} pendentes`)}
      ${metric("Aguardando pagamento", aguardando.length, "pedidos não pagos")}
      ${metric("Total comprado", money(pedidos.reduce((sum,item)=>sum+Number(item.total||0),0)), "histórico")}
    </div>
    <div class="section-grid">
      <article class="panel">
        <div class="panel-head"><h2>Pedidos recentes</h2><button class="link-button" data-go="pedidos">Ver todos</button></div>
        <div class="panel-body order-list">
          ${recentes.length ? recentes.map(item => `<button class="order-summary" data-go="pedidos"><span><strong>Pedido #${item.id}</strong><small>${date(item.dataCriacao)}</small></span><b>${money(item.total)}</b>${status(item.status)}</button>`).join("") : '<div class="empty compact"><strong>Nenhum pedido ainda</strong>Adicione produtos ao carrinho e confirme a entrega.</div>'}
        </div>
      </article>
      <article class="panel">
        <div class="panel-head"><h2>Onde você quer começar?</h2></div>
        <div class="panel-body quick-actions">
          <button class="quick-action" data-go="categorias">Explorar categorias <svg class="ico"><use href="#i-arrow"/></svg></button>
          <button class="quick-action" data-go="produtos">Ver produtos <svg class="ico"><use href="#i-arrow"/></svg></button>
          <button class="quick-action" data-go="carrinho">Revisar carrinho <svg class="ico"><use href="#i-arrow"/></svg></button>
          ${aguardando.length ? `<button class="quick-action" data-pay="${aguardando[0].id}">Pagar pedido #${aguardando[0].id} <svg class="ico"><use href="#i-arrow"/></svg></button>` : ""}
        </div>
      </article>
    </div>`;
  bindContentActions();
  $$('[data-pay]').forEach(button => button.onclick = () => openForm("pagamento", null, {pedidoId:Number(button.dataset.pay)}));
}

function metric(label, value, note) {
  return `<article class="metric"><small>${label}</small><strong>${safe(value)}</strong><span class="trend">${safe(note)}</span></article>`;
}

async function renderCustomerCategories() {
  const [categorias, produtos] = await Promise.all([getAll("categorias", true), getAll("produtos", true)]);
  const ativas = categorias.filter(item => item.ativo);
  content.innerHTML = `<div class="category-grid">
    ${ativas.length ? ativas.map(categoria => {
      const quantidade = produtos.filter(produto => produto.ativo && produto.estoque > 0 && produto.categoriaId === categoria.id).length;
      return `<article class="category-card"><div class="category-icon"><svg class="ico"><use href="#i-tag"/></svg></div><div><h2>${safe(categoria.nome)}</h2><p>${safe(categoria.descricao || "Explore os produtos desta categoria.")}</p></div><span>${quantidade} ${quantidade === 1 ? "produto" : "produtos"}</span><button class="button secondary" data-browse-category="${categoria.id}">Ver produtos</button></article>`;
    }).join("") : '<div class="panel empty"><strong>Nenhuma categoria disponível</strong>Volte novamente em breve.</div>'}
  </div>`;
  $$('[data-browse-category]').forEach(button => button.onclick = () => {
    state.categoryFilter = Number(button.dataset.browseCategory);
    navigate("produtos");
  });
}

function freightValue(mode, subtotal) {
  if (mode === "EXPRESSA") return 39.90;
  if (mode === "PADRAO" && subtotal < 200) return 19.90;
  return 0;
}

async function renderCart() {
  const produtos = await getAll("produtos", true);
  state.cart = state.cart.filter(cartItem => produtos.some(produto => produto.id === cartItem.produtoId));
  saveCart();
  const entries = state.cart.map(cartItem => ({
    cartItem,
    produto: produtos.find(produto => produto.id === cartItem.produtoId)
  }));
  const subtotal = entries.reduce((sum, entry) => sum + Number(entry.produto.preco) * entry.cartItem.quantidade, 0);
  content.innerHTML = entries.length ? `
    <div class="cart-layout">
      <section class="panel">
        <div class="panel-head"><h2>Seus produtos</h2><button class="link-button" data-go="produtos">Continuar comprando</button></div>
        <div class="panel-body cart-list">
          ${entries.map(({cartItem, produto}) => `<article class="cart-item">
            <div class="product-glyph">${safe(produto.nome.charAt(0))}</div>
            <div class="cart-product"><strong>${safe(produto.nome)}</strong><small>${money(produto.preco)} por unidade · ${produto.estoque} em estoque</small></div>
            <label class="quantity-control"><span>Qtd.</span><input type="number" min="1" max="${produto.estoque}" value="${cartItem.quantidade}" data-cart-quantity="${produto.id}" aria-label="Quantidade de ${safe(produto.nome)}"></label>
            <b>${money(Number(produto.preco) * cartItem.quantidade)}</b>
            <button class="icon-button remove-cart" data-cart-remove="${produto.id}" aria-label="Remover ${safe(produto.nome)}"><svg class="ico"><use href="#i-x"/></svg></button>
          </article>`).join("")}
        </div>
      </section>
      <aside class="panel checkout-card">
        <div class="panel-head"><h2>Entrega e resumo</h2></div>
        <form id="checkout-form" class="panel-body checkout-form">
          <label class="field full">Modalidade de entrega
            <select name="modalidadeFrete" required>
              <option value="PADRAO">Entrega padrão — ${subtotal >= 200 ? "grátis" : money(19.90)}</option>
              <option value="EXPRESSA">Entrega expressa — ${money(39.90)}</option>
              <option value="RETIRADA">Retirada — grátis</option>
            </select>
          </label>
          <div id="delivery-address" class="delivery-address">
            <label class="field">CEP<input name="cepEntrega" inputmode="numeric" maxlength="8" pattern="\d{8}" placeholder="00000000" required></label>
            <label class="field full">Endereço completo<input name="enderecoEntrega" maxlength="255" placeholder="Rua, número, bairro e cidade" required></label>
          </div>
          <div class="checkout-totals"><span>Produtos <b>${money(subtotal)}</b></span><span>Frete <b id="freight-total">${subtotal >= 200 ? "Grátis" : money(19.90)}</b></span><span class="grand-total">Total <b id="checkout-total">${money(subtotal + freightValue("PADRAO", subtotal))}</b></span></div>
          <label class="accept-delivery"><input name="aceite" type="checkbox" required> Confirmo os itens, o endereço e a modalidade de entrega.</label>
          <p class="checkout-note">O pedido será criado somente após esta confirmação. Em seguida, você poderá escolher a forma de pagamento.</p>
          <button class="button primary checkout-button" type="submit">Confirmar entrega e ir para pagamento</button>
        </form>
      </aside>
    </div>` : `<div class="panel empty cart-empty"><svg class="empty-cart-icon"><use href="#i-cart"/></svg><strong>Seu carrinho está vazio</strong>Escolha produtos ou navegue pelas categorias para começar.<div class="empty-actions"><button class="button primary" data-go="produtos">Ver produtos</button><button class="button secondary" data-go="categorias">Ver categorias</button></div></div>`;
  bindContentActions();
  $$('[data-cart-quantity]').forEach(input => input.onchange = () => updateCartQuantity(Number(input.dataset.cartQuantity), Number(input.value), Number(input.max)));
  $$('[data-cart-remove]').forEach(button => button.onclick = () => removeFromCart(Number(button.dataset.cartRemove)));
  const form = $("#checkout-form");
  if (form) {
    const mode = form.elements.modalidadeFrete;
    const address = $("#delivery-address");
    const updateDelivery = () => {
      const isPickup = mode.value === "RETIRADA";
      address.hidden = isPickup;
      address.querySelectorAll("input").forEach(input => input.required = !isPickup);
      const freight = freightValue(mode.value, subtotal);
      $("#freight-total").textContent = freight === 0 ? "Grátis" : money(freight);
      $("#checkout-total").textContent = money(subtotal + freight);
    };
    mode.onchange = updateDelivery;
    updateDelivery();
    form.onsubmit = event => checkout(event, subtotal);
  }
}

function addToCart(productId) {
  const produto = state.cache.produtos?.find(item => item.id === productId);
  if (!produto || !produto.ativo || produto.estoque <= 0) {
    toast("Este produto não está disponível.", "error");
    return;
  }
  const existing = state.cart.find(item => item.produtoId === productId);
  if (existing) {
    if (existing.quantidade >= produto.estoque) {
      toast("Você já adicionou todo o estoque disponível.", "error");
      return;
    }
    existing.quantidade += 1;
  } else {
    state.cart.push({produtoId:productId, quantidade:1});
  }
  saveCart();
  toast(`${produto.nome} foi adicionado ao carrinho.`);
}

function updateCartQuantity(productId, quantity, stock) {
  const item = state.cart.find(value => value.produtoId === productId);
  if (!item) return;
  item.quantidade = Math.max(1, Math.min(stock, Number.isFinite(quantity) ? quantity : 1));
  saveCart();
  renderCart();
}

function removeFromCart(productId) {
  state.cart = state.cart.filter(item => item.produtoId !== productId);
  saveCart();
  renderCart();
}

async function checkout(event) {
  event.preventDefault();
  const button = event.submitter;
  setBusy(button, true);
  const data = Object.fromEntries(new FormData(event.target));
  const pickup = data.modalidadeFrete === "RETIRADA";
  const payload = {
    clienteId: state.user?.clienteId,
    itens: state.cart.map(item => ({produtoId:item.produtoId, quantidade:item.quantidade})),
    modalidadeFrete: data.modalidadeFrete,
    cepEntrega: pickup ? null : data.cepEntrega,
    enderecoEntrega: pickup ? null : data.enderecoEntrega
  };
  try {
    const pedido = await api("pedidos", {method:"POST", body:JSON.stringify(payload)});
    state.cart = [];
    saveCart();
    state.cache = {};
    toast(`Pedido #${pedido.id} criado. Agora escolha o pagamento.`);
    await navigate("pedidos");
    await openForm("pagamento", null, {pedidoId:pedido.id});
  } catch (error) {
    toast(error.message, "error");
    setBusy(button, false);
  }
}

const columns = {
  produtos:["Produto","Preço","Estoque","Categoria","Status"],
  categorias:["Categoria","Descrição","Status"],
  clientes:["Cliente","CPF","Telefone","Status"],
  pedidos:["Pedido","Cliente","Total","Data","Status"],
  pagamentos:["Pagamento","Pedido","Valor","Método","Data","Status"]
};

async function renderTable(entity) {
  let items = await getAll(entity, true);
  if (entity === "produtos") await getAll("categorias");
  if (entity === "produtos" && !isAdmin()) items = items.filter(item => item.ativo && item.estoque > 0);
  if (entity === "pedidos") {
    await getAll("produtos");
    if (isAdmin()) await getAll("clientes");
  }
  if (entity === "pagamentos") await getAll("pedidos");
  const activeCategory = entity === "produtos" && state.categoryFilter
    ? state.cache.categorias?.find(item => item.id === state.categoryFilter)
    : null;
  if (activeCategory) items = items.filter(item => item.categoriaId === activeCategory.id);
  const singular = {produtos:"produto",categorias:"categoria",clientes:"cliente",pedidos:"pedido",pagamentos:"pagamento"}[entity];
  const podeCriar = isAdmin() || entity === "pagamentos";
  const customerAction = !isAdmin() && entity === "pedidos"
    ? '<button class="button primary" data-go="carrinho">Ir para o carrinho</button>'
    : "";
  content.innerHTML = `
    <div class="list-toolbar">
      <div class="toolbar-search"><label class="search"><svg class="ico"><use href="#i-search"/></svg><input id="table-search" placeholder="Buscar em ${entity}…" aria-label="Buscar"></label>${activeCategory ? `<button class="filter-chip" id="clear-category-filter">${safe(activeCategory.nome)} <span>×</span></button>` : ""}</div>
      ${podeCriar ? `<button class="button primary" data-new="${singular}">${isAdmin() ? `+ Novo ${singular}` : "Pagar pedido"}</button>` : customerAction}
    </div>
    <div class="table-wrap"><table><thead><tr>${columns[entity].map(name=>`<th>${name}</th>`).join("")}<th>Ações</th></tr></thead><tbody id="table-body">${rows(entity, items)}</tbody></table></div>`;
  $("#table-search").addEventListener("input", event => {
    const term = event.target.value.toLocaleLowerCase("pt-BR");
    const filtered = items.filter(item => JSON.stringify(item).toLocaleLowerCase("pt-BR").includes(term));
    $("#table-body").innerHTML = rows(entity, filtered);
    bindRowActions(entity);
  });
  bindContentActions();
  bindRowActions(entity);
  const clearFilter = $("#clear-category-filter");
  if (clearFilter) clearFilter.onclick = () => { state.categoryFilter = null; renderTable("produtos"); };
}

function rows(entity, items) {
  if (!items.length) return `<tr><td colspan="${columns[entity].length + 1}" class="empty"><strong>Nenhum registro ainda</strong>Use o botão acima para cadastrar.</td></tr>`;
  const categoryName = id => state.cache.categorias?.find(item=>item.id===id)?.nome || `#${id}`;
  const clientName = id => state.cache.clientes?.find(item=>item.id===id)?.nome || `#${id}`;
  return items.map(item => {
    let cells = "";
    if (entity === "produtos") cells = `<td><strong>${safe(item.nome)}</strong><br><small>${safe(item.descricao || "")}</small></td><td>${money(item.preco)}</td><td>${item.estoque}</td><td>${safe(categoryName(item.categoriaId))}</td><td>${status(item.ativo ? "ATIVO":"INATIVO")}</td>`;
    if (entity === "categorias") cells = `<td><strong>${safe(item.nome)}</strong></td><td>${safe(item.descricao || "—")}</td><td>${status(item.ativo ? "ATIVO":"INATIVO")}</td>`;
    if (entity === "clientes") cells = `<td><strong>${safe(item.nome)}</strong><br><small>${safe(item.email)}</small></td><td>${safe(item.cpf)}</td><td>${safe(item.telefone || "—")}</td><td>${status(item.ativo ? "ATIVO":"INATIVO")}</td>`;
    if (entity === "pedidos") cells = `<td><strong>#${item.id}</strong></td><td>${safe(item.clienteNome || (isAdmin() ? clientName(item.clienteId) : state.user?.nome) || `#${item.clienteId}`)}</td><td>${money(item.total)}</td><td>${date(item.dataCriacao)}</td><td>${status(item.status)}</td>`;
    if (entity === "pagamentos") cells = `<td><strong>#${item.id}</strong></td><td>#${item.pedidoId}</td><td>${money(item.valor)}</td><td>${safe(paymentMethodLabel(item.metodo))}</td><td>${date(item.dataCriacao)}</td><td>${status(item.status)}</td>`;
    let actions = "";
    if (isAdmin()) {
      actions = entity === "pagamentos" ? `<button data-status-payment="${item.id}">Status</button>` : `<button data-edit="${item.id}">Editar</button>`;
      actions += `<button data-delete="${item.id}">Excluir</button>`;
    } else if (entity === "produtos" && item.ativo && item.estoque > 0) {
      actions = `<button data-buy="${item.id}">Comprar</button>`;
    }
    return `<tr>${cells}<td><div class="actions">${actions || "—"}</div></td></tr>`;
  }).join("");
}

function status(value) {
  const warning = value === "PENDENTE";
  const off = ["INATIVO","CANCELADO","RECUSADO","REEMBOLSADO"].includes(value);
  return `<span class="pill ${warning ? "warn" : off ? "off" : ""}">${safe(value)}</span>`;
}

function bindContentActions() {
  $$('[data-go]').forEach(button => button.onclick = () => navigate(button.dataset.go));
  $$('[data-new]').forEach(button => button.onclick = () => openForm(button.dataset.new));
}

function bindRowActions(entity) {
  $$('[data-edit]').forEach(button => button.onclick = () => {
    const item = state.cache[entity].find(value => value.id === Number(button.dataset.edit));
    openForm(entity.slice(0,-1), item);
  });
  $$('[data-delete]').forEach(button => button.onclick = () => removeEntity(entity, Number(button.dataset.delete)));
  $$('[data-status-payment]').forEach(button => button.onclick = () => changePaymentStatus(Number(button.dataset.statusPayment)));
  bindBuyActions();
}

function bindBuyActions() {
  $$('[data-buy]').forEach(button => button.onclick = () => addToCart(Number(button.dataset.buy)));
}

async function openForm(type, item = null, defaults = {}) {
  if (!isAdmin() && ["produto", "categoria", "cliente"].includes(type)) {
    toast("Esta ação é exclusiva do administrador.", "error");
    return;
  }
  const dialog = $("#entity-dialog");
  const title = type === "pagamento"
    ? (isAdmin() ? "Registrar pagamento" : "Pagar pedido")
    : `${item ? "Editar" : "Novo"} ${type}`;
  $("#dialog-title").textContent = title;
  try {
    if (type === "produto") await getAll("categorias");
    if (type === "pedido") {
      await Promise.all([getAll("produtos"), getAll("pedidos")]);
      if (isAdmin()) await getAll("clientes");
    }
    if (type === "pagamento") await Promise.all([getAll("pedidos"), getAll("pagamentos")]);
    $("#dialog-content").innerHTML = formMarkup(type, item, defaults);
    if (type === "pagamento" && !isAdmin()) {
      const pedido = $("#entity-form [name=pedidoId]");
      const valor = $("#entity-form [name=valor]");
      const metodo = $("#entity-form [name=metodo]");
      const atualizarPagamento = () => {
        valor.value = state.cache.pedidos?.find(value => value.id === Number(pedido.value))?.total || "";
        const pagamento = state.cache.pagamentos?.find(value => value.pedidoId === Number(pedido.value));
        metodo.value = pagamento?.status === "PENDENTE" ? pagamento.metodo : "PIX";
      };
      pedido.addEventListener("change", atualizarPagamento);
      atualizarPagamento();
    }
    $("#entity-form").onsubmit = event => saveEntity(event, type, item?.id);
    dialog.showModal();
  } catch (error) { toast(error.message, "error"); }
}

function formMarkup(type, item = {}, defaults = {}) {
  item = item ?? {};
  const active = item.ativo !== false;
  const options = (items, selected, label="nome") => items.map(value => `<option value="${value.id}" ${value.id === selected ? "selected":""}>${safe(value[label] || `#${value.id}`)}</option>`).join("");
  let fields = "";
  if (type === "categoria") fields = `${field("nome","Nome",item.nome,"text",true)}${field("descricao","Descrição",item.descricao,"textarea")} ${check(active)}`;
  if (type === "produto") fields = `${field("nome","Nome",item.nome,"text",true)}${field("preco","Preço",item.preco,"number",true,'step="0.01" min="0.01"')}${field("estoque","Estoque",item.estoque ?? 0,"number",true,'min="0"')}<label class="field">Categoria<select name="categoriaId" required><option value="">Selecione…</option>${options(state.cache.categorias || [],item.categoriaId)}</select></label>${field("descricao","Descrição",item.descricao,"textarea",false,"",true)}${check(active)}`;
  if (type === "cliente") fields = `${field("nome","Nome",item.nome,"text",true)}${field("email","E-mail",item.email,"email",true)}${field("cpf","CPF (11 dígitos)",item.cpf,"text",true,'pattern="\\d{11}"')}${field("telefone","Telefone",item.telefone,"tel",false,'pattern="\\d{10,11}"')}${field("endereco","Endereço",item.endereco,"text",false,"",true)}${check(active)}`;
  if (type === "pedido" && item.id) fields = `<label class="field full">Status<select name="status" required>${["PENDENTE","CONFIRMADO","ENVIADO","ENTREGUE","CANCELADO"].map(value=>`<option ${value===item.status?"selected":""}>${value}</option>`).join("")}</select></label>`;
  if (type === "pedido" && !item.id) {
    const clienteField = isAdmin()
      ? `<label class="field">Cliente<select name="clienteId" required><option value="">Selecione…</option>${options((state.cache.clientes||[]).filter(i=>i.ativo))}</select></label>`
      : `<input type="hidden" name="clienteId" value="${state.user?.clienteId || ""}">`;
    fields = `${clienteField}<label class="field">Produto<select name="produtoId" required><option value="">Selecione…</option>${options((state.cache.produtos||[]).filter(i=>i.ativo && i.estoque>0), defaults.produtoId)}</select></label>${field("quantidade","Quantidade",1,"number",true,'min="1"')}`;
  }
  if (type === "pagamento") {
    const encerrados = new Set((state.cache.pagamentos || []).filter(value => ["APROVADO","REEMBOLSADO"].includes(value.status)).map(value => value.pedidoId));
    const pedidos = (state.cache.pedidos || []).filter(value => value.status !== "CANCELADO" && !encerrados.has(value.id));
    const metodos = ["PIX","CARTAO_CREDITO","CARTAO_DEBITO","BOLETO"];
    fields = `<label class="field">Pedido<select name="pedidoId" required><option value="">Selecione…</option>${pedidos.map(value=>`<option value="${value.id}" ${value.id===defaults.pedidoId?"selected":""}>#${value.id} — ${money(value.total)}</option>`).join("")}</select></label>${field("valor","Valor",item.valor,"number",true,`step="0.01" min="0.01" ${isAdmin() ? "" : "readonly"}`)}<label class="field full">Método<select name="metodo" required>${metodos.map(value=>`<option value="${value}">${paymentMethodLabel(value)}</option>`).join("")}</select></label><p class="form-help full">Se este pedido já tiver um pagamento pendente, a forma de pagamento será atualizada sem criar outro registro.</p>`;
  }
  return `<form id="entity-form" class="form-grid">${fields}<div class="form-actions"><button type="button" class="button secondary" onclick="document.querySelector('#entity-dialog').close()">Cancelar</button><button type="submit" class="button primary">Salvar</button></div></form>`;
}

function field(name, label, value="", type="text", required=false, attrs="", full=false) {
  if (type === "textarea") return `<label class="field full">${label}<textarea name="${name}" ${required?"required":""}>${safe(value)}</textarea></label>`;
  return `<label class="field ${full?"full":""}">${label}<input name="${name}" type="${type}" value="${safe(value)}" ${required?"required":""} ${attrs}></label>`;
}
function check(active) { return `<label class="check-field full"><input name="ativo" type="checkbox" ${active?"checked":""}> Registro ativo</label>`; }

async function saveEntity(event, type, id) {
  event.preventDefault();
  const button = event.submitter;
  setBusy(button, true);
  const data = Object.fromEntries(new FormData(event.target));
  if ("ativo" in Object.fromEntries(new FormData(event.target)) || ["categoria","produto","cliente"].includes(type)) data.ativo = event.target.elements.ativo?.checked ?? true;
  if (type === "produto") { data.preco = Number(data.preco); data.estoque = Number(data.estoque); data.categoriaId = Number(data.categoriaId); }
  if (type === "pedido" && !id) data.itens = [{produtoId:Number(data.produtoId), quantidade:Number(data.quantidade)}], delete data.produtoId, delete data.quantidade, data.clienteId = Number(data.clienteId);
  if (type === "pagamento") { data.pedidoId = Number(data.pedidoId); data.valor = Number(data.valor); }
  const plural = {categoria:"categorias",produto:"produtos",cliente:"clientes",pedido:"pedidos",pagamento:"pagamentos"}[type];
  try {
    await api(`${plural}${id ? `/${id}` : ""}`, {method:id?"PUT":"POST", body:JSON.stringify(data)});
    state.cache = {};
    $("#entity-dialog").close();
    toast(type === "pagamento" ? "Forma de pagamento salva com sucesso." : `${type[0].toUpperCase()+type.slice(1)} salvo com sucesso.`);
    navigate(plural);
  } catch (error) { toast(error.message,"error"); setBusy(button,false); }
}

async function removeEntity(entity, id) {
  if (!confirm(`Excluir o registro #${id}? Esta ação não pode ser desfeita.`)) return;
  try {
    await api(`${entity}/${id}`, {method:"DELETE"});
    state.cache = {};
    toast("Registro excluído.");
    navigate(entity);
  } catch (error) { toast(error.message,"error"); }
}

async function changePaymentStatus(id) {
  const value = prompt("Novo status: PENDENTE, APROVADO, RECUSADO ou REEMBOLSADO");
  if (!value) return;
  try {
    await api(`pagamentos/${id}/status?status=${encodeURIComponent(value.toUpperCase())}`, {method:"PATCH"});
    state.cache = {};
    toast("Status atualizado.");
    navigate("pagamentos");
  } catch (error) { toast(error.message,"error"); }
}

const loginForm = $("#login-form");
const loginEmail = $("#login-email");
const loginPassword = $("#login-password");

function enableLoginEditing(field) {
  loginEditingStarted = true;
  field.readOnly = false;
}

[loginEmail, loginPassword].forEach(field => {
  field.addEventListener("pointerdown", () => enableLoginEditing(field));
  field.addEventListener("keydown", () => enableLoginEditing(field));
  field.addEventListener("focus", () => enableLoginEditing(field));
});

$("#admin-login-hint").onclick = () => {
  enableLoginEditing(loginEmail);
  enableLoginEditing(loginPassword);
  loginEmail.value = "admin@paguemais.local";
  loginPassword.value = "";
  loginPassword.focus();
};

$(".password-toggle").onclick = event => {
  enableLoginEditing(loginPassword);
  const visible = loginPassword.type === "text";
  loginPassword.type = visible ? "password" : "text";
  event.currentTarget.textContent = visible ? "Mostrar" : "Ocultar";
  event.currentTarget.setAttribute("aria-label", visible ? "Mostrar senha" : "Ocultar senha");
  event.currentTarget.setAttribute("aria-pressed", String(!visible));
  loginPassword.focus();
};

loginForm.addEventListener("submit", async event => {
  event.preventDefault();
  const button = event.submitter;
  setBusy(button,true);
  try {
    const payload = {email:loginEmail.value.trim(), senha:loginPassword.value};
    const result = await api("auth/login", {method:"POST", body:JSON.stringify(payload)});
    state.token = result.token;
    state.user = {id:result.id,email:result.email,nome:result.nome,papel:result.papel,clienteId:result.clienteId};
    sessionStorage.setItem("pagueMais.token",state.token);
    sessionStorage.setItem("pagueMais.user",JSON.stringify(state.user));
    showApp();
  } catch (error) {
    loginPassword.value = "";
    loginPassword.focus();
    toast(error.message,"error");
  } finally { setBusy(button,false); }
});

$("#register-form").addEventListener("submit", async event => {
  event.preventDefault();
  const button = event.submitter;
  setBusy(button,true);
  const payload = Object.fromEntries(new FormData(event.target));
  try {
    await api("auth/registrar", {method:"POST", body:JSON.stringify(payload)});
    toast("Conta criada. Agora faça seu login.");
    showAuth("login");
    enableLoginEditing(loginEmail);
    enableLoginEditing(loginPassword);
    loginEmail.value = payload.email;
  } catch (error) { toast(error.message,"error"); } finally { setBusy(button,false); }
});

$$('[data-auth-view]').forEach(button => button.onclick = () => showAuth(button.dataset.authView));
$$('.nav-item').forEach(button => button.onclick = () => navigate(button.dataset.view));
$("#logout-button").onclick = () => logout("Você saiu da conta.");
$("#menu-button").onclick = () => $(".sidebar").classList.toggle("open");
$("#dialog-close").onclick = () => $("#entity-dialog").close();
window.addEventListener("hashchange", () => {
  const target = location.hash.slice(1) || "dashboard";
  if (state.token && target !== state.view) navigate(target);
});

window.addEventListener("pageshow", () => {
  if (!state.token) prepareEmptyLoginForm();
});

state.token ? showApp() : showAuth();
