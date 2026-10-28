/* SPBANK FRONT-END — Integração com o backend oficial */

let saldo = 0;
let filtroAtual = 'todos';
let extrato = [];
let currentUser = null;
let currentAccountSummary = null;
let customerAccounts = [];
let pendingTransfer = null;
let pendingIdempotencyKey = null;
let adminCustomers = [];
let cardRevealEpoch = 0;

const apiBaseUrl = window.SPBANK_API_URL ?? 'http://localhost:8080';
const apiTimeoutMs = 10_000;

const themePreferenceKey = 'spbank.theme';
const validThemePreferences = new Set(['system', 'light', 'dark']);
const themeLabels = { system: 'Sistema', light: 'Claro', dark: 'Escuro' };
const themeIcons = { system: 'contrast', light: 'light_mode', dark: 'dark_mode' };
const systemThemeQuery = window.matchMedia('(prefers-color-scheme: dark)');

function getThemePreference() {
  try {
    const saved = localStorage.getItem(themePreferenceKey);
    if (validThemePreferences.has(saved)) return saved;
  } catch (_) {}
  return 'system';
}

function resolveTheme(preference) {
  if (preference === 'light' || preference === 'dark') return preference;
  return systemThemeQuery.matches ? 'dark' : 'light';
}

function updateThemeAssets(effectiveTheme) {
  const dark = effectiveTheme === 'dark';
  const logoPath = dark ? 'img/logo-spbank-dark.png' : 'img/logo-spbank.png';
  const faviconPath = dark ? 'img/favicon-spbank-dark.png' : 'img/favicon-spbank.png';

  document.querySelectorAll('.theme-logo').forEach(logo => {
    logo.src = logoPath;
  });

  const favicon = document.getElementById('appFavicon');
  if (favicon) favicon.href = faviconPath;
}

function updateThemeControl(preference) {
  const currentLabel = document.getElementById('themeCurrentLabel');
  const icon = document.getElementById('themeIcon');
  const menuButton = document.getElementById('themeMenuButton');

  if (currentLabel) currentLabel.textContent = themeLabels[preference] ?? themeLabels.system;
  if (icon) icon.textContent = themeIcons[preference] ?? themeIcons.system;
  if (menuButton) {
    menuButton.setAttribute(
      'aria-label',
      `Tema atual: ${themeLabels[preference] ?? themeLabels.system}. Alterar tema.`
    );
  }

  document.querySelectorAll('[data-theme-option]').forEach(option => {
    option.classList.toggle('active', option.dataset.themeOption === preference);
  });
}

function applyTheme(preference = getThemePreference()) {
  const normalized = validThemePreferences.has(preference) ? preference : 'system';
  const effective = resolveTheme(normalized);

  document.documentElement.setAttribute('data-theme', effective);
  document.documentElement.style.colorScheme = effective;
  updateThemeAssets(effective);
  updateThemeControl(normalized);
}

function saveThemePreference(preference) {
  if (!validThemePreferences.has(preference)) return;
  try {
    localStorage.setItem(themePreferenceKey, preference);
  } catch (_) {}
  applyTheme(preference);
}

function closeThemeMenu(returnFocus = false) {
  const menu = document.getElementById('themeMenu');
  const button = document.getElementById('themeMenuButton');
  if (!menu) return;
  menu.classList.add('hidden');
  button?.setAttribute('aria-expanded', 'false');
  if (returnFocus) button?.focus();
}

function openThemeMenu() {
  const menu = document.getElementById('themeMenu');
  const button = document.getElementById('themeMenuButton');
  if (!menu) return;
  menu.classList.remove('hidden');
  button?.setAttribute('aria-expanded', 'true');
  menu.querySelector('.theme-option.active')?.focus();
}

function toggleThemeMenu() {
  const menu = document.getElementById('themeMenu');
  if (!menu) return;
  if (menu.classList.contains('hidden')) openThemeMenu();
  else closeThemeMenu();
}

function handleSystemThemeChange() {
  if (getThemePreference() === 'system') applyTheme('system');
}

function initializeTheme() {
  applyTheme(getThemePreference());

  const control = document.getElementById('themeControl');
  const menuButton = document.getElementById('themeMenuButton');

  menuButton?.addEventListener('click', event => {
    event.stopPropagation();
    toggleThemeMenu();
  });

  document.querySelectorAll('[data-theme-option]').forEach(option => {
    option.addEventListener('click', event => {
      event.stopPropagation();
      saveThemePreference(option.dataset.themeOption);
      closeThemeMenu(true);
    });
  });

  document.addEventListener('click', event => {
    if (control && !control.contains(event.target)) closeThemeMenu();
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') closeThemeMenu(true);
  });

  if (typeof systemThemeQuery.addEventListener === 'function') {
    systemThemeQuery.addEventListener('change', handleSystemThemeChange);
  } else if (typeof systemThemeQuery.addListener === 'function') {
    systemThemeQuery.addListener(handleSystemThemeChange);
  }

  window.addEventListener('storage', event => {
    if (event.key === themePreferenceKey) applyTheme(getThemePreference());
  });
}

const authTokenKey = 'spbank.access-token';
const adminAuthTokenKey = 'spbank.admin-access-token';
const adminNameKey = 'spbank.admin-name';
const activeContextKey = 'spbank.active-context';

const transferStatusLabels = {
  SCHEDULED: 'Agendada',
  PROCESSING: 'Em processamento',
  COMPLETED: 'Concluída',
  FAILED: 'Falhou',
  CANCELLED: 'Cancelada'
};

const transferTypeLabels = {
  INTERNAL: 'Entre contas SPBank',
  TED: 'TED'
};

const accountTypeLabels = {
  CURRENT: 'Conta corrente',
  SAVINGS: 'Conta poupança'
};

const customerPlanLabels = {
  STANDARD: 'Standard',
  PLUS: 'Plus'
};

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  });
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('pt-BR');
}

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('pt-BR');
}

function formatCpf(value) {
  const digits = String(value ?? '').replace(/\D/g, '');
  if (digits.length !== 11) return value ?? '—';
  return digits.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
}

function escapeHtml(value) {
  return String(value ?? '—').replace(/[&<>'"]/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
  })[character]);
}

function transferModalityLabel(value) {
  if (value === 'TED') return 'Transferência - TED';
  if (value === 'INTERNAL' || value === 'INTERNA') return 'Transferência';
  return value || 'Transferência';
}

function accountTypeLabel(value) {
  return accountTypeLabels[value] ?? value ?? 'Conta';
}

function customerPlanLabel(value) {
  return customerPlanLabels[value] ?? value ?? '—';
}

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) element.textContent = value ?? '—';
}

function clearMessage(element) {
  if (!element) return;
  element.textContent = '';
  element.classList.remove('success-message');
}

function showSuccess(element, message) {
  if (!element) return;
  element.textContent = message;
  element.classList.add('success-message');
}

function showApiError(element, error) {
  if (!element) return;
  element.classList.remove('success-message');
  const message = error?.message || 'Não foi possível concluir a operação.';
  element.textContent = error?.correlationId
    ? `${message} (código de atendimento: ${error.correlationId})`
    : message;
}

function clearClientSession() {
  hideRevealedCardData();
  sessionStorage.removeItem(authTokenKey);
  if (sessionStorage.getItem(activeContextKey) === 'client') {
    sessionStorage.removeItem(activeContextKey);
  }

  currentUser = null;
  currentAccountSummary = null;
  customerAccounts = [];
  pendingTransfer = null;
  pendingIdempotencyKey = null;
  saldo = 0;
  extrato = [];
}

function clearAdminSession() {
  sessionStorage.removeItem(adminAuthTokenKey);
  sessionStorage.removeItem(adminNameKey);
  if (sessionStorage.getItem(activeContextKey) === 'admin') {
    sessionStorage.removeItem(activeContextKey);
  }
  adminCustomers = [];
}

async function api(path, options = {}) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), apiTimeoutMs);

  try {
    const token = sessionStorage.getItem(authTokenKey);
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {})
      }
    });

    const contentType = response.headers.get('content-type') ?? '';
    const isJson =
      contentType.includes('application/json') ||
      contentType.includes('application/problem+json');

    const body = response.status === 204 || !isJson
      ? null
      : await response.json();

    if (!response.ok) {
      const publicRoute =
        path === '/api/v1/auth/login' ||
        path === '/api/v1/auth/register';

      if (response.status === 401 && !publicRoute) {
        clearClientSession();
        showLogin('Sua sessão expirou. Entre novamente.');
      }

      const error = new Error(
        body?.detail ||
        'Serviço temporariamente indisponível. Consulte a operação antes de reenviar.'
      );
      error.status = response.status;
      error.code = body?.errorCode;
      error.correlationId = body?.correlationId;
      error.domain = body?.domain;
      throw error;
    }

    return body;
  } catch (error) {
    if (error.name === 'AbortError') {
      const timeoutError = new Error(
        'O banco demorou para responder. Consulte a operação antes de tentar novamente.'
      );
      timeoutError.code = 'NETWORK_TIMEOUT';
      throw timeoutError;
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

async function adminApi(path, options = {}) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), apiTimeoutMs);

  try {
    const token = sessionStorage.getItem(adminAuthTokenKey);
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {})
      }
    });

    const contentType = response.headers.get('content-type') ?? '';
    const isJson =
      contentType.includes('application/json') ||
      contentType.includes('application/problem+json');

    const body = response.status === 204 || !isJson
      ? null
      : await response.json();

    if (!response.ok) {
      const loginRoute = path === '/api/v1/admin/auth/login';

      if (response.status === 401 && !loginRoute) {
        clearAdminSession();
        showPublicArea('admin');
        showApiError(
          document.getElementById('adminLoginError'),
          new Error('Sua sessão gerencial expirou. Entre novamente.')
        );
      }

      const error = new Error(
        body?.detail || 'Não foi possível concluir a operação gerencial.'
      );
      error.status = response.status;
      error.code = body?.errorCode;
      error.correlationId = body?.correlationId;
      error.domain = body?.domain;
      throw error;
    }

    return body;
  } catch (error) {
    if (error.name === 'AbortError') {
      const timeoutError = new Error(
        'O SPBank demorou para responder. Tente novamente.'
      );
      timeoutError.code = 'NETWORK_TIMEOUT';
      throw timeoutError;
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

function showAccessPanel(panel) {
  const loginScreen = document.getElementById('loginScreen');
  const panels = {
    login: document.getElementById('loginPanel'),
    register: document.getElementById('registerPanel'),
    admin: document.getElementById('adminLoginPanel')
  };

  const controls = document.querySelectorAll('[data-public-tab]');
  const managerEntry = document.getElementById('adminLoginTab');
  const clientReturn = document.getElementById('clientAccessBackButton');

  Object.entries(panels).forEach(([name, element]) => {
    if (!element) return;
    const active = name === panel;
    element.classList.toggle('hidden', !active);
    element.setAttribute('aria-hidden', String(!active));
  });

  controls.forEach(control => {
    if (!control.classList.contains('access-tab')) return;
    const active = control.dataset.publicTab === panel;
    control.classList.toggle('active', active);
    control.setAttribute('aria-selected', String(active));
  });

  managerEntry?.classList.toggle('hidden', panel === 'admin');
  clientReturn?.classList.toggle('hidden', panel !== 'admin');

  if (loginScreen) {
    loginScreen.classList.toggle('registration-mode', panel === 'register');
    loginScreen.classList.toggle('admin-access-mode', panel === 'admin');
  }

  ['loginError', 'registerError', 'registerSuccess', 'adminLoginError']
    .forEach(id => clearMessage(document.getElementById(id)));
}

function showPublicArea(panel = 'login') {
  hideRevealedCardData();
  document.getElementById('appShell')?.classList.add('hidden');
  document.getElementById('adminShell')?.classList.add('hidden');
  document.getElementById('loginScreen')?.classList.remove('hidden');
  showAccessPanel(panel);
}

function showLogin(message = '', success = false) {
  showPublicArea('login');
  const errorBox = document.getElementById('loginError');
  if (!errorBox) return;
  errorBox.textContent = message;
  errorBox.classList.toggle('success-message', success);
}

function showAuthenticatedApp(user) {
  currentUser = user;
  document.getElementById('currentUserName').textContent = user.holderName;
  document.getElementById('loginScreen').classList.add('hidden');
  document.getElementById('adminShell').classList.add('hidden');
  document.getElementById('appShell').classList.remove('hidden');
}

async function handleLogin(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const errorBox = document.getElementById('loginError');
  const data = Object.fromEntries(new FormData(form));

  button.disabled = true;
  button.textContent = 'Entrando…';
  clearMessage(errorBox);

  try {
    const session = await api('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(data)
    });

    sessionStorage.setItem(authTokenKey, session.accessToken);
    sessionStorage.setItem(activeContextKey, 'client');

    saldo = 0;
    extrato = [];
    currentAccountSummary = null;
    customerAccounts = [];

    showAuthenticatedApp(session);

    await Promise.all([
      loadCustomerAccounts(),
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    loadPage('dashboard');
    form.reset();
  } catch (error) {
    showApiError(errorBox, error);
  } finally {
    button.disabled = false;
    button.textContent = 'Entrar';
  }
}

async function handleRegistration(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const errorBox = document.getElementById('registerError');
  const successBox = document.getElementById('registerSuccess');

  clearMessage(errorBox);
  clearMessage(successBox);

  const data = Object.fromEntries(new FormData(form));

  if (data.password !== data.passwordConfirmation) {
    showApiError(
      errorBox,
      new Error('A confirmação da senha deve ser igual à senha informada.')
    );
    return;
  }

  const payload = {
    fullName: data.fullName.trim(),
    cpf: data.cpf.replace(/\D/g, ''),
    birthDate: data.birthDate,
    mobile: data.mobile.trim(),
    email: data.email.trim(),
    address: {
      postalCode: data.postalCode.replace(/\D/g, ''),
      street: data.street.trim(),
      number: data.number.trim(),
      complement: data.complement?.trim() ? data.complement.trim() : null,
      district: data.district.trim(),
      city: data.city.trim(),
      state: data.state.trim().toUpperCase()
    },
    username: data.username.trim(),
    password: data.password,
    accountType: data.accountType
  };

  button.disabled = true;
  button.textContent = 'Criando cadastro…';

  try {
    await api('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    const username = data.username.trim();
    form.reset();
    showAccessPanel('login');
    document.getElementById('loginUsername').value = username;
    showLogin('Cadastro realizado com sucesso. Faça seu login.', true);
  } catch (error) {
    showApiError(errorBox, error);
  } finally {
    button.disabled = false;
    button.textContent = 'Criar cadastro';
  }
}

async function performLogout() {
  try {
    await api('/api/v1/auth/logout', { method: 'POST' });
  } catch (_) {
    // A sessão local é encerrada mesmo sem resposta do servidor.
  } finally {
    clearClientSession();
    showLogin();
  }
}

function missingAccountType() {
  const types = new Set(customerAccounts.map(account => account.accountType));
  if (!types.has('CURRENT')) return 'CURRENT';
  if (!types.has('SAVINGS')) return 'SAVINGS';
  return null;
}

function selectedCustomerAccount() {
  return customerAccounts.find(account => account.selected) ?? null;
}

function updateAccountContext() {
  const context = document.getElementById('accountContext');
  const selector = document.getElementById('accountSelector');
  const openButton = document.getElementById('openMissingAccountButton');

  if (!context || !selector || !openButton) return;

  if (customerAccounts.length === 0) {
    context.classList.add('hidden');
    return;
  }

  context.classList.remove('hidden');

  selector.replaceChildren(
    ...customerAccounts.map(account => {
      const option = new Option(
        `${accountTypeLabel(account.accountType)} · Ag. ${account.branch} · ${account.accountNumber}`,
        account.id
      );
      option.selected = Boolean(account.selected);
      return option;
    })
  );

  const missingType = missingAccountType();
  openButton.classList.toggle('hidden', !missingType);

  if (missingType) {
    openButton.textContent = missingType === 'CURRENT'
      ? 'Abrir conta corrente'
      : 'Abrir conta poupança';
  }
}

async function loadCustomerAccounts() {
  customerAccounts = await api('/api/v1/auth/accounts');
  updateAccountContext();
  return customerAccounts;
}

async function handleAccountSelection(event) {
  const selector = event.currentTarget;
  const accountId = selector.value;
  const selected = selectedCustomerAccount();

  if (!accountId || selected?.id === accountId) return;

  selector.disabled = true;

  try {
    await api(`/api/v1/auth/accounts/${accountId}/select`, {
      method: 'POST'
    });

    saldo = 0;
    extrato = [];
    currentAccountSummary = null;

    await Promise.all([
      loadCustomerAccounts(),
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    const user = await api('/api/v1/auth/me');
    showAuthenticatedApp(user);
    loadPage('dashboard');
  } catch (error) {
    await loadCustomerAccounts().catch(() => {});
    const content = document.getElementById('content');

    if (content) {
      content.innerHTML = `
        <div class="card">
          <p id="accountSelectionError"></p>
        </div>
      `;
      showApiError(document.getElementById('accountSelectionError'), error);
    }
  } finally {
    selector.disabled = false;
  }
}

function openMissingAccountDialog() {
  const type = missingAccountType();
  if (!type) return;

  const dialog = document.getElementById('openAccountDialog');
  const select = document.getElementById('newAccountType');

  clearMessage(document.getElementById('openAccountError'));
  select.replaceChildren(new Option(accountTypeLabel(type), type));
  dialog.showModal();
}

async function handleOpenAccount(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const errorBox = document.getElementById('openAccountError');
  const data = Object.fromEntries(new FormData(form));

  clearMessage(errorBox);
  button.disabled = true;
  button.textContent = 'Abrindo conta…';

  try {
    const account = await api('/api/v1/auth/accounts', {
      method: 'POST',
      body: JSON.stringify({ accountType: data.accountType })
    });

    await api(`/api/v1/auth/accounts/${account.id}/select`, {
      method: 'POST'
    });

    document.getElementById('openAccountDialog').close();

    saldo = 0;
    extrato = [];
    currentAccountSummary = null;

    await Promise.all([
      loadCustomerAccounts(),
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    loadPage('dashboard');
  } catch (error) {
    showApiError(errorBox, error);
  } finally {
    button.disabled = false;
    button.textContent = 'Abrir conta';
  }
}

async function loadAccountSummary() {
  const summary = await api('/api/v1/accounts/me/summary');
  currentAccountSummary = summary;
  saldo = Number(summary.balance);

  const element = document.getElementById('transferBalance');
  if (element) element.textContent = formatarMoeda(saldo);

  return summary;
}

async function loadBanks() {
  const banks = await api('/api/v1/banks');
  const select = document.querySelector('[name="bankCode"]');
  if (!select) return;

  const placeholder = new Option('Selecione a instituição', '');
  placeholder.disabled = true;
  placeholder.selected = true;

  select.replaceChildren(
    placeholder,
    ...banks.map(bank => new Option(`${bank.code} — ${bank.name}`, bank.code))
  );
}

function renderFeaturePlaceholder(title, description) {
  const content = document.getElementById('content');

  content.innerHTML = `
    <div class="card highlight">
      <h2>${escapeHtml(title)}</h2>
      <p>${escapeHtml(description)}</p>
    </div>

    <div class="card">
      <h3>Módulo em adaptação</h3>
      <p>
        Esta área será conectada futuramente à API oficial do SPBank.
        Por enquanto, saldo, extrato e movimentações oficiais
        permanecem centralizados no backend.
      </p>

      <div class="actions">
        <button class="btn" type="button" onclick="loadPage('dashboard')">
          Voltar ao dashboard
        </button>
      </div>
    </div>
  `;
}

function renderDashboard() {
  updateNavigation("dashboard");
  const content = document.getElementById('content');
  const recentEntries = extrato
    .slice()
    .sort((a, b) => new Date(b.data) - new Date(a.data))
    .slice(0, 5);

  const selectedAccount = selectedCustomerAccount();
  const missingType = missingAccountType();

  content.innerHTML = `
    <div class="header">
      <div>
        <h2>Dashboard</h2>
        <p class="summary-context">
          Dados oficiais da conta atualmente selecionada.
        </p>
      </div>
    </div>

    <div class="grid-3">
      <div class="card highlight">
        <p>Saldo disponível</p>
        <h2>${formatarMoeda(saldo)}</h2>
      </div>

      <div class="card">
        <p>Plano atual</p>
        <h2>
          ${escapeHtml(customerPlanLabel(currentAccountSummary?.customerPlan))}
        </h2>
      </div>

      <div class="card">
        <p>Modalidade</p>
        <h2>${escapeHtml(accountTypeLabel(selectedAccount?.accountType))}</h2>
      </div>
    </div>

    ${selectedAccount ? `
      <div class="card account-summary-card">
        <div class="header">
          <div>
            <h3>Conta ativa</h3>
            <p class="summary-context">
              ${escapeHtml(accountTypeLabel(selectedAccount.accountType))}
            </p>
          </div>

          ${missingType ? `
            <button
              class="btn-outline btn-compact"
              type="button"
              onclick="openMissingAccountDialog()"
            >
              ${missingType === 'CURRENT'
                ? 'Abrir conta corrente'
                : 'Abrir conta poupança'}
            </button>
          ` : ''}
        </div>

        <div class="statement-details">
          <div class="statement-detail">
            <span>Agência</span>
            <strong>${escapeHtml(selectedAccount.branch)}</strong>
          </div>

          <div class="statement-detail">
            <span>Conta</span>
            <strong>${escapeHtml(selectedAccount.accountNumber)}</strong>
          </div>

          <div class="statement-detail">
            <span>Saldo</span>
            <strong>${formatarMoeda(selectedAccount.balance)}</strong>
          </div>
        </div>
      </div>
    ` : ''}

    <div class="menu-grid">
      <div class="menu-item" onclick="loadPage('pix')">
        <div class="icon">
          <span class="material-symbols-outlined">window</span>
        </div>
        <span>Pix</span>
      </div>

      <div class="menu-item" onclick="loadPage('transfer')">
        <div class="icon">
          <span class="material-symbols-outlined">sync_alt</span>
        </div>
        <span>Transferência</span>
      </div>

      <div class="menu-item" onclick="loadPage('extrato')">
        <div class="icon">
          <span class="material-symbols-outlined">receipt</span>
        </div>
        <span>Extrato</span>
      </div>

      <div class="menu-item" onclick="loadPage('profile')">
        <div class="icon">
          <span class="material-symbols-outlined">manage_accounts</span>
        </div>
        <span>Meus dados</span>
      </div>
    </div>

    <div class="card">
      <div class="header">
        <h3>Transações recentes</h3>
        <button
          class="btn-outline btn-compact"
          type="button"
          onclick="loadPage('extrato')"
        >
          Ver extrato
        </button>
      </div>

      ${recentEntries.length === 0
        ? '<p>Nenhuma movimentação registrada.</p>'
        : recentEntries.map(item => `
            <div class="row">
              <span>${escapeHtml(item.tipo)}</span>
              <strong class="${item.valor < 0 ? 'saida' : 'entrada'}">
                ${formatarMoeda(item.valor)}
              </strong>
            </div>
          `).join('')}
    </div>
  `;
  organizeDashboardWorkspace();
}

function renderTransferPage() {
  const content = document.getElementById('content');

  content.innerHTML = `
    <div class="card highlight">
      <h2>Transferência</h2>
      <p>
        Saldo disponível:
        <strong id="transferBalance">carregando…</strong>
      </p>
    </div>

    <form id="transferForm" class="card">
      <label>
        Instituição de destino
        <select name="bankCode" required>
          <option value="">Carregando instituições…</option>
        </select>
      </label>

      <h3>Conta de destino</h3>

      <input
        name="recipientName"
        required
        maxlength="120"
        placeholder="Nome completo"
      >

      <input
        name="recipientDocument"
        required
        inputmode="numeric"
        placeholder="CPF ou CNPJ (somente números)"
      >

      <div class="grid-2">
        <input name="branch" required placeholder="Agência">
        <input name="accountNumber" required placeholder="Conta">
      </div>

      <select name="accountType" required>
        <option value="CURRENT">Conta corrente (CC)</option>
        <option value="SAVINGS">Conta poupança (CP)</option>
      </select>

      <input
        name="amount"
        type="number"
        min="0.01"
        step="0.01"
        required
        placeholder="Valor"
      >

      <label>
        Agendar (opcional)
        <input name="scheduledFor" type="date">
      </label>

      <div id="transferError" class="error-message" role="alert"></div>

      <button class="btn" type="submit">
        Revisar transferência
      </button>
    </form>

    <section class="card" aria-labelledby="scheduledTitle">
      <h3 id="scheduledTitle">Transferências agendadas</h3>
      <p id="scheduledMessage" class="operation-message" role="status"></p>
      <div id="scheduledTransfers">Carregando agendamentos…</div>
    </section>

    <dialog
      id="transferReview"
      class="transfer-dialog"
      aria-labelledby="reviewTitle"
    >
      <h2 id="reviewTitle">Revise antes de confirmar</h2>

      <dl class="review-list">
        <dt>Destinatário</dt><dd id="reviewRecipient"></dd>
        <dt>Instituição</dt><dd id="reviewInstitution"></dd>
        <dt>Modalidade</dt><dd id="reviewType"></dd>
        <dt>Conta</dt><dd id="reviewAccount"></dd>
        <dt>Valor</dt><dd id="reviewAmount"></dd>
        <dt>Tarifa</dt><dd id="reviewFee"></dd>
        <dt>Total</dt><dd id="reviewTotal"></dd>
        <dt>Data efetiva</dt><dd id="reviewDate"></dd>
      </dl>

      <label class="confirmation-field">
        Confirme com sua senha
        <input
          id="transferConfirmationPassword"
          type="password"
          autocomplete="current-password"
          required
        >
      </label>

      <div id="reviewError" class="error-message" role="alert"></div>

      <div class="dialog-actions">
        <button id="cancelTransfer" class="btn-secondary" type="button">
          Voltar
        </button>

        <button id="confirmTransfer" class="btn" type="button">
          Confirmar transferência
        </button>
      </div>
    </dialog>
  `;

  organizeTransferWorkspace();
  document.getElementById('transferForm')
    .addEventListener('submit', submitTransfer);

  document.getElementById('cancelTransfer')
    .addEventListener('click', cancelReview);

  document.getElementById('confirmTransfer')
    .addEventListener('click', confirmTransfer);

  Promise.all([
    loadAccountSummary(),
    loadBanks(),
    loadScheduledTransfers()
  ]).catch(error =>
    showApiError(document.getElementById('transferError'), error)
  );
}

async function submitTransfer(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const errorBox = document.getElementById('transferError');
  const data = Object.fromEntries(new FormData(form));

  const payload = {
    ...data,
    recipientDocument: data.recipientDocument.replace(/\D/g, ''),
    amount: Number(data.amount),
    scheduledFor: data.scheduledFor || null
  };

  button.disabled = true;
  button.textContent = 'Calculando…';
  clearMessage(errorBox);

  try {
    const preview = await api('/api/v1/transfers/preview', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    pendingTransfer = payload;
    pendingIdempotencyKey = crypto.randomUUID();

    setText(
      'reviewRecipient',
      `${payload.recipientName} — ${payload.recipientDocument}`
    );
    setText('reviewInstitution', preview.institutionName);
    setText('reviewType', transferTypeLabels[preview.type] || preview.type);
    setText(
      'reviewAccount',
      `${payload.bankCode} / ${payload.branch} / ${payload.accountNumber}`
    );
    setText('reviewAmount', formatarMoeda(preview.amount));
    setText('reviewFee', formatarMoeda(preview.fee));
    setText('reviewTotal', formatarMoeda(preview.total));
    setText('reviewDate', preview.effectiveDate);

    document.getElementById('transferConfirmationPassword').value = '';
    clearMessage(document.getElementById('reviewError'));
    document.getElementById('transferReview').showModal();
    document.getElementById('transferConfirmationPassword').focus();
  } catch (error) {
    showApiError(errorBox, error);
  } finally {
    button.disabled = false;
    button.textContent = 'Revisar transferência';
  }
}

async function confirmTransfer() {
  if (!pendingTransfer || !pendingIdempotencyKey) return;

  const button = document.getElementById('confirmTransfer');
  const reviewError = document.getElementById('reviewError');
  const passwordInput = document.getElementById('transferConfirmationPassword');
  const confirmationPassword = passwordInput.value;

  if (!confirmationPassword) {
    showApiError(
      reviewError,
      new Error('Informe sua senha para confirmar a transferência.')
    );
    passwordInput.focus();
    return;
  }

  button.disabled = true;
  button.textContent = 'Processando…';
  clearMessage(reviewError);

  try {
    const transfer = await api('/api/v1/transfers', {
      method: 'POST',
      headers: { 'Idempotency-Key': pendingIdempotencyKey },
      body: JSON.stringify({
        transfer: pendingTransfer,
        confirmationPassword
      })
    });

    document.getElementById('transferReview').close();
    passwordInput.value = '';
    pendingTransfer = null;
    pendingIdempotencyKey = null;

    renderTransferReceipt(transfer);

    await Promise.all([
      loadAccountSummary(),
      loadEntries(),
      loadScheduledTransfers(),
      loadCustomerAccounts()
    ]);
  } catch (error) {
    passwordInput.value = '';
    showApiError(reviewError, error);
    passwordInput.focus();
  } finally {
    button.disabled = false;
    button.textContent = 'Confirmar transferência';
  }
}

function cancelReview() {
  const dialog = document.getElementById('transferReview');
  const passwordInput = document.getElementById('transferConfirmationPassword');

  if (passwordInput) passwordInput.value = '';
  pendingTransfer = null;
  pendingIdempotencyKey = null;
  dialog.close();
}

async function loadScheduledTransfers() {
  const container = document.getElementById('scheduledTransfers');
  if (!container) return;

  const transfers = await api('/api/v1/transfers/scheduled');

  if (transfers.length === 0) {
    container.innerHTML = '<p>Nenhuma transferência agendada.</p>';
    return;
  }

  container.innerHTML = `
    <div class="table-responsive">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Destinatário</th>
            <th>Modalidade</th>
            <th>Valor</th>
            <th>Ação</th>
          </tr>
        </thead>
        <tbody>
          ${transfers.map(transfer => `
            <tr>
              <td>${escapeHtml(formatDate(transfer.scheduledFor))}</td>
              <td>${escapeHtml(transfer.recipientName)}</td>
              <td>
                ${escapeHtml(
                  transferTypeLabels[transfer.type] || transfer.type
                )}
              </td>
              <td>${formatarMoeda(transfer.amount)}</td>
              <td>
                <button
                  class="scheduled-cancel"
                  type="button"
                  data-cancel-scheduled="${escapeHtml(transfer.id)}"
                >
                  Cancelar
                </button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;

  container.querySelectorAll('[data-cancel-scheduled]').forEach(button => {
    button.addEventListener('click', () => cancelScheduledTransfer(button));
  });
}

async function cancelScheduledTransfer(button) {
  const id = button.dataset.cancelScheduled;
  const message = document.getElementById('scheduledMessage');

  button.disabled = true;
  message.textContent = 'Cancelando agendamento…';

  try {
    await api(`/api/v1/transfers/${id}`, { method: 'DELETE' });
    message.textContent = 'Transferência agendada cancelada.';
    await loadScheduledTransfers();
  } catch (error) {
    showApiError(message, error);
    button.disabled = false;
  }
}

function renderTransferReceipt(transfer) {
  const content = document.getElementById('content');

  content.innerHTML = `
    <section class="card receipt" aria-labelledby="receiptTitle">
      <h2 id="receiptTitle">Transferência registrada</h2>
      <p>Status: <strong id="receiptStatus"></strong></p>

      <dl class="review-list">
        <dt>Identificador</dt><dd id="receiptId"></dd>
        <dt>Destinatário</dt><dd id="receiptRecipient"></dd>
        <dt>Conta</dt><dd id="receiptAccount"></dd>
        <dt>Modalidade</dt><dd id="receiptType"></dd>
        <dt>Valor</dt><dd id="receiptAmount"></dd>
        <dt>Tarifa</dt><dd id="receiptFee"></dd>
        <dt>Data agendada</dt><dd id="receiptDate"></dd>
        <dt>Solicitada em</dt><dd id="receiptRequestedAt"></dd>
        <dt>Processada em</dt><dd id="receiptProcessedAt"></dd>

        ${transfer.type === 'TED'
          ? '<dt>Liquidação</dt><dd id="receiptSettlement"></dd>'
          : ''}
      </dl>

      <p>
        Saldo atualizado:
        <strong id="transferBalance">carregando…</strong>
      </p>

      <button id="newTransfer" class="btn" type="button">
        Nova transferência
      </button>
    </section>

    <section class="card">
      <h2>Lançamentos recentes</h2>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th>Data/hora</th>
              <th>Descrição</th>
              <th>Para/de</th>
              <th>Modalidade</th>
              <th>Movimento</th>
              <th>Valor</th>
              <th>Saldo após</th>
            </tr>
          </thead>
          <tbody id="recentEntries"></tbody>
        </table>
      </div>
    </section>
  `;

  organizeTransferReceiptWorkspace();
  setText('receiptStatus', transferStatusLabels[transfer.status] || transfer.status);
  setText('receiptId', transfer.id);
  setText('receiptRecipient', transfer.recipientName);
  setText(
    'receiptAccount',
    `${transfer.bankCode} / ${transfer.branch} / ${transfer.accountNumber}`
  );
  setText('receiptType', transferTypeLabels[transfer.type] || transfer.type);
  setText('receiptAmount', formatarMoeda(transfer.amount));
  setText('receiptFee', formatarMoeda(transfer.fee));
  setText(
    'receiptDate',
    transfer.scheduledFor ? formatDate(transfer.scheduledFor) : 'Imediata'
  );
  setText('receiptRequestedAt', formatDateTime(transfer.requestedAt));
  setText(
    'receiptProcessedAt',
    transfer.processedAt ? formatDateTime(transfer.processedAt) : 'Aguardando'
  );

  if (transfer.type === 'TED') {
    setText(
      'receiptSettlement',
      transfer.settlementReference
        ? `${transfer.settlementReference} (ambiente simulado)`
        : 'Aguardando processamento'
    );
  }

  document.getElementById('newTransfer')
    .addEventListener('click', () => loadPage('transfer'));
}

async function loadEntries() {
  const tbody = document.getElementById('recentEntries');
  const entries = await api('/api/v1/accounts/me/entries?limit=20');

  replaceOfficialEntries(entries);

  if (!tbody) return;
  tbody.replaceChildren();

  for (const entry of entries) {
    const row = document.createElement('tr');
    const values = [
      formatDateTime(entry.occurredAt),
      entry.description,
      entry.counterpartyName || '—',
      transferModalityLabel(entry.operationType),
      entry.direction === 'DEBIT' ? 'Débito' : 'Crédito',
      formatarMoeda(entry.amount),
      formatarMoeda(entry.balanceAfter)
    ];

    for (const value of values) {
      const cell = document.createElement('td');
      cell.textContent = value;
      row.appendChild(cell);
    }

    tbody.appendChild(row);
  }
}

function replaceOfficialEntries(entries) {
  extrato = entries.map(entry => ({
    id: entry.id,
    source: 'spbank-api',
    category: entry.referenceType === 'PAYMENT' ? 'payment' : entry.referenceType === 'INVESTMENT' ? 'investment' : entry.referenceType === 'PIX' ? 'pix' : entry.referenceType === 'CARD' ? 'card' : 'transfer',
    tipo: entry.type === 'FEE'
      ? 'Tarifa de transferência'
      : entry.description,
    descricao: entry.direction === 'DEBIT'
      ? 'Débito em conta'
      : 'Crédito em conta',
    valor: (entry.direction === 'DEBIT' ? -1 : 1) * Number(entry.amount),
    data: entry.occurredAt,
    direction: entry.direction,
    entryType: entry.type,
    counterpartyName: entry.counterpartyName,
    counterpartyBankCode: entry.counterpartyBankCode,
    operationType: entry.operationType,
    balanceAfter: Number(entry.balanceAfter),
    referenceId: entry.referenceId
  }));
}

async function refreshOfficialStatement() {
  const entries = await api('/api/v1/accounts/me/entries?limit=100');
  replaceOfficialEntries(entries);
  return entries;
}

function filtrarExtrato(tipo, botao) {
  filtroAtual = tipo;

  document.querySelectorAll('.filtro-btn').forEach(btn => {
    btn.classList.remove('ativo');
    btn.setAttribute('aria-pressed','false');
  });

  if (botao) botao.classList.add('ativo');

  renderExtrato();
  renderStatementSummary();
}

function atualizarFiltroAtivo() {
  const mapa = { todos: 'btnTodos', transfer: 'btnTransfer', payment: 'btnPayment', pix: 'btnPix', investment: 'btnInvestment', card: 'btnCard' };

  document.querySelectorAll('.filtro-btn').forEach(btn => {
    btn.classList.remove('ativo');
  });

  const ativo = document.getElementById(mapa[filtroAtual]);
  if (ativo) { ativo.classList.add('ativo'); ativo.setAttribute('aria-pressed','true'); }
}

function getFilteredStatementEntries() {
  return extrato
    .slice()
    .sort((a, b) => new Date(b.data) - new Date(a.data))
    .filter(item => {
      if (filtroAtual === 'todos') return true;
      if (['transfer','payment','pix','investment','card'].includes(filtroAtual)) return item.category === filtroAtual;
      return false;
    });
}

function renderStatementSummary() {
  const movimentos = getFilteredStatementEntries();
  let entradas = 0;
  let saidas = 0;

  movimentos.forEach(item => {
    if (item.valor >= 0) entradas += Number(item.valor);
    else saidas += Math.abs(Number(item.valor));
  });

  renderResumoValores(entradas, saidas);

  const contexto = document.getElementById('resumoContexto');
  if (contexto) {
    contexto.textContent = filtroAtual === 'todos'
      ? 'Todas as movimentações carregadas'
      : ({transfer:'Somente transferências',payment:'Pagamentos e recargas',pix:'Somente Pix',investment:'Somente investimentos',card:'Somente cartão'}[filtroAtual]||'Movimentações filtradas');
  }
}

function renderResumoValores(entradas, saidas) {
  const resultado = entradas - saidas;
  const resultadoClasse = resultado < 0 ? 'saida' : 'resultado';
  const resumo = document.getElementById('resumoValores');

  if (!resumo) return;

  resumo.innerHTML = `
    <div class="resumo-item">
      <h4>Entradas no filtro</h4>
      <strong class="entrada">${formatarMoeda(entradas)}</strong>
    </div>

    <div class="resumo-item">
      <h4>Saídas no filtro</h4>
      <strong class="saida">${formatarMoeda(saidas)}</strong>
    </div>

    <div class="resumo-item">
      <h4>Resultado no filtro</h4>
      <strong class="${resultadoClasse}">${formatarMoeda(resultado)}</strong>
    </div>

    <div class="resumo-item">
      <h4>Saldo atual</h4>
      <strong>${formatarMoeda(saldo)}</strong>
    </div>
  `;
}

function renderExtrato() {
  const lista = getFilteredStatementEntries();
  const element = document.getElementById('listaExtrato');

  if (!element) return;

  if (lista.length === 0) {
    element.innerHTML = `
      <div class="card">
        <p>Nenhuma movimentação.</p>
      </div>
    `;
    return;
  }

  element.innerHTML = lista.map(item => {
    const counterpartyLabel = item.entryType === 'FEE'
      ? 'Relacionado a'
      : item.direction === 'CREDIT'
        ? 'Recebido de'
        : 'Enviado para';

    return `
      <div class="card statement-card">
        <div class="row">
          <strong>${escapeHtml(item.tipo)}</strong>
          <span class="${item.valor < 0 ? 'saida' : 'entrada'}">
            ${formatarMoeda(item.valor)}
          </span>
        </div>

        <div class="statement-details">
          <div class="statement-detail">
            <span>Data e hora</span>
            <strong>${escapeHtml(formatDateTime(item.data))}</strong>
          </div>

          <div class="statement-detail">
            <span>${counterpartyLabel}</span>
            <strong>${escapeHtml(item.counterpartyName)}</strong>
          </div>

          <div class="statement-detail">
            <span>Banco</span>
            <strong>${escapeHtml(item.counterpartyBankCode)}</strong>
          </div>

          <div class="statement-detail">
            <span>Modalidade</span>
            <strong>
              ${escapeHtml(transferModalityLabel(item.operationType))}
            </strong>
          </div>

          <div class="statement-detail">
            <span>Natureza</span>
            <strong>${item.direction === 'DEBIT' ? 'Débito' : 'Crédito'}</strong>
          </div>

          <div class="statement-detail">
            <span>Saldo após</span>
            <strong>${formatarMoeda(item.balanceAfter)}</strong>
          </div>

          <div class="statement-detail">
            <span>Identificador</span>
            <strong>${escapeHtml(item.referenceId)}</strong>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function renderExtratoPage() {
  const content = document.getElementById('content');

  content.innerHTML = `
    <div class="card highlight">
      <h2>Extrato</h2>
      <p>Movimentações oficiais da conta selecionada.</p>
    </div>

    <div class="card statement-toolbar">
      <button
        id="btnTodos"
        class="filtro-btn ativo"
        type="button"
        onclick="filtrarExtrato('todos', this)"
      >
        Todos
      </button>

      <button
        id="btnTransfer"
        class="filtro-btn"
        type="button"
        onclick="filtrarExtrato('transfer', this)"
      >
        Transferências
      </button>
<button id="btnPayment" class="filtro-btn" type="button" onclick="filtrarExtrato('payment',this)">Pagamentos</button>
<button id="btnPix" class="filtro-btn" type="button" onclick="filtrarExtrato('pix',this)">Pix</button>
<button id="btnInvestment" class="filtro-btn" type="button" onclick="filtrarExtrato('investment',this)">Investimentos</button>
<button id="btnCard" class="filtro-btn" type="button" onclick="filtrarExtrato('card',this)">Cartão</button>
    </div>

    <div class="card">
      <h3>Resumo do extrato</h3>
      <p id="resumoContexto" class="summary-context"></p>
      <div id="resumoValores" class="resumo-valores"></div>
    </div>

    <div id="listaExtrato"></div>
  `;

  organizeStatementWorkspace();
  atualizarFiltroAtivo();
  renderExtrato();
  renderStatementSummary();

  Promise.all([
    loadAccountSummary(),
    refreshOfficialStatement()
  ]).then(() => {
    if (document.getElementById('listaExtrato')) {
      renderExtrato();
      renderStatementSummary();
    }
  }).catch(error => {
    const lista = document.getElementById('listaExtrato');
    if (!lista) return;

    lista.innerHTML = `
      <div class="card">
        <p id="statementError"></p>
      </div>
    `;

    showApiError(document.getElementById('statementError'), error);
  });
}

function renderProfilePage() {
  const content = document.getElementById('content');

  content.innerHTML = `
    <div class="card highlight">
      <h2>Meus dados</h2>
      <p>
        Consulte seus dados cadastrais e mantenha
        contato e endereço atualizados.
      </p>
    </div>

    <form id="profileForm" class="card">
      <div class="header">
        <div>
          <h3>Dados cadastrais</h3>
          <p class="summary-context">
            Nome, CPF e nascimento identificam o cliente
            e não podem ser alterados.
          </p>
        </div>
      </div>

      <fieldset class="form-section">
        <legend>Identificação</legend>

        <div class="grid-2">
          <label>
            Nome completo
            <input id="profileFullName" disabled>
          </label>

          <label>
            CPF
            <input id="profileCpf" disabled>
          </label>
        </div>

        <label>
          Data de nascimento
          <input id="profileBirthDate" type="date" disabled>
        </label>
      </fieldset>

      <fieldset class="form-section">
        <legend>Contato</legend>

        <div class="grid-2">
          <label>
            Celular
            <input id="profileMobile" name="mobile" type="tel" required>
          </label>

          <label>
            E-mail
            <input id="profileEmail" name="email" type="email" required>
          </label>
        </div>
      </fieldset>

      <fieldset class="form-section">
        <legend>Endereço</legend>

        <div class="grid-2">
          <label>
            CEP
            <input id="profilePostalCode" name="postalCode" required>
          </label>

          <label>
            Estado
            <input
              id="profileState"
              name="state"
              maxlength="2"
              required
            >
          </label>
        </div>

        <label>
          Rua
          <input id="profileStreet" name="street" required>
        </label>

        <div class="grid-2">
          <label>
            Número
            <input id="profileNumber" name="number" required>
          </label>

          <label>
            Complemento
            <input id="profileComplement" name="complement">
          </label>
        </div>

        <div class="grid-2">
          <label>
            Bairro
            <input id="profileDistrict" name="district" required>
          </label>

          <label>
            Cidade
            <input id="profileCity" name="city" required>
          </label>
        </div>
      </fieldset>

      <div id="profileMessage" class="error-message" role="status"></div>

      <button class="btn" type="submit">
        Salvar alterações
      </button>
    </form>

    <form id="passwordForm" class="card">
      <div class="header">
        <div>
          <h3>Alterar senha</h3>
          <p class="summary-context">
            Confirme sua senha atual antes de definir uma nova.
          </p>
        </div>
      </div>

      <label>
        Senha atual
        <input
          name="currentPassword"
          type="password"
          autocomplete="current-password"
          required
        >
      </label>

      <div class="grid-2">
        <label>
          Nova senha
          <input
            name="newPassword"
            type="password"
            minlength="8"
            maxlength="72"
            autocomplete="new-password"
            required
          >
        </label>

        <label>
          Confirmar nova senha
          <input
            name="newPasswordConfirmation"
            type="password"
            minlength="8"
            maxlength="72"
            autocomplete="new-password"
            required
          >
        </label>
      </div>

      <div id="passwordMessage" class="error-message" role="status"></div>

      <button class="btn" type="submit">
        Alterar senha
      </button>
    </form>
  `;

  organizeProfileWorkspace();
  document.getElementById('profileForm')
    .addEventListener('submit', handleProfileUpdate);

  document.getElementById('passwordForm')
    .addEventListener('submit', handlePasswordChange);

  loadProfile().catch(error=>{const message=document.getElementById('profileMessage');if(message)showApiError(message,error);});
}

async function loadProfile() {
  const profile = await api('/api/v1/auth/profile');
  if(!document.getElementById('profileForm'))return profile;

  document.getElementById('profileFullName').value = profile.fullName ?? '';
  document.getElementById('profileCpf').value = formatCpf(profile.cpf);
  document.getElementById('profileBirthDate').value = profile.birthDate ?? '';
  document.getElementById('profileMobile').value = profile.mobile ?? '';
  document.getElementById('profileEmail').value = profile.email ?? '';

  document.getElementById('profilePostalCode').value =
    profile.address?.postalCode ?? '';

  document.getElementById('profileStreet').value =
    profile.address?.street ?? '';

  document.getElementById('profileNumber').value =
    profile.address?.number ?? '';

  document.getElementById('profileComplement').value =
    profile.address?.complement ?? '';

  document.getElementById('profileDistrict').value =
    profile.address?.district ?? '';

  document.getElementById('profileCity').value =
    profile.address?.city ?? '';

  document.getElementById('profileState').value =
    profile.address?.state ?? '';

  return profile;
}

async function handleProfileUpdate(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const message = document.getElementById('profileMessage');
  const data = Object.fromEntries(new FormData(form));

  const payload = {
    mobile: data.mobile.trim(),
    email: data.email.trim(),
    address: {
      postalCode: data.postalCode.replace(/\D/g, ''),
      street: data.street.trim(),
      number: data.number.trim(),
      complement: data.complement?.trim() ? data.complement.trim() : null,
      district: data.district.trim(),
      city: data.city.trim(),
      state: data.state.trim().toUpperCase()
    }
  };

  clearMessage(message);
  button.disabled = true;
  button.textContent = 'Salvando…';

  try {
    await api('/api/v1/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(payload)
    });

    await loadProfile();
    showSuccess(message, 'Dados atualizados com sucesso.');
  } catch (error) {
    showApiError(message, error);
  } finally {
    button.disabled = false;
    button.textContent = 'Salvar alterações';
  }
}

async function handlePasswordChange(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const message = document.getElementById('passwordMessage');
  const data = Object.fromEntries(new FormData(form));

  clearMessage(message);

  if (data.newPassword !== data.newPasswordConfirmation) {
    showApiError(
      message,
      new Error('A confirmação da nova senha deve ser igual à nova senha.')
    );
    return;
  }

  button.disabled = true;
  button.textContent = 'Alterando…';

  try {
    await api('/api/v1/auth/password', {
      method: 'PUT',
      body: JSON.stringify({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
        newPasswordConfirmation: data.newPasswordConfirmation
      })
    });

    showSuccess(message, 'Senha alterada com sucesso.');
  } catch (error) {
    showApiError(message, error);
  } finally {
    form.reset();
    button.disabled = false;
    button.textContent = 'Alterar senha';
  }
}

async function renderCardAllocationPage() {
  const content = document.getElementById('content');
  content.innerHTML = '<section id="creditAllocationPage" class="card" aria-live="polite">Carregando crédito…</section><section id="cardInstruments" class="card" aria-live="polite">Carregando cartões…</section><section id="cardFinance" class="card" aria-live="polite">Carregando faturas…</section>';
  organizeCardWorkspace();
  renderCardInstruments(document.getElementById('cardInstruments'));
  renderCardFinance(document.getElementById('cardFinance'));
  const page = document.getElementById('creditAllocationPage');
  try {
    const [overview, allocation] = await Promise.all([
      api('/api/v1/cards/credit'),
      api('/api/v1/cards/credit/allocation')
    ]);
    if (!page.isConnected) return;
    page.innerHTML = `
      <h2>Distribuição do crédito</h2>
      <p>Plano ${escapeHtml(overview.customerPlan)} · Teto: ${formatarMoeda(overview.ceiling)}</p>
      <p>Aprovado: <strong>${formatarMoeda(allocation.approvedTotal)}</strong></p>
      <p>Distribuído: <strong>${formatarMoeda(allocation.allocatedTotal)}</strong> ·
        Não distribuído: <strong>${formatarMoeda(allocation.unallocatedTotal)}</strong></p>
      <p>Informe o limite total desejado para cada bandeira e confirme com sua senha de login.</p>
      <form id="creditAllocationForm">
        <fieldset>
          <legend>Limites por bandeira</legend>
          ${allocation.lines.map(line => `
            <label>${cardBrandLogo(line.brand)} ${escapeHtml(line.brand)}
              <input type="number" name="${escapeHtml(line.brand.toLowerCase())}"
                min="0" step="0.01" required value="${escapeHtml(line.allocated)}">
            </label>
            <p>Comprometido: ${formatarMoeda(line.committed)} ·
              Disponível: ${formatarMoeda(line.available)} · Excesso: ${formatarMoeda(line.excess)}</p>
          `).join('')}
          <label>Senha de login
            <input type="password" name="confirmationPassword" autocomplete="current-password"
              maxlength="128" required>
          </label>
          <button type="submit" class="btn">Salvar distribuição</button>
        </fieldset>
      </form>
      <div id="creditAllocationMessage" role="status" aria-live="polite"></div>
    `;
    decorateCreditAllocation(page,overview,allocation);
    page.querySelector('form').addEventListener('submit', handleCreditAllocation);
  } catch (error) {
    if (page.isConnected) {
      page.innerHTML = '<h2>Distribuição do crédito</h2><div role="alert"></div>';
      showApiError(page.querySelector('[role="alert"]'), error);
    }
  }
}

async function handleCreditAllocation(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = form.parentElement.querySelector('#creditAllocationMessage');
  const fieldset = form.querySelector('fieldset');
  const password = form.elements.confirmationPassword;
  const values = Object.fromEntries(new FormData(form));
  const intention = JSON.stringify({ brisa: values.brisa, mistershard: values.mistershard, elu: values.elu });
  // Preserve the key across uncertain responses; never retain the password with it.
  if (form.pendingAllocation?.intention !== intention) {
    form.pendingAllocation = { intention, key: crypto.randomUUID() };
  }
  const body = JSON.stringify({ ...JSON.parse(intention), confirmationPassword: values.confirmationPassword });
  password.value = '';
  clearMessage(message);
  fieldset.disabled = true;
  try {
    await api('/api/v1/cards/credit/allocation', {
      method: 'PUT', headers: { 'Idempotency-Key': form.pendingAllocation.key }, body
    });
    form.pendingAllocation = null;
    if (!form.isConnected) return;
    // Read current state after a possible replay of an older operation.
    await renderCardAllocationPage();
    const currentMessage = document.getElementById('creditAllocationMessage');
    if (currentMessage) showSuccess(currentMessage, 'Distribuição salva com sucesso.');
  } catch (error) {
    if (form.isConnected) showApiError(message, error);
  } finally {
    password.value = '';
    fieldset.disabled = false;
  }
}

function hideRevealedCardData() {
  cardRevealEpoch++;
  document.querySelectorAll('.card-sensitive').forEach(element => element.replaceChildren());
  document.querySelectorAll('[data-card-form] input[type="password"], [data-finance-action] input[type="password"], #investmentForm input[type="password"], [data-financial-form] input[type="password"]').forEach(input => { input.value = ''; });
}

function cardSecretInput(name, label, pin = false) {
  return `<label>${escapeHtml(label)}<input type="password" name="${name}" required
    autocomplete="off" ${pin ? 'inputmode="numeric" pattern="[0-9]{4}" minlength="4" maxlength="4"' : 'maxlength="128"'}></label>`;
}

function cardActionForm(cardId, path, label, fields = '', method = 'POST') {
  return `<form data-card-form data-card-id="${escapeHtml(cardId)}" data-path="${path}" data-method="${method}">
    <fieldset><legend>${escapeHtml(label)}</legend>${fields}
    <button type="submit" class="btn-outline">${escapeHtml(label)}</button></fieldset>
    <div data-card-message role="status"></div></form>`;
}

async function renderCardInstruments(container) {
  try {
    const cards = await api('/api/v1/cards');
    if (!container.isConnected) return;
    container.innerHTML = `<h2>Meus cartões</h2>
      <form data-card-form data-path="" data-method="POST"><fieldset><legend>Emitir cartão</legend>
        <label>Bandeira<select name="brand" required><option value="BRISA">Brisa</option>
          <option value="MISTERSHARD">Mistershard</option><option value="ELU">Elu</option></select></label>
        <label>Vencimento<select name="dueDay" required><option>5</option><option>10</option><option>15</option><option>20</option><option>25</option></select></label>
        <button type="submit" class="btn">Emitir cartão</button></fieldset><div data-card-message role="status"></div></form>
      ${cards.length ? cards.map(card => {
        let actions = '';
        if (card.state === 'PENDING_ACTIVATION') {
          actions += cardActionForm(card.id, 'activate', 'Ativar cartão',
            cardSecretInput('confirmationPassword', 'Senha de login') + cardSecretInput('pin', 'Criar PIN de quatro dígitos', true)
              + cardSecretInput('pinConfirmation', 'Confirmar PIN', true));
        }
        if (card.state === 'ACTIVE') {
          actions += cardActionForm(card.id, 'details', 'Revelar dados', cardSecretInput('pin', 'PIN', true));
          actions += cardActionForm(card.id, 'pin', 'Alterar PIN', cardSecretInput('currentPin', 'PIN atual', true)
            + cardSecretInput('newPin', 'Novo PIN', true) + cardSecretInput('pinConfirmation', 'Confirmar novo PIN', true), 'PUT');
          actions += cardActionForm(card.id, 'pin/recovery', 'Recuperar PIN', cardSecretInput('confirmationPassword', 'Senha de login')
            + cardSecretInput('newPin', 'Novo PIN', true) + cardSecretInput('pinConfirmation', 'Confirmar novo PIN', true), 'PUT');
        }
        if (['ACTIVE', 'PENDING_ACTIVATION'].includes(card.state) && !card.voluntaryBlocked) {
          actions += cardActionForm(card.id, 'block', 'Bloquear voluntariamente', '<label>Motivo<input name="reason" maxlength="240" required></label>');
        }
        if (['ACTIVE', 'PENDING_ACTIVATION', 'EXPIRED'].includes(card.state)) {
          actions += cardActionForm(card.id, 'cancel', 'Cancelar definitivamente o instrumento');
        }
        if (card.state === 'CANCELLED') actions += cardActionForm(card.id, 'replacement', 'Solicitar substituição');
        return `<article class="card" data-card-article><h3>${cardBrandLogo(card.brand)} ${escapeHtml(card.brand)} · final ${escapeHtml(card.lastFour)}</h3>
          <p>Estado: ${escapeHtml(card.state)} · Limite: ${formatarMoeda(card.allocated)} · Disponível: ${formatarMoeda(card.available)}</p>
          <p>Bloqueio de PIN: ${card.pinBlocked ? 'Sim' : 'Não'} · Voluntário: ${card.voluntaryBlocked ? 'Sim' : 'Não'} · Gerencial: ${card.administrativeBlocked ? 'Sim' : 'Não'}</p>
          ${card.voluntaryReason ? `<p>Motivo voluntário: ${escapeHtml(card.voluntaryReason)}</p>` : ''}
          ${card.administrativeReason ? `<p>Motivo gerencial: ${escapeHtml(card.administrativeReason)}</p>` : ''}
          <div class="card-sensitive" aria-live="polite"></div>
          <button type="button" data-hide-card class="btn-outline">Ocultar dados</button>
          <details><summary>Gerenciar instrumento</summary>${actions}</details></article>`;
      }).join('') : '<p>Nenhum cartão emitido.</p>'}`;
    decorateCardInstruments(container);
    container.querySelectorAll('[data-card-form]').forEach(form => form.addEventListener('submit', handleCardInstrumentAction));
    container.querySelectorAll('[data-hide-card]').forEach(button => button.addEventListener('click', hideRevealedCardData));
  } catch (error) {
    if (container.isConnected) { container.innerHTML = '<h2>Meus cartões</h2><div role="alert"></div>'; showApiError(container.querySelector('[role="alert"]'), error); }
  }
}

async function handleCardInstrumentAction(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const container = form.closest('#cardInstruments');
  const message = form.querySelector('[data-card-message]');
  const fields = form.querySelector('fieldset');
  if (form.dataset.path === 'cancel' && !window.confirm('Cancelar este instrumento definitivamente? O histórico e a dívida serão preservados.')) return;
  const body = JSON.stringify(Object.fromEntries(new FormData(form)));
  hideRevealedCardData();
  const revealEpoch = cardRevealEpoch;
  fields.disabled = true;
  clearMessage(message);
  try {
    const path = form.dataset.cardId ? `/${encodeURIComponent(form.dataset.cardId)}/${form.dataset.path}` : '';
    const result = await api(`/api/v1/cards${path}`, { method: form.dataset.method, body });
    if (!form.isConnected) return;
    if (form.dataset.path === 'details') {
      if (document.hidden || revealEpoch !== cardRevealEpoch) return;
      const area = form.closest('[data-card-article]').querySelector('.card-sensitive');
      const paragraph = document.createElement('p');
      paragraph.textContent = `${result.holderName} · ${result.pan} · Validade ${result.expiresOn.slice(0,7).split('-').reverse().join('/')} · CVV ${result.cvv}`;
      area.replaceChildren(paragraph);
    } else {
      await renderCardInstruments(container);
    }
  } catch (error) {
    if (form.isConnected) showApiError(message, error);
    // A PIN refusal can have committed a new security block; refresh the summary without retaining secrets.
    if (error.code === 'CARD_PIN_BLOCKED' && container.isConnected) {
      await renderCardInstruments(container);
      const notice = document.createElement('p'); notice.textContent = error.message; notice.setAttribute('role', 'alert'); container.prepend(notice);
    }
  } finally {
    form.querySelectorAll('input[type="password"]').forEach(input => { input.value = ''; });
    fields.disabled = false;
  }
}

async function renderAdminCardInstruments(customerId, container) {
  let financePanel = container.parentElement.querySelector('.admin-card-finance');
  if (!financePanel) { financePanel = document.createElement('div'); financePanel.className='admin-card-finance'; container.parentElement.append(financePanel); }
  renderAdminCardFinance(customerId, financePanel);
  container.textContent = 'Carregando cartões…';
  try {
    const cards = await adminApi(`/api/v1/admin/cards/customers/${encodeURIComponent(customerId)}`);
    if (!container.isConnected) return;
    container.innerHTML = cards.length ? cards.map(card => `<form data-admin-card-form data-card-id="${escapeHtml(card.id)}">
      <h4>${escapeHtml(card.brand)} · final ${escapeHtml(card.lastFour)} · ${escapeHtml(card.state)}</h4>
      <label>Ação<select name="action">
        <option value="ADMINISTRATIVE:${!card.administrativeBlocked}">${card.administrativeBlocked ? 'Retirar bloqueio gerencial' : 'Aplicar bloqueio gerencial'}</option>
        ${card.voluntaryBlocked ? '<option value="VOLUNTARY:false">Retirar bloqueio voluntário</option>' : ''}
      </select></label><label>Motivo<input name="reason" maxlength="240" required></label>
      <button type="submit" class="btn-outline">Confirmar ação</button><div role="status"></div></form>`).join('') : '<p>Nenhum cartão emitido.</p>';
    container.querySelectorAll('form').forEach(form => form.addEventListener('submit', async event => {
      event.preventDefault();
      const button = form.querySelector('button'); const message = form.querySelector('[role="status"]');
      const [cause, blocked] = form.elements.action.value.split(':'); button.disabled = true;
      try {
        await adminApi(`/api/v1/admin/cards/customers/${encodeURIComponent(customerId)}/${encodeURIComponent(form.dataset.cardId)}`, {
          method: 'PUT', body: JSON.stringify({ cause, blocked: blocked === 'true', reason: form.elements.reason.value })
        });
        if (form.isConnected) await renderAdminCardInstruments(customerId, container);
      } catch (error) { if (form.isConnected) showApiError(message, error); }
      finally { button.disabled = false; }
    }));
  } catch (error) { if (container.isConnected) showApiError(container, error); }
}

function loadPage(page) {
  updateNavigation(page);
  hideRevealedCardData();
  switch (page) {
    case 'dashboard':
      renderDashboard();
      break;
    case 'transfer':
      renderTransferPage();
      break;
    case 'extrato':
      renderExtratoPage();
      break;
    case 'profile':
      renderProfilePage();
      break;
    case 'pix':
      renderPixPage();
      break;
    case 'payments':
      renderPaymentsPage();
      break;
    case 'invest':
      renderInvestmentsPage();
      break;
    case 'cartao':
      renderCardAllocationPage();
      break;
    default:
      renderDashboard();
  }
}

/* =========================================================
   ADMINISTRAÇÃO POR CLIENTE
========================================================= */

function showAdminApp(administrator) {
  hideRevealedCardData();
  document.getElementById('loginScreen').classList.add('hidden');
  document.getElementById('appShell').classList.add('hidden');
  document.getElementById('adminShell').classList.remove('hidden');

  document.getElementById('currentAdministratorName').textContent =
    administrator.displayName ?? 'Gerente SPBank';
}

async function handleAdminLogin(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const errorBox = document.getElementById('adminLoginError');
  const data = Object.fromEntries(new FormData(form));

  clearMessage(errorBox);
  button.disabled = true;
  button.textContent = 'Entrando…';

  try {
    const session = await adminApi('/api/v1/admin/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        username: data.username,
        password: data.password
      })
    });

    sessionStorage.setItem(adminAuthTokenKey, session.accessToken);
    sessionStorage.setItem(adminNameKey, session.displayName);
    sessionStorage.setItem(activeContextKey, 'admin');

    showAdminApp(session);
    form.reset();

    await loadAdminCustomers();
  } catch (error) {
    showApiError(errorBox, error);
  } finally {
    button.disabled = false;
    button.textContent = 'Entrar na área gerencial';
  }
}

async function performAdminLogout() {
  try {
    await adminApi('/api/v1/admin/auth/logout', { method: 'POST' });
  } catch (_) {
    // Encerra a sessão local mesmo se o backend não responder.
  } finally {
    clearAdminSession();
    showPublicArea('admin');
  }
}

async function loadAdminCustomers() {
  const container = document.getElementById('adminCustomers');
  const errorBox = document.getElementById('adminGlobalError');

  clearMessage(errorBox);

  if (container) {
    container.innerHTML = '<p>Carregando clientes…</p>';
  }

  try {
    const response = await adminApi('/api/v1/admin/customers');
    adminCustomers = Array.isArray(response) ? response : [];
    renderAdminCustomers();
  } catch (error) {
    if (container) {
      container.innerHTML =
        '<p>Não foi possível carregar os clientes.</p>';
    }

    showApiError(errorBox, error);
    throw error;
  }
}

function renderAdminCustomers() {
  const container = document.getElementById('adminCustomers');
  if (!container) return;

  if (adminCustomers.length === 0) {
    container.innerHTML = '<p>Nenhum cliente cadastrado.</p>';
    return;
  }

  container.innerHTML = adminCustomers.map(customer => {
    const customerId = escapeHtml(customer.customerId);
    const accounts = Array.isArray(customer.accounts)
      ? customer.accounts
      : [];

    const targetPlan = customer.customerPlan === 'PLUS'
      ? 'STANDARD'
      : 'PLUS';

    const actionLabel = targetPlan === 'PLUS'
      ? 'Promover para Plus'
      : 'Rebaixar para Standard';

    const accountsHtml = accounts.length === 0
      ? `
        <div class="statement-detail">
          <span>Contas</span>
          <strong>Nenhuma conta cadastrada</strong>
        </div>
      `
      : accounts.map(account => `
        <div class="statement-detail">
          <span>${escapeHtml(accountTypeLabel(account.accountType))}</span>
          <strong>
            Ag. ${escapeHtml(account.branch)}
            · Conta ${escapeHtml(account.accountNumber)}
            · ${account.active ? 'Ativa' : 'Inativa'}
          </strong>
        </div>
      `).join('');

    return `
      <article
        class="admin-account-card"
        data-customer-id="${customerId}"
      >
        <div class="admin-account-head">
          <div>
            <h3>${escapeHtml(customer.fullName)}</h3>

            <p class="summary-context">
              CPF: ${escapeHtml(customer.maskedDocument)}
            </p>
          </div>

          <span class="plan-badge ${
            customer.customerPlan === 'PLUS' ? 'plus' : 'standard'
          }">
            ${escapeHtml(customerPlanLabel(customer.customerPlan))}
          </span>
        </div>

        <div class="statement-details">
          <div class="statement-detail">
            <span>Situação do cliente</span>
            <strong>${customer.active ? 'Ativo' : 'Inativo'}</strong>
          </div>

          <div class="statement-detail">
            <span>Quantidade de contas</span>
            <strong>${accounts.length}</strong>
          </div>

          ${accountsHtml}
        </div>

        <div class="admin-plan-action">
          <label>
            Motivo da alteração

            <input
              type="text"
              maxlength="240"
              data-plan-reason="${customerId}"
              placeholder="Informe o motivo da decisão"
              ${customer.active ? '' : 'disabled'}
            >
          </label>

          <div class="actions">
            <button
              class="btn"
              type="button"
              data-plan-action="${customerId}"
              data-target-plan="${targetPlan}"
              ${customer.active ? '' : 'disabled'}
            >
              ${actionLabel}
            </button>

            <button
              class="btn-outline"
              type="button"
              data-plan-history="${customerId}"
              data-customer-name="${escapeHtml(customer.fullName)}"
            >
              Ver histórico
            </button>
            <button class="btn-outline" type="button" data-admin-cards="${customerId}">Gerenciar cartões</button>
          </div>
          <div data-admin-card-panel></div>

          <p
            class="error-message admin-row-message"
            data-plan-message="${customerId}"
            role="status"
          ></p>
        </div>
      </article>
    `;
  }).join('');

  container.querySelectorAll('[data-plan-action]').forEach(button => {
    button.addEventListener('click', () => changeAdminCustomerPlan(button));
  });

  container.querySelectorAll('[data-admin-cards]').forEach(button => {
    button.addEventListener('click', () => renderAdminCardInstruments(button.dataset.adminCards,
      button.closest('article').querySelector('[data-admin-card-panel]')));
  });

  container.querySelectorAll('[data-plan-history]').forEach(button => {
    button.addEventListener('click', () => {
      openCustomerPlanHistory(
        button.dataset.planHistory,
        button.dataset.customerName
      );
    });
  });
}

async function changeAdminCustomerPlan(button) {
  const customerId = button.dataset.planAction;
  const targetPlan = button.dataset.targetPlan;

  const card = button.closest('[data-customer-id]');
  const reasonInput = card.querySelector('[data-plan-reason]');
  const message = card.querySelector('[data-plan-message]');

  const reason = reasonInput?.value.trim() ?? '';

  clearMessage(message);

  if (!reason) {
    showApiError(
      message,
      new Error('Informe o motivo da alteração do plano.')
    );
    reasonInput?.focus();
    return;
  }

  const originalLabel = button.textContent;

  button.disabled = true;
  button.textContent = 'Salvando…';

  try {
    await adminApi(
      `/api/v1/admin/customers/${encodeURIComponent(customerId)}/plan`,
      {
        method: 'PUT',
        body: JSON.stringify({
          customerPlan: targetPlan,
          reason
        })
      }
    );

    await loadAdminCustomers();

    showSuccess(
      document.getElementById('adminGlobalError'),
      'Plano do cliente alterado e histórico registrado com sucesso.'
    );
  } catch (error) {
    showApiError(message, error);
  } finally {
    button.disabled = false;
    button.textContent = originalLabel;
  }
}

async function openCustomerPlanHistory(customerId, customerName) {
  const dialog = document.getElementById('planHistoryDialog');
  const content = document.getElementById('planHistoryContent');
  const customer = document.getElementById('planHistoryCustomer');

  customer.textContent = customerName || '';
  content.innerHTML = '<p>Carregando histórico…</p>';

  dialog.showModal();

  try {
    const history = await adminApi(
      `/api/v1/admin/customers/${encodeURIComponent(customerId)}/plan-history`
    );

    renderCustomerPlanHistory(history);
  } catch (error) {
    content.innerHTML = `
      <p
        id="planHistoryError"
        class="error-message"
        role="alert"
      ></p>
    `;

    showApiError(document.getElementById('planHistoryError'), error);
  }
}

function renderCustomerPlanHistory(history) {
  const content = document.getElementById('planHistoryContent');

  if (!Array.isArray(history) || history.length === 0) {
    content.innerHTML = '<p>Nenhuma alteração de plano registrada.</p>';
    return;
  }

  content.innerHTML = `
    <div class="plan-history-list">
      ${history.map(change => `
        <article class="plan-history-item">
          <div class="row">
            <strong>
              ${escapeHtml(customerPlanLabel(change.previousPlan))}
              →
              ${escapeHtml(customerPlanLabel(change.newPlan))}
            </strong>

            <span>${escapeHtml(formatDateTime(change.changedAt))}</span>
          </div>

          <div class="statement-details">
            <div class="statement-detail">
              <span>Gerente</span>
              <strong>${escapeHtml(change.administratorName)}</strong>
            </div>

            <div class="statement-detail">
              <span>Motivo</span>
              <strong>${escapeHtml(change.reason)}</strong>
            </div>
          </div>
        </article>
      `).join('')}
    </div>
  `;
}

/* =========================================================
   INICIALIZAÇÃO
========================================================= */

async function initializeApp() {
  initializeTheme();
  initializeNavigation();
  initializeClientThemePlacement();

  document.querySelectorAll('[data-public-tab]').forEach(control => {
    control.addEventListener('click', () => {
      showAccessPanel(control.dataset.publicTab);
    });
  });

  document.getElementById('loginForm')
    .addEventListener('submit', handleLogin);

  document.getElementById('registerForm')
    .addEventListener('submit', handleRegistration);

  document.getElementById('adminLoginForm')
    .addEventListener('submit', handleAdminLogin);

  document.getElementById('logoutButton')
    .addEventListener('click', performLogout);

  document.getElementById('accountSelector')
    .addEventListener('change', handleAccountSelection);

  document.getElementById('openMissingAccountButton')
    .addEventListener('click', openMissingAccountDialog);

  document.getElementById('openAccountForm')
    .addEventListener('submit', handleOpenAccount);

  document.getElementById('cancelOpenAccountButton')
    .addEventListener('click', () => {
      document.getElementById('openAccountDialog').close();
    });

  document.getElementById('adminLogoutButton')
    .addEventListener('click', performAdminLogout);

  document.getElementById('refreshAdminCustomersButton')
    .addEventListener('click', () => {
      loadAdminCustomers().catch(() => {});
    });

  document.getElementById('closePlanHistoryButton')
    .addEventListener('click', () => {
      document.getElementById('planHistoryDialog').close();
    });

  const context = sessionStorage.getItem(activeContextKey);
  const clientToken = sessionStorage.getItem(authTokenKey);
  const adminToken = sessionStorage.getItem(adminAuthTokenKey);

  if (context === 'admin' && adminToken) {
    const displayName =
      sessionStorage.getItem(adminNameKey) || 'Gerente SPBank';

    showAdminApp({ displayName });

    try {
      await loadAdminCustomers();
      return;
    } catch (error) {
      if (error.status === 401) return;

      showApiError(
        document.getElementById('adminGlobalError'),
        error
      );
      return;
    }
  }

  if (clientToken) {
    try {
      const user = await api('/api/v1/auth/me');
      sessionStorage.setItem(activeContextKey, 'client');

      saldo = 0;
      extrato = [];

      showAuthenticatedApp(user);

      await Promise.all([
        loadCustomerAccounts(),
        loadAccountSummary(),
        refreshOfficialStatement()
      ]);

      loadPage('dashboard');
      return;
    } catch (error) {
      clearClientSession();

      showLogin(
        error.status === 401
          ? 'Sua sessão expirou. Entre novamente.'
          : 'Não foi possível conectar ao SPBank. Verifique o backend.'
      );
      return;
    }
  }

  if (adminToken) {
    const displayName =
      sessionStorage.getItem(adminNameKey) || 'Gerente SPBank';

    try {
      sessionStorage.setItem(activeContextKey, 'admin');
      showAdminApp({ displayName });
      await loadAdminCustomers();
      return;
    } catch (_) {
      clearAdminSession();
      showPublicArea('admin');
      return;
    }
  }

  showPublicArea('login');
}

document.addEventListener('visibilitychange', () => { if (document.hidden) hideRevealedCardData(); });
window.addEventListener('pagehide', hideRevealedCardData);
initializeApp();

/* Floating navigation: only layout preference is kept locally. */
function initializeNavigation(){
 const nav=document.querySelector('#appShell .floating-nav');if(!nav)return;
 let expanded=false;try{expanded=localStorage.getItem('spbank.nav-expanded')==='true';}catch{}
 const more=document.createElement('button');more.type='button';more.className='nav-more';more.setAttribute('aria-label','Mais opções de navegação');more.setAttribute('aria-controls','primaryNavigation');more.setAttribute('aria-expanded','false');more.innerHTML='<span class="material-symbols-outlined" aria-hidden="true">apps</span><span>Mais</span>';nav.append(more);
 more.onclick=()=>setMobileNavigationOpen(nav,!nav.classList.contains('mobile-open'));
 matchMedia('(max-width:900px)').addEventListener('change',()=>setMobileNavigationOpen(nav,false));
 nav.querySelectorAll('[data-nav-page]').forEach(button=>{button.title=button.getAttribute('aria-label');});
 const toggle=nav.querySelector('.nav-toggle');
 const apply=()=>{nav.classList.toggle('is-expanded',expanded);document.getElementById('appShell').classList.toggle('nav-expanded',expanded);toggle.setAttribute('aria-expanded',String(expanded));toggle.setAttribute('aria-label',expanded?'Recolher menu':'Expandir menu');toggle.title=expanded?'Recolher menu':'Expandir menu';toggle.firstElementChild.textContent=expanded?'chevron_left':'chevron_right';};
 toggle.addEventListener('click',()=>{expanded=!expanded;apply();try{localStorage.setItem('spbank.nav-expanded',String(expanded));}catch{}});
 nav.addEventListener('keydown',event=>{if(event.key==='Escape'&&nav.classList.contains('mobile-open')){setMobileNavigationOpen(nav,false);more.focus();}else if(event.key==='Escape'&&expanded){expanded=false;apply();toggle.focus();try{localStorage.setItem('spbank.nav-expanded','false');}catch{}}});apply();updateNavigation('dashboard');
}
function updateNavigation(page){
 const nav=document.querySelector('.floating-nav');if(nav){const wasOpen=nav.classList.contains('mobile-open');setMobileNavigationOpen(nav,false);const more=nav.querySelector('.nav-more');if(more){if(wasOpen&&matchMedia('(max-width:900px)').matches)more.focus();more.setAttribute('aria-expanded','false');more.classList.toggle('is-active',['invest','cartao','extrato','profile'].includes(page));}}
 document.querySelectorAll('[data-nav-page]').forEach(button=>{const active=button.dataset.navPage===page;button.classList.toggle('is-active',active);if(active)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');});
}

function setMobileNavigationOpen(nav,open){
 nav.classList.toggle('mobile-open',open);const more=nav.querySelector('.nav-more');if(!more)return;
 more.setAttribute('aria-expanded',String(open));more.setAttribute('aria-label',open?'Fechar opções de navegação':'Mais opções de navegação');
 more.firstElementChild.textContent=open?'close':'apps';more.lastElementChild.textContent=open?'Fechar':'Mais';
}

function initializeClientThemePlacement(){
 const control=document.getElementById('themeControl'),shell=document.getElementById('appShell');if(!control||!shell)return;
 const home=document.createComment('theme control home');control.before(home);
 const compact=matchMedia('(max-width:560px)');
 const place=()=>{const target=compact.matches&&!shell.classList.contains('hidden')?shell.querySelector('.app-header .user-session'):null;
  if(target&&control.parentElement!==target){closeThemeMenu();target.append(control);}
  else if(!target&&control.previousSibling!==home){closeThemeMenu();home.after(control);}
 };
 new MutationObserver(place).observe(shell,{attributes:true,attributeFilter:['class']});compact.addEventListener('change',place);place();
}
