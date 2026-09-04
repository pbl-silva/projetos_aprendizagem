/* =========================================================
   SPBANK FRONT-END
   Integração com o backend oficial
========================================================= */


/* =========================================================
   ESTADO DA APLICAÇÃO
========================================================= */

let saldo = 0;
let filtroAtual = 'todos';
let extrato = [];

let currentUser = null;
let currentAccountSummary = null;
let customerAccounts = [];

let pendingTransfer = null;
let pendingIdempotencyKey = null;

let adminAccounts = [];


/* =========================================================
   CONFIGURAÇÃO
========================================================= */

const apiBaseUrl =
  window.SPBANK_API_URL ??
  'http://localhost:8080';

const apiTimeoutMs = 10_000;


/* =========================================================
   TEMA
========================================================= */

const themePreferenceKey =
  'spbank.theme';

const validThemePreferences =
  new Set([
    'system',
    'light',
    'dark'
  ]);

const themeLabels = {
  system: 'Sistema',
  light: 'Claro',
  dark: 'Escuro'
};

const themeIcons = {
  system: 'contrast',
  light: 'light_mode',
  dark: 'dark_mode'
};

const systemThemeQuery =
  window.matchMedia(
    '(prefers-color-scheme: dark)'
  );


function getThemePreference() {
  try {
    const saved =
      localStorage.getItem(
        themePreferenceKey
      );

    if (
      validThemePreferences.has(
        saved
      )
    ) {
      return saved;
    }

  } catch (_) {
    /*
     * Se o navegador impedir acesso
     * ao localStorage, o SPBank
     * continua funcionando em Sistema.
     */
  }

  return 'system';
}


function resolveTheme(
  preference
) {
  if (
    preference === 'light' ||
    preference === 'dark'
  ) {
    return preference;
  }

  return systemThemeQuery.matches
    ? 'dark'
    : 'light';
}


function updateThemeAssets(
  effectiveTheme
) {
  const dark =
    effectiveTheme === 'dark';

  const logoPath =
    dark
      ? 'img/logo-spbank-dark.png'
      : 'img/logo-spbank.png';

  const faviconPath =
    dark
      ? 'img/favicon-spbank-dark.png'
      : 'img/favicon-spbank.png';


  document
    .querySelectorAll(
      '.theme-logo'
    )
    .forEach(
      logo => {
        logo.src =
          logoPath;
      }
    );


  const favicon =
    document.getElementById(
      'appFavicon'
    );

  if (favicon) {
    favicon.href =
      faviconPath;
  }
}


function updateThemeControl(
  preference
) {
  const currentLabel =
    document.getElementById(
      'themeCurrentLabel'
    );

  const icon =
    document.getElementById(
      'themeIcon'
    );

  const menuButton =
    document.getElementById(
      'themeMenuButton'
    );


  if (currentLabel) {
    currentLabel.textContent =
      themeLabels[preference] ??
      themeLabels.system;
  }


  if (icon) {
    icon.textContent =
      themeIcons[preference] ??
      themeIcons.system;
  }


  if (menuButton) {
    menuButton.setAttribute(
      'aria-label',
      `Tema atual: ${
        themeLabels[preference] ??
        themeLabels.system
      }. Alterar tema.`
    );
  }


  document
    .querySelectorAll(
      '[data-theme-option]'
    )
    .forEach(
      option => {
        const active =
          option.dataset
            .themeOption ===
          preference;

        option.classList.toggle(
          'active',
          active
        );
      }
    );
}


function applyTheme(
  preference =
    getThemePreference()
) {
  const normalizedPreference =
    validThemePreferences.has(
      preference
    )
      ? preference
      : 'system';

  const effectiveTheme =
    resolveTheme(
      normalizedPreference
    );


  document
    .documentElement
    .setAttribute(
      'data-theme',
      effectiveTheme
    );


  /*
   * Também informa ao navegador
   * qual esquema está efetivamente
   * em uso.
   */
  document
    .documentElement
    .style
    .colorScheme =
      effectiveTheme;


  updateThemeAssets(
    effectiveTheme
  );

  updateThemeControl(
    normalizedPreference
  );
}


function saveThemePreference(
  preference
) {
  if (
    !validThemePreferences.has(
      preference
    )
  ) {
    return;
  }

  try {
    localStorage.setItem(
      themePreferenceKey,
      preference
    );

  } catch (_) {
    /*
     * O tema ainda será aplicado na
     * página atual mesmo que o navegador
     * não permita persistência.
     */
  }

  applyTheme(
    preference
  );
}


function closeThemeMenu(
  returnFocus = false
) {
  const menu =
    document.getElementById(
      'themeMenu'
    );

  const button =
    document.getElementById(
      'themeMenuButton'
    );

  if (!menu) {
    return;
  }

  menu.classList.add(
    'hidden'
  );

  button?.setAttribute(
    'aria-expanded',
    'false'
  );

  if (returnFocus) {
    button?.focus();
  }
}


function openThemeMenu() {
  const menu =
    document.getElementById(
      'themeMenu'
    );

  const button =
    document.getElementById(
      'themeMenuButton'
    );

  if (!menu) {
    return;
  }

  menu.classList.remove(
    'hidden'
  );

  button?.setAttribute(
    'aria-expanded',
    'true'
  );

  const active =
    menu.querySelector(
      '.theme-option.active'
    );

  active?.focus();
}


function toggleThemeMenu() {
  const menu =
    document.getElementById(
      'themeMenu'
    );

  if (!menu) {
    return;
  }

  if (
    menu.classList.contains(
      'hidden'
    )
  ) {
    openThemeMenu();

  } else {
    closeThemeMenu();
  }
}


function handleSystemThemeChange() {
  if (
    getThemePreference() !==
    'system'
  ) {
    return;
  }

  applyTheme(
    'system'
  );
}


function initializeTheme() {
  applyTheme(
    getThemePreference()
  );


  const control =
    document.getElementById(
      'themeControl'
    );

  const menuButton =
    document.getElementById(
      'themeMenuButton'
    );


  menuButton?.addEventListener(
    'click',
    event => {
      event.stopPropagation();

      toggleThemeMenu();
    }
  );


  document
    .querySelectorAll(
      '[data-theme-option]'
    )
    .forEach(
      option => {
        option.addEventListener(
          'click',
          event => {
            event.stopPropagation();

            const preference =
              option.dataset
                .themeOption;

            saveThemePreference(
              preference
            );

            closeThemeMenu(
              true
            );
          }
        );
      }
    );


  /*
   * Fecha quando o usuário clicar
   * em qualquer ponto fora do menu.
   */
  document.addEventListener(
    'click',
    event => {
      if (
        control &&
        !control.contains(
          event.target
        )
      ) {
        closeThemeMenu();
      }
    }
  );


  /*
   * Escape fecha o menu.
   */
  document.addEventListener(
    'keydown',
    event => {
      if (
        event.key ===
        'Escape'
      ) {
        closeThemeMenu(
          true
        );
      }
    }
  );


  /*
   * Com a preferência Sistema,
   * acompanha alterações do Windows
   * enquanto a página está aberta.
   */
  if (
    typeof systemThemeQuery
      .addEventListener ===
    'function'
  ) {
    systemThemeQuery
      .addEventListener(
        'change',
        handleSystemThemeChange
      );

  } else if (
    typeof systemThemeQuery
      .addListener ===
    'function'
  ) {
    systemThemeQuery
      .addListener(
        handleSystemThemeChange
      );
  }


  /*
   * Sincroniza a preferência entre
   * abas abertas do mesmo navegador.
   */
  window.addEventListener(
    'storage',
    event => {
      if (
        event.key ===
        themePreferenceKey
      ) {
        applyTheme(
          getThemePreference()
        );
      }
    }
  );
}


/* =========================================================
   SESSÕES
========================================================= */

const authTokenKey =
  'spbank.access-token';

const adminAuthTokenKey =
  'spbank.admin-access-token';

const adminNameKey =
  'spbank.admin-name';

const activeContextKey =
  'spbank.active-context';


/* =========================================================
   LABELS
========================================================= */

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

const accountPlanLabels = {
  STANDARD: 'Standard',
  PLUS: 'Plus'
};


/* =========================================================
   HELPERS DE FORMATAÇÃO
========================================================= */

function formatarMoeda(valor) {
  return Number(
    valor ?? 0
  ).toLocaleString(
    'pt-BR',
    {
      style: 'currency',
      currency: 'BRL'
    }
  );
}


function formatDate(value) {
  if (!value) {
    return '—';
  }

  const date =
    new Date(
      `${value}T00:00:00`
    );

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return value;
  }

  return date.toLocaleDateString(
    'pt-BR'
  );
}


function formatDateTime(value) {
  if (!value) {
    return '—';
  }

  const date =
    new Date(value);

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return value;
  }

  return date.toLocaleString(
    'pt-BR'
  );
}


function formatCpf(value) {
  const digits =
    String(
      value ?? ''
    ).replace(
      /\D/g,
      ''
    );

  if (
    digits.length !== 11
  ) {
    return value ?? '—';
  }

  return digits.replace(
    /^(\d{3})(\d{3})(\d{3})(\d{2})$/,
    '$1.$2.$3-$4'
  );
}


function escapeHtml(value) {
  return String(
    value ?? '—'
  ).replace(
    /[&<>'"]/g,
    character => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    })[character]
  );
}


function transferModalityLabel(
  value
) {
  if (
    value === 'TED'
  ) {
    return 'Transferência - TED';
  }

  if (
    value === 'INTERNAL' ||
    value === 'INTERNA'
  ) {
    return 'Transferência';
  }

  return (
    value ||
    'Transferência'
  );
}


function accountTypeLabel(value) {
  return (
    accountTypeLabels[value] ??
    value ??
    'Conta'
  );
}


function accountPlanLabel(value) {
  return (
    accountPlanLabels[value] ??
    value ??
    '—'
  );
}


/* =========================================================
   HELPERS DE DOM
========================================================= */

function setText(
  id,
  value
) {
  const element =
    document.getElementById(
      id
    );

  if (element) {
    element.textContent =
      value ?? '—';
  }
}


function clearMessage(element) {
  if (!element) {
    return;
  }

  element.textContent = '';

  element.classList.remove(
    'success-message'
  );
}


function showSuccess(
  element,
  message
) {
  if (!element) {
    return;
  }

  element.textContent =
    message;

  element.classList.add(
    'success-message'
  );
}


function showApiError(
  element,
  error
) {
  if (!element) {
    return;
  }

  element.classList.remove(
    'success-message'
  );

  const message =
    error?.message ||
    'Não foi possível concluir a operação.';

  element.textContent =
    error?.correlationId
      ? `${message} (código de atendimento: ${error.correlationId})`
      : message;
}


/* =========================================================
   CONTROLE DE SESSÃO LOCAL
========================================================= */

function clearClientSession() {
  sessionStorage.removeItem(
    authTokenKey
  );

  if (
    sessionStorage.getItem(
      activeContextKey
    ) === 'client'
  ) {
    sessionStorage.removeItem(
      activeContextKey
    );
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
  sessionStorage.removeItem(
    adminAuthTokenKey
  );

  sessionStorage.removeItem(
    adminNameKey
  );

  if (
    sessionStorage.getItem(
      activeContextKey
    ) === 'admin'
  ) {
    sessionStorage.removeItem(
      activeContextKey
    );
  }

  adminAccounts = [];
}


/* =========================================================
   CLIENTE HTTP DO CLIENTE
========================================================= */

async function api(
  path,
  options = {}
) {
  const controller =
    new AbortController();

  const timeoutId =
    setTimeout(
      () =>
        controller.abort(),
      apiTimeoutMs
    );

  try {
    const token =
      sessionStorage.getItem(
        authTokenKey
      );

    const response =
      await fetch(
        `${apiBaseUrl}${path}`,
        {
          ...options,

          signal:
            controller.signal,

          headers: {
            'Content-Type':
              'application/json',

            ...(token
              ? {
                  Authorization:
                    `Bearer ${token}`
                }
              : {}),

            ...(options.headers || {})
          }
        }
      );

    const contentType =
      response.headers
        .get('content-type') ??
      '';

    const isJson =
      contentType.includes(
        'application/json'
      ) ||
      contentType.includes(
        'application/problem+json'
      );

    const body =
      response.status === 204 ||
      !isJson
        ? null
        : await response.json();

    if (!response.ok) {
      const publicRoute =
        path ===
          '/api/v1/auth/login' ||
        path ===
          '/api/v1/auth/register';

      if (
        response.status === 401 &&
        !publicRoute
      ) {
        clearClientSession();

        showLogin(
          'Sua sessão expirou. Entre novamente.'
        );
      }

      const error =
        new Error(
          body?.detail ||
          'Serviço temporariamente indisponível. Consulte a operação antes de reenviar.'
        );

      error.status =
        response.status;

      error.code =
        body?.errorCode;

      error.correlationId =
        body?.correlationId;

      error.domain =
        body?.domain;

      throw error;
    }

    return body;

  } catch (error) {
    if (
      error.name ===
      'AbortError'
    ) {
      const timeoutError =
        new Error(
          'O banco demorou para responder. Consulte a operação antes de tentar novamente.'
        );

      timeoutError.code =
        'NETWORK_TIMEOUT';

      throw timeoutError;
    }

    throw error;

  } finally {
    clearTimeout(
      timeoutId
    );
  }
}


/* =========================================================
   CLIENTE HTTP ADMINISTRATIVO
========================================================= */

async function adminApi(
  path,
  options = {}
) {
  const controller =
    new AbortController();

  const timeoutId =
    setTimeout(
      () =>
        controller.abort(),
      apiTimeoutMs
    );

  try {
    const token =
      sessionStorage.getItem(
        adminAuthTokenKey
      );

    const response =
      await fetch(
        `${apiBaseUrl}${path}`,
        {
          ...options,

          signal:
            controller.signal,

          headers: {
            'Content-Type':
              'application/json',

            ...(token
              ? {
                  Authorization:
                    `Bearer ${token}`
                }
              : {}),

            ...(options.headers || {})
          }
        }
      );

    const contentType =
      response.headers
        .get('content-type') ??
      '';

    const isJson =
      contentType.includes(
        'application/json'
      ) ||
      contentType.includes(
        'application/problem+json'
      );

    const body =
      response.status === 204 ||
      !isJson
        ? null
        : await response.json();

    if (!response.ok) {
      const loginRoute =
        path ===
        '/api/v1/admin/auth/login';

      if (
        response.status === 401 &&
        !loginRoute
      ) {
        clearAdminSession();

        showPublicArea(
          'admin'
        );

        showApiError(
          document.getElementById(
            'adminLoginError'
          ),
          new Error(
            'Sua sessão gerencial expirou. Entre novamente.'
          )
        );
      }

      const error =
        new Error(
          body?.detail ||
          'Não foi possível concluir a operação gerencial.'
        );

      error.status =
        response.status;

      error.code =
        body?.errorCode;

      error.correlationId =
        body?.correlationId;

      error.domain =
        body?.domain;

      throw error;
    }

    return body;

  } catch (error) {
    if (
      error.name ===
      'AbortError'
    ) {
      const timeoutError =
        new Error(
          'O SPBank demorou para responder. Tente novamente.'
        );

      timeoutError.code =
        'NETWORK_TIMEOUT';

      throw timeoutError;
    }

    throw error;

  } finally {
    clearTimeout(
      timeoutId
    );
  }
}


/* =========================================================
   ÁREA PÚBLICA
========================================================= */

function showAccessPanel(panel) {
  const loginScreen =
    document.getElementById(
      'loginScreen'
    );

  const panels = {
    login:
      document.getElementById(
        'loginPanel'
      ),

    register:
      document.getElementById(
        'registerPanel'
      ),

    admin:
      document.getElementById(
        'adminLoginPanel'
      )
  };

  const controls =
    document.querySelectorAll(
      '[data-public-tab]'
    );

  const managerEntry =
    document.getElementById(
      'adminLoginTab'
    );

  const clientReturn =
    document.getElementById(
      'clientAccessBackButton'
    );


  Object
    .entries(panels)
    .forEach(
      ([name, element]) => {
        if (!element) {
          return;
        }

        const active =
          name === panel;

        element.classList.toggle(
          'hidden',
          !active
        );

        element.setAttribute(
          'aria-hidden',
          String(!active)
        );
      }
    );


  /*
   * Apenas as duas abas principais
   * recebem estado visual de tab.
   */
  controls.forEach(
    control => {
      if (
        !control.classList.contains(
          'access-tab'
        )
      ) {
        return;
      }

      const active =
        control.dataset.publicTab ===
        panel;

      control.classList.toggle(
        'active',
        active
      );

      control.setAttribute(
        'aria-selected',
        String(active)
      );
    }
  );


  /*
   * O acesso gerencial fica discreto.
   * Dentro da área gerencial, aparece
   * apenas o retorno para o cliente.
   */
  managerEntry?.classList.toggle(
    'hidden',
    panel === 'admin'
  );

  clientReturn?.classList.toggle(
    'hidden',
    panel !== 'admin'
  );


  if (loginScreen) {
    loginScreen.classList.toggle(
      'registration-mode',
      panel === 'register'
    );

    loginScreen.classList.toggle(
      'admin-access-mode',
      panel === 'admin'
    );
  }


  clearMessage(
    document.getElementById(
      'loginError'
    )
  );

  clearMessage(
    document.getElementById(
      'registerError'
    )
  );

  clearMessage(
    document.getElementById(
      'registerSuccess'
    )
  );

  clearMessage(
    document.getElementById(
      'adminLoginError'
    )
  );
}


function showPublicArea(
  panel = 'login'
) {
  document
    .getElementById(
      'appShell'
    )
    ?.classList
    .add('hidden');

  document
    .getElementById(
      'adminShell'
    )
    ?.classList
    .add('hidden');

  document
    .getElementById(
      'loginScreen'
    )
    ?.classList
    .remove('hidden');

  showAccessPanel(
    panel
  );
}


function showLogin(
  message = '',
  success = false
) {
  showPublicArea(
    'login'
  );

  const errorBox =
    document.getElementById(
      'loginError'
    );

  if (!errorBox) {
    return;
  }

  errorBox.textContent =
    message;

  errorBox.classList.toggle(
    'success-message',
    success
  );
}


/* =========================================================
   LOGIN DO CLIENTE
========================================================= */

function showAuthenticatedApp(user) {
  currentUser =
    user;

  document
    .getElementById(
      'currentUserName'
    )
    .textContent =
      user.holderName;

  document
    .getElementById(
      'loginScreen'
    )
    .classList
    .add('hidden');

  document
    .getElementById(
      'adminShell'
    )
    .classList
    .add('hidden');

  document
    .getElementById(
      'appShell'
    )
    .classList
    .remove('hidden');
}


async function handleLogin(event) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const errorBox =
    document.getElementById(
      'loginError'
    );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  button.disabled = true;

  button.textContent =
    'Entrando…';

  clearMessage(
    errorBox
  );

  try {
    const session =
      await api(
        '/api/v1/auth/login',
        {
          method: 'POST',

          body:
            JSON.stringify(
              data
            )
        }
      );

    sessionStorage.setItem(
      authTokenKey,
      session.accessToken
    );

    sessionStorage.setItem(
      activeContextKey,
      'client'
    );

    saldo = 0;
    extrato = [];
    currentAccountSummary = null;
    customerAccounts = [];

    showAuthenticatedApp(
      session
    );

    await Promise.all([
      loadCustomerAccounts(),
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    loadPage(
      'dashboard'
    );

    form.reset();

  } catch (error) {
    showApiError(
      errorBox,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Entrar';
  }
}


/* =========================================================
   CADASTRO
========================================================= */

async function handleRegistration(
  event
) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const errorBox =
    document.getElementById(
      'registerError'
    );

  const successBox =
    document.getElementById(
      'registerSuccess'
    );

  clearMessage(
    errorBox
  );

  clearMessage(
    successBox
  );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  if (
    data.password !==
    data.passwordConfirmation
  ) {
    showApiError(
      errorBox,
      new Error(
        'A confirmação da senha deve ser igual à senha informada.'
      )
    );

    return;
  }

  const payload = {
    fullName:
      data.fullName.trim(),

    cpf:
      data.cpf.replace(
        /\D/g,
        ''
      ),

    birthDate:
      data.birthDate,

    mobile:
      data.mobile.trim(),

    email:
      data.email.trim(),

    address: {
      postalCode:
        data.postalCode.replace(
          /\D/g,
          ''
        ),

      street:
        data.street.trim(),

      number:
        data.number.trim(),

      complement:
        data.complement?.trim()
          ? data.complement.trim()
          : null,

      district:
        data.district.trim(),

      city:
        data.city.trim(),

      state:
        data.state
          .trim()
          .toUpperCase()
    },

    username:
      data.username.trim(),

    password:
      data.password,

    accountType:
      data.accountType
  };


  button.disabled =
    true;

  button.textContent =
    'Criando cadastro…';


  try {
    await api(
      '/api/v1/auth/register',
      {
        method: 'POST',

        body:
          JSON.stringify(
            payload
          )
      }
    );

    const username =
      payload.username;

    form.reset();

    showLogin(
      'Cadastro concluído. Entre com o usuário e a senha que você acabou de criar.',
      true
    );

    const usernameInput =
      document.getElementById(
        'loginUsername'
      );

    if (usernameInput) {
      usernameInput.value =
        username;

      usernameInput.focus();
    }

  } catch (error) {
    showApiError(
      errorBox,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Criar cadastro';
  }
}


/* =========================================================
   LOGOUT DO CLIENTE
========================================================= */

async function performLogout() {
  try {
    await api(
      '/api/v1/auth/logout',
      {
        method: 'POST'
      }
    );

  } catch (_) {
    /*
     * A sessão local deve terminar
     * mesmo que o servidor não
     * responda ao logout.
     */

  } finally {
    clearClientSession();

    showLogin();
  }
}


/* =========================================================
   CONTAS DO CLIENTE
========================================================= */

function missingAccountType() {
  const types =
    new Set(
      customerAccounts.map(
        account =>
          account.accountType
      )
    );

  if (
    !types.has(
      'CURRENT'
    )
  ) {
    return 'CURRENT';
  }

  if (
    !types.has(
      'SAVINGS'
    )
  ) {
    return 'SAVINGS';
  }

  return null;
}


function selectedCustomerAccount() {
  return (
    customerAccounts.find(
      account =>
        account.selected
    ) ??
    null
  );
}


function updateAccountContext() {
  const context =
    document.getElementById(
      'accountContext'
    );

  const selector =
    document.getElementById(
      'accountSelector'
    );

  const openButton =
    document.getElementById(
      'openMissingAccountButton'
    );

  if (
    !context ||
    !selector ||
    !openButton
  ) {
    return;
  }

  if (
    customerAccounts.length === 0
  ) {
    context.classList.add(
      'hidden'
    );

    return;
  }

  context.classList.remove(
    'hidden'
  );

  selector.replaceChildren(
    ...customerAccounts.map(
      account => {
        const option =
          new Option(
            `${accountTypeLabel(
              account.accountType
            )} · Ag. ${account.branch} · ${account.accountNumber}`,
            account.id
          );

        option.selected =
          Boolean(
            account.selected
          );

        return option;
      }
    )
  );

  const missingType =
    missingAccountType();

  if (missingType) {
    openButton.classList.remove(
      'hidden'
    );

    openButton.textContent =
      missingType === 'CURRENT'
        ? 'Abrir conta corrente'
        : 'Abrir conta poupança';

  } else {
    openButton.classList.add(
      'hidden'
    );
  }
}


async function loadCustomerAccounts() {
  customerAccounts =
    await api(
      '/api/v1/auth/accounts'
    );

  updateAccountContext();

  return customerAccounts;
}


async function handleAccountSelection(
  event
) {
  const selector =
    event.currentTarget;

  const accountId =
    selector.value;

  const selected =
    selectedCustomerAccount();

  if (
    !accountId ||
    selected?.id ===
      accountId
  ) {
    return;
  }

  selector.disabled =
    true;

  try {
    await api(
      `/api/v1/auth/accounts/${accountId}/select`,
      {
        method: 'POST'
      }
    );

    saldo = 0;
    extrato = [];
    currentAccountSummary = null;

    await Promise.all([
      loadCustomerAccounts(),
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    const user =
      await api(
        '/api/v1/auth/me'
      );

    showAuthenticatedApp(
      user
    );

    loadPage(
      'dashboard'
    );

  } catch (error) {
    await loadCustomerAccounts()
      .catch(
        () => {}
      );

    const content =
      document.getElementById(
        'content'
      );

    if (content) {
      content.innerHTML = `
        <div class="card">
          <p id="accountSelectionError"></p>
        </div>
      `;

      showApiError(
        document.getElementById(
          'accountSelectionError'
        ),
        error
      );
    }

  } finally {
    selector.disabled =
      false;
  }
}


function openMissingAccountDialog() {
  const type =
    missingAccountType();

  if (!type) {
    return;
  }

  const dialog =
    document.getElementById(
      'openAccountDialog'
    );

  const select =
    document.getElementById(
      'newAccountType'
    );

  clearMessage(
    document.getElementById(
      'openAccountError'
    )
  );

  select.replaceChildren(
    new Option(
      accountTypeLabel(type),
      type
    )
  );

  dialog.showModal();
}


async function handleOpenAccount(
  event
) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const errorBox =
    document.getElementById(
      'openAccountError'
    );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  clearMessage(
    errorBox
  );

  button.disabled =
    true;

  button.textContent =
    'Abrindo conta…';

  try {
    const account =
      await api(
        '/api/v1/auth/accounts',
        {
          method: 'POST',

          body:
            JSON.stringify({
              accountType:
                data.accountType
            })
        }
      );

    await api(
      `/api/v1/auth/accounts/${account.id}/select`,
      {
        method: 'POST'
      }
    );

    document
      .getElementById(
        'openAccountDialog'
      )
      .close();

    saldo = 0;
    extrato = [];
    currentAccountSummary = null;

    await Promise.all([
      loadCustomerAccounts(),
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    loadPage(
      'dashboard'
    );

  } catch (error) {
    showApiError(
      errorBox,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Abrir conta';
  }
}


/* =========================================================
   RESUMO DA CONTA
========================================================= */

async function loadAccountSummary() {
  const summary =
    await api(
      '/api/v1/accounts/me/summary'
    );

  currentAccountSummary =
    summary;

  saldo =
    Number(
      summary.balance
    );

  const element =
    document.getElementById(
      'transferBalance'
    );

  if (element) {
    element.textContent =
      formatarMoeda(
        saldo
      );
  }

  return summary;
}


/* =========================================================
   CATÁLOGO DE BANCOS
========================================================= */

async function loadBanks() {
  const banks =
    await api(
      '/api/v1/banks'
    );

  const select =
    document.querySelector(
      '[name="bankCode"]'
    );

  if (!select) {
    return;
  }

  const placeholder =
    new Option(
      'Selecione a instituição',
      ''
    );

  placeholder.disabled =
    true;

  placeholder.selected =
    true;

  select.replaceChildren(
    placeholder,

    ...banks.map(
      bank =>
        new Option(
          `${bank.code} — ${bank.name}`,
          bank.code
        )
    )
  );
}


/* =========================================================
   PLACEHOLDERS
========================================================= */

function renderFeaturePlaceholder(
  title,
  description
) {
  const content =
    document.getElementById(
      'content'
    );

  content.innerHTML = `
    <div class="card highlight">

      <h2>
        ${escapeHtml(title)}
      </h2>

      <p>
        ${escapeHtml(description)}
      </p>

    </div>


    <div class="card">

      <h3>
        Módulo em adaptação
      </h3>

      <p>
        Esta área será conectada futuramente
        à API oficial do SPBank.

        Por enquanto, saldo, extrato e
        movimentações oficiais permanecem
        centralizados no backend.
      </p>


      <div class="actions">

        <button
          class="btn"
          type="button"
          onclick="loadPage('dashboard')"
        >
          Voltar ao dashboard
        </button>

      </div>

    </div>
  `;
}


/* =========================================================
   DASHBOARD
========================================================= */

function renderDashboard() {
  const content =
    document.getElementById(
      'content'
    );

  const recentEntries =
    extrato
      .slice()
      .sort(
        (a, b) =>
          new Date(b.data) -
          new Date(a.data)
      )
      .slice(
        0,
        5
      );

  const selectedAccount =
    selectedCustomerAccount();

  const missingType =
    missingAccountType();

  content.innerHTML = `
    <div class="header">

      <div>

        <h2>
          Dashboard
        </h2>

        <p class="summary-context">
          Dados oficiais da conta atualmente selecionada.
        </p>

      </div>

    </div>


    <div class="grid-3">

      <div class="card highlight">

        <p>
          Saldo disponível
        </p>

        <h2>
          ${formatarMoeda(saldo)}
        </h2>

      </div>


      <div class="card">

        <p>
          Plano atual
        </p>

        <h2>
          ${
            escapeHtml(
              accountPlanLabel(
                currentAccountSummary
                  ?.accountPlan
              )
            )
          }
        </h2>

      </div>


      <div class="card">

        <p>
          Modalidade
        </p>

        <h2>
          ${
            escapeHtml(
              accountTypeLabel(
                selectedAccount
                  ?.accountType
              )
            )
          }
        </h2>

      </div>

    </div>


    ${
      selectedAccount
        ? `
          <div class="card account-summary-card">

            <div class="header">

              <div>

                <h3>
                  Conta ativa
                </h3>

                <p class="summary-context">
                  ${
                    escapeHtml(
                      accountTypeLabel(
                        selectedAccount.accountType
                      )
                    )
                  }
                </p>

              </div>

              ${
                missingType
                  ? `
                    <button
                      class="btn-outline btn-compact"
                      type="button"
                      onclick="openMissingAccountDialog()"
                    >
                      ${
                        missingType ===
                        'CURRENT'
                          ? 'Abrir conta corrente'
                          : 'Abrir conta poupança'
                      }
                    </button>
                  `
                  : ''
              }

            </div>


            <div class="statement-details">

              <div class="statement-detail">

                <span>
                  Agência
                </span>

                <strong>
                  ${
                    escapeHtml(
                      selectedAccount.branch
                    )
                  }
                </strong>

              </div>


              <div class="statement-detail">

                <span>
                  Conta
                </span>

                <strong>
                  ${
                    escapeHtml(
                      selectedAccount.accountNumber
                    )
                  }
                </strong>

              </div>


              <div class="statement-detail">

                <span>
                  Saldo
                </span>

                <strong>
                  ${
                    formatarMoeda(
                      selectedAccount.balance
                    )
                  }
                </strong>

              </div>

            </div>

          </div>
        `
        : ''
    }


    <div class="menu-grid">

      <div
        class="menu-item"
        onclick="loadPage('pix')"
      >

        <div class="icon">

          <span class="material-symbols-outlined">
            window
          </span>

        </div>

        <span>
          Pix
        </span>

      </div>


      <div
        class="menu-item"
        onclick="loadPage('transfer')"
      >

        <div class="icon">

          <span class="material-symbols-outlined">
            sync_alt
          </span>

        </div>

        <span>
          Transferência
        </span>

      </div>


      <div
        class="menu-item"
        onclick="loadPage('extrato')"
      >

        <div class="icon">

          <span class="material-symbols-outlined">
            receipt
          </span>

        </div>

        <span>
          Extrato
        </span>

      </div>


      <div
        class="menu-item"
        onclick="loadPage('profile')"
      >

        <div class="icon">

          <span class="material-symbols-outlined">
            manage_accounts
          </span>

        </div>

        <span>
          Meus dados
        </span>

      </div>

    </div>


    <div class="card">

      <div class="header">

        <h3>
          Transações recentes
        </h3>

        <button
          class="btn-outline btn-compact"
          type="button"
          onclick="loadPage('extrato')"
        >
          Ver extrato
        </button>

      </div>


      ${
        recentEntries.length === 0
          ? `
            <p>
              Nenhuma movimentação registrada.
            </p>
          `
          : recentEntries
              .map(
                item => `
                  <div class="row">

                    <span>
                      ${escapeHtml(item.tipo)}
                    </span>

                    <strong
                      class="${
                        item.valor < 0
                          ? 'saida'
                          : 'entrada'
                      }"
                    >
                      ${formatarMoeda(item.valor)}
                    </strong>

                  </div>
                `
              )
              .join('')
      }

    </div>
  `;
}


/* =========================================================
   TRANSFERÊNCIA
========================================================= */

function renderTransferPage() {
  const content =
    document.getElementById(
      'content'
    );

  content.innerHTML = `
    <div class="card highlight">

      <h2>
        Transferência
      </h2>

      <p>
        Saldo disponível:

        <strong id="transferBalance">
          carregando…
        </strong>
      </p>

    </div>


    <form
      id="transferForm"
      class="card"
    >

      <label>

        Instituição de destino

        <select
          name="bankCode"
          required
        >

          <option value="">
            Carregando instituições…
          </option>

        </select>

      </label>


      <h3>
        Conta de destino
      </h3>


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

        <input
          name="branch"
          required
          placeholder="Agência"
        >

        <input
          name="accountNumber"
          required
          placeholder="Conta"
        >

      </div>


      <select
        name="accountType"
        required
      >

        <option value="CURRENT">
          Conta corrente (CC)
        </option>

        <option value="SAVINGS">
          Conta poupança (CP)
        </option>

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

        <input
          name="scheduledFor"
          type="date"
        >

      </label>


      <div
        id="transferError"
        class="error-message"
        role="alert"
      ></div>


      <button
        class="btn"
        type="submit"
      >
        Revisar transferência
      </button>

    </form>


    <section
      class="card"
      aria-labelledby="scheduledTitle"
    >

      <h3 id="scheduledTitle">
        Transferências agendadas
      </h3>

      <p
        id="scheduledMessage"
        class="operation-message"
        role="status"
      ></p>

      <div id="scheduledTransfers">
        Carregando agendamentos…
      </div>

    </section>


    <dialog
      id="transferReview"
      class="transfer-dialog"
      aria-labelledby="reviewTitle"
    >

      <h2 id="reviewTitle">
        Revise antes de confirmar
      </h2>


      <dl class="review-list">

        <dt>Destinatário</dt>
        <dd id="reviewRecipient"></dd>

        <dt>Instituição</dt>
        <dd id="reviewInstitution"></dd>

        <dt>Modalidade</dt>
        <dd id="reviewType"></dd>

        <dt>Conta</dt>
        <dd id="reviewAccount"></dd>

        <dt>Valor</dt>
        <dd id="reviewAmount"></dd>

        <dt>Tarifa</dt>
        <dd id="reviewFee"></dd>

        <dt>Total</dt>
        <dd id="reviewTotal"></dd>

        <dt>Data efetiva</dt>
        <dd id="reviewDate"></dd>

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


      <div
        id="reviewError"
        class="error-message"
        role="alert"
      ></div>


      <div class="dialog-actions">

        <button
          id="cancelTransfer"
          class="btn-secondary"
          type="button"
        >
          Voltar
        </button>


        <button
          id="confirmTransfer"
          class="btn"
          type="button"
        >
          Confirmar transferência
        </button>

      </div>

    </dialog>
  `;


  document
    .getElementById(
      'transferForm'
    )
    .addEventListener(
      'submit',
      submitTransfer
    );


  document
    .getElementById(
      'cancelTransfer'
    )
    .addEventListener(
      'click',
      cancelReview
    );


  document
    .getElementById(
      'confirmTransfer'
    )
    .addEventListener(
      'click',
      confirmTransfer
    );


  Promise
    .all([
      loadAccountSummary(),
      loadBanks(),
      loadScheduledTransfers()
    ])
    .catch(
      error =>
        showApiError(
          document.getElementById(
            'transferError'
          ),
          error
        )
    );
}


async function submitTransfer(event) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const errorBox =
    document.getElementById(
      'transferError'
    );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  const payload = {
    ...data,

    recipientDocument:
      data.recipientDocument.replace(
        /\D/g,
        ''
      ),

    amount:
      Number(
        data.amount
      ),

    scheduledFor:
      data.scheduledFor ||
      null
  };

  button.disabled =
    true;

  button.textContent =
    'Calculando…';

  clearMessage(
    errorBox
  );

  try {
    const preview =
      await api(
        '/api/v1/transfers/preview',
        {
          method: 'POST',

          body:
            JSON.stringify(
              payload
            )
        }
      );

    pendingTransfer =
      payload;

    pendingIdempotencyKey =
      crypto.randomUUID();

    setText(
      'reviewRecipient',
      `${payload.recipientName} — ${payload.recipientDocument}`
    );

    setText(
      'reviewInstitution',
      preview.institutionName
    );

    setText(
      'reviewType',
      transferTypeLabels[
        preview.type
      ] ||
      preview.type
    );

    setText(
      'reviewAccount',
      `${payload.bankCode} / ${payload.branch} / ${payload.accountNumber}`
    );

    setText(
      'reviewAmount',
      formatarMoeda(
        preview.amount
      )
    );

    setText(
      'reviewFee',
      formatarMoeda(
        preview.fee
      )
    );

    setText(
      'reviewTotal',
      formatarMoeda(
        preview.total
      )
    );

    setText(
      'reviewDate',
      preview.effectiveDate
    );

    document
      .getElementById(
        'transferConfirmationPassword'
      )
      .value = '';

    clearMessage(
      document.getElementById(
        'reviewError'
      )
    );

    document
      .getElementById(
        'transferReview'
      )
      .showModal();

    document
      .getElementById(
        'transferConfirmationPassword'
      )
      .focus();

  } catch (error) {
    showApiError(
      errorBox,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Revisar transferência';
  }
}


async function confirmTransfer() {
  if (
    !pendingTransfer ||
    !pendingIdempotencyKey
  ) {
    return;
  }

  const button =
    document.getElementById(
      'confirmTransfer'
    );

  const reviewError =
    document.getElementById(
      'reviewError'
    );

  const passwordInput =
    document.getElementById(
      'transferConfirmationPassword'
    );

  const confirmationPassword =
    passwordInput.value;

  if (!confirmationPassword) {
    showApiError(
      reviewError,
      new Error(
        'Informe sua senha para confirmar a transferência.'
      )
    );

    passwordInput.focus();

    return;
  }

  button.disabled =
    true;

  button.textContent =
    'Processando…';

  clearMessage(
    reviewError
  );

  try {
    const transfer =
      await api(
        '/api/v1/transfers',
        {
          method: 'POST',

          headers: {
            'Idempotency-Key':
              pendingIdempotencyKey
          },

          body:
            JSON.stringify({
              transfer:
                pendingTransfer,

              confirmationPassword
            })
        }
      );

    document
      .getElementById(
        'transferReview'
      )
      .close();

    passwordInput.value = '';

    pendingTransfer = null;
    pendingIdempotencyKey = null;

    renderTransferReceipt(
      transfer
    );

    await Promise.all([
      loadAccountSummary(),
      loadEntries(),
      loadScheduledTransfers(),
      loadCustomerAccounts()
    ]);

  } catch (error) {
    passwordInput.value = '';

    showApiError(
      reviewError,
      error
    );

    passwordInput.focus();

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Confirmar transferência';
  }
}


function cancelReview() {
  const dialog =
    document.getElementById(
      'transferReview'
    );

  const passwordInput =
    document.getElementById(
      'transferConfirmationPassword'
    );

  if (passwordInput) {
    passwordInput.value = '';
  }

  pendingTransfer = null;
  pendingIdempotencyKey = null;

  dialog.close();
}


async function loadScheduledTransfers() {
  const container =
    document.getElementById(
      'scheduledTransfers'
    );

  if (!container) {
    return;
  }

  const transfers =
    await api(
      '/api/v1/transfers/scheduled'
    );

  if (
    transfers.length === 0
  ) {
    container.innerHTML =
      '<p>Nenhuma transferência agendada.</p>';

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

          ${
            transfers
              .map(
                transfer => `
                  <tr>

                    <td>
                      ${
                        escapeHtml(
                          formatDate(
                            transfer.scheduledFor
                          )
                        )
                      }
                    </td>

                    <td>
                      ${
                        escapeHtml(
                          transfer.recipientName
                        )
                      }
                    </td>

                    <td>
                      ${
                        escapeHtml(
                          transferTypeLabels[
                            transfer.type
                          ] ||
                          transfer.type
                        )
                      }
                    </td>

                    <td>
                      ${
                        escapeHtml(
                          formatarMoeda(
                            transfer.amount
                          )
                        )
                      }
                    </td>

                    <td>

                      <button
                        class="scheduled-cancel"
                        type="button"
                        data-cancel-scheduled="${
                          escapeHtml(
                            transfer.id
                          )
                        }"
                      >
                        Cancelar
                      </button>

                    </td>

                  </tr>
                `
              )
              .join('')
          }

        </tbody>

      </table>

    </div>
  `;


  container
    .querySelectorAll(
      '[data-cancel-scheduled]'
    )
    .forEach(
      button => {
        button.addEventListener(
          'click',
          () =>
            cancelScheduledTransfer(
              button
            )
        );
      }
    );
}


async function cancelScheduledTransfer(
  button
) {
  const id =
    button.dataset
      .cancelScheduled;

  const message =
    document.getElementById(
      'scheduledMessage'
    );

  button.disabled =
    true;

  message.textContent =
    'Cancelando agendamento…';

  try {
    await api(
      `/api/v1/transfers/${id}`,
      {
        method: 'DELETE'
      }
    );

    message.textContent =
      'Transferência agendada cancelada.';

    await loadScheduledTransfers();

  } catch (error) {
    showApiError(
      message,
      error
    );

    button.disabled =
      false;
  }
}


function renderTransferReceipt(
  transfer
) {
  const content =
    document.getElementById(
      'content'
    );

  content.innerHTML = `
    <section
      class="card receipt"
      aria-labelledby="receiptTitle"
    >

      <h2 id="receiptTitle">
        Transferência registrada
      </h2>


      <p>
        Status:
        <strong id="receiptStatus"></strong>
      </p>


      <dl class="review-list">

        <dt>Identificador</dt>
        <dd id="receiptId"></dd>

        <dt>Destinatário</dt>
        <dd id="receiptRecipient"></dd>

        <dt>Conta</dt>
        <dd id="receiptAccount"></dd>

        <dt>Modalidade</dt>
        <dd id="receiptType"></dd>

        <dt>Valor</dt>
        <dd id="receiptAmount"></dd>

        <dt>Tarifa</dt>
        <dd id="receiptFee"></dd>

        <dt>Data agendada</dt>
        <dd id="receiptDate"></dd>

        <dt>Solicitada em</dt>
        <dd id="receiptRequestedAt"></dd>

        <dt>Processada em</dt>
        <dd id="receiptProcessedAt"></dd>

        ${
          transfer.type === 'TED'
            ? `
              <dt>Liquidação</dt>
              <dd id="receiptSettlement"></dd>
            `
            : ''
        }

      </dl>


      <p>
        Saldo atualizado:

        <strong id="transferBalance">
          carregando…
        </strong>
      </p>


      <button
        id="newTransfer"
        class="btn"
        type="button"
      >
        Nova transferência
      </button>

    </section>


    <section class="card">

      <h2>
        Lançamentos recentes
      </h2>


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


  setText(
    'receiptStatus',
    transferStatusLabels[
      transfer.status
    ] ||
    transfer.status
  );

  setText(
    'receiptId',
    transfer.id
  );

  setText(
    'receiptRecipient',
    transfer.recipientName
  );

  setText(
    'receiptAccount',
    `${transfer.bankCode} / ${transfer.branch} / ${transfer.accountNumber}`
  );

  setText(
    'receiptType',
    transferTypeLabels[
      transfer.type
    ] ||
    transfer.type
  );

  setText(
    'receiptAmount',
    formatarMoeda(
      transfer.amount
    )
  );

  setText(
    'receiptFee',
    formatarMoeda(
      transfer.fee
    )
  );

  setText(
    'receiptDate',
    transfer.scheduledFor
      ? formatDate(
          transfer.scheduledFor
        )
      : 'Imediata'
  );

  setText(
    'receiptRequestedAt',
    formatDateTime(
      transfer.requestedAt
    )
  );

  setText(
    'receiptProcessedAt',
    transfer.processedAt
      ? formatDateTime(
          transfer.processedAt
        )
      : 'Aguardando'
  );

  if (
    transfer.type === 'TED'
  ) {
    setText(
      'receiptSettlement',
      transfer.settlementReference
        ? `${transfer.settlementReference} (ambiente simulado)`
        : 'Aguardando processamento'
    );
  }

  document
    .getElementById(
      'newTransfer'
    )
    .addEventListener(
      'click',
      () =>
        loadPage(
          'transfer'
        )
    );
}


/* =========================================================
   EXTRATO
========================================================= */

async function loadEntries() {
  const tbody =
    document.getElementById(
      'recentEntries'
    );

  const entries =
    await api(
      '/api/v1/accounts/me/entries?limit=20'
    );

  replaceOfficialEntries(
    entries
  );

  if (!tbody) {
    return;
  }

  tbody.replaceChildren();

  for (
    const entry of entries
  ) {
    const row =
      document.createElement(
        'tr'
      );

    const values = [
      formatDateTime(
        entry.occurredAt
      ),

      entry.description,

      entry.counterpartyName ||
      '—',

      transferModalityLabel(
        entry.operationType
      ),

      entry.direction === 'DEBIT'
        ? 'Débito'
        : 'Crédito',

      formatarMoeda(
        entry.amount
      ),

      formatarMoeda(
        entry.balanceAfter
      )
    ];

    for (
      const value of values
    ) {
      const cell =
        document.createElement(
          'td'
        );

      cell.textContent =
        value;

      row.appendChild(
        cell
      );
    }

    tbody.appendChild(
      row
    );
  }
}


function replaceOfficialEntries(
  entries
) {
  extrato =
    entries.map(
      entry => ({
        id:
          entry.id,

        source:
          'spbank-api',

        category:
          'transfer',

        tipo:
          entry.type === 'FEE'
            ? 'Tarifa de transferência'
            : entry.description,

        descricao:
          entry.direction === 'DEBIT'
            ? 'Débito em conta'
            : 'Crédito em conta',

        valor:
          (
            entry.direction === 'DEBIT'
              ? -1
              : 1
          )
          *
          Number(
            entry.amount
          ),

        data:
          entry.occurredAt,

        direction:
          entry.direction,

        entryType:
          entry.type,

        counterpartyName:
          entry.counterpartyName,

        counterpartyBankCode:
          entry.counterpartyBankCode,

        operationType:
          entry.operationType,

        balanceAfter:
          Number(
            entry.balanceAfter
          ),

        referenceId:
          entry.referenceId
      })
    );
}


async function refreshOfficialStatement() {
  const entries =
    await api(
      '/api/v1/accounts/me/entries?limit=100'
    );

  replaceOfficialEntries(
    entries
  );

  return entries;
}


function filtrarExtrato(
  tipo,
  botao
) {
  filtroAtual =
    tipo;

  document
    .querySelectorAll(
      '.filtro-btn'
    )
    .forEach(
      btn => {
        btn.classList.remove(
          'ativo'
        );
      }
    );

  if (botao) {
    botao.classList.add(
      'ativo'
    );
  }

  renderExtrato();

  renderStatementSummary();
}


function atualizarFiltroAtivo() {
  const mapa = {
    todos:
      'btnTodos',

    transfer:
      'btnTransfer'
  };

  document
    .querySelectorAll(
      '.filtro-btn'
    )
    .forEach(
      btn => {
        btn.classList.remove(
          'ativo'
        );
      }
    );

  const ativo =
    document.getElementById(
      mapa[filtroAtual]
    );

  if (ativo) {
    ativo.classList.add(
      'ativo'
    );
  }
}


function getFilteredStatementEntries() {
  return extrato
    .slice()
    .sort(
      (a, b) =>
        new Date(b.data) -
        new Date(a.data)
    )
    .filter(
      item => {
        if (
          filtroAtual ===
          'todos'
        ) {
          return true;
        }

        if (
          filtroAtual ===
          'transfer'
        ) {
          return (
            item.category ===
            'transfer'
          );
        }

        return false;
      }
    );
}


function renderStatementSummary() {
  const movimentos =
    getFilteredStatementEntries();

  let entradas = 0;
  let saidas = 0;

  movimentos.forEach(
    item => {
      if (
        item.valor >= 0
      ) {
        entradas +=
          Number(
            item.valor
          );

      } else {
        saidas +=
          Math.abs(
            Number(
              item.valor
            )
          );
      }
    }
  );

  renderResumoValores(
    entradas,
    saidas
  );

  const contexto =
    document.getElementById(
      'resumoContexto'
    );

  if (contexto) {
    contexto.textContent =
      filtroAtual === 'todos'
        ? 'Todas as movimentações carregadas'
        : 'Somente transferências';
  }
}


function renderResumoValores(
  entradas,
  saidas
) {
  const resultado =
    entradas - saidas;

  const resultadoClasse =
    resultado < 0
      ? 'saida'
      : 'resultado';

  const resumo =
    document.getElementById(
      'resumoValores'
    );

  if (!resumo) {
    return;
  }

  resumo.innerHTML = `
    <div class="resumo-item">

      <h4>
        Entradas no filtro
      </h4>

      <strong class="entrada">
        ${formatarMoeda(entradas)}
      </strong>

    </div>


    <div class="resumo-item">

      <h4>
        Saídas no filtro
      </h4>

      <strong class="saida">
        ${formatarMoeda(saidas)}
      </strong>

    </div>


    <div class="resumo-item">

      <h4>
        Resultado no filtro
      </h4>

      <strong
        class="${resultadoClasse}"
      >
        ${formatarMoeda(resultado)}
      </strong>

    </div>


    <div class="resumo-item">

      <h4>
        Saldo atual
      </h4>

      <strong>
        ${formatarMoeda(saldo)}
      </strong>

    </div>
  `;
}


function renderExtrato() {
  const lista =
    getFilteredStatementEntries();

  const element =
    document.getElementById(
      'listaExtrato'
    );

  if (!element) {
    return;
  }

  if (
    lista.length === 0
  ) {
    element.innerHTML = `
      <div class="card">
        <p>
          Nenhuma movimentação.
        </p>
      </div>
    `;

    return;
  }

  element.innerHTML =
    lista
      .map(
        item => {
          const counterpartyLabel =
            item.entryType === 'FEE'
              ? 'Relacionado a'
              : item.direction === 'CREDIT'
                ? 'Recebido de'
                : 'Enviado para';

          return `
            <div
              class="
                card
                statement-card
              "
            >

              <div class="row">

                <strong>
                  ${escapeHtml(item.tipo)}
                </strong>

                <span
                  class="${
                    item.valor < 0
                      ? 'saida'
                      : 'entrada'
                  }"
                >
                  ${formatarMoeda(item.valor)}
                </span>

              </div>


              <div class="statement-details">

                <div class="statement-detail">

                  <span>
                    Data e hora
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        formatDateTime(
                          item.data
                        )
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    ${counterpartyLabel}
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        item.counterpartyName
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Banco
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        item.counterpartyBankCode
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Modalidade
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        transferModalityLabel(
                          item.operationType
                        )
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Natureza
                  </span>

                  <strong>
                    ${
                      item.direction === 'DEBIT'
                        ? 'Débito'
                        : 'Crédito'
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Saldo após
                  </span>

                  <strong>
                    ${
                      formatarMoeda(
                        item.balanceAfter
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Identificador
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        item.referenceId
                      )
                    }
                  </strong>

                </div>

              </div>

            </div>
          `;
        }
      )
      .join('');
}


function renderExtratoPage() {
  const content =
    document.getElementById(
      'content'
    );

  content.innerHTML = `
    <div class="card highlight">

      <h2>
        Extrato
      </h2>

      <p>
        Movimentações oficiais da conta selecionada.
      </p>

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

    </div>


    <div class="card">

      <h3>
        Resumo do extrato
      </h3>

      <p
        id="resumoContexto"
        class="summary-context"
      ></p>

      <div
        id="resumoValores"
        class="resumo-valores"
      ></div>

    </div>


    <div id="listaExtrato"></div>
  `;

  atualizarFiltroAtivo();

  renderExtrato();

  renderStatementSummary();

  Promise
    .all([
      loadAccountSummary(),
      refreshOfficialStatement()
    ])
    .then(
      () => {
        if (
          document.getElementById(
            'listaExtrato'
          )
        ) {
          renderExtrato();

          renderStatementSummary();
        }
      }
    )
    .catch(
      error => {
        const lista =
          document.getElementById(
            'listaExtrato'
          );

        if (!lista) {
          return;
        }

        lista.innerHTML = `
          <div class="card">
            <p id="statementError"></p>
          </div>
        `;

        showApiError(
          document.getElementById(
            'statementError'
          ),
          error
        );
      }
    );
}


/* =========================================================
   PERFIL
========================================================= */

function renderProfilePage() {
  const content =
    document.getElementById(
      'content'
    );

  content.innerHTML = `
    <div class="card highlight">

      <h2>
        Meus dados
      </h2>

      <p>
        Consulte seus dados cadastrais
        e mantenha contato e endereço atualizados.
      </p>

    </div>


    <form
      id="profileForm"
      class="card"
    >

      <div class="header">

        <div>

          <h3>
            Dados cadastrais
          </h3>

          <p class="summary-context">
            Nome, CPF e nascimento identificam
            o cliente e não podem ser alterados.
          </p>

        </div>

      </div>


      <fieldset class="form-section">

        <legend>
          Identificação
        </legend>


        <div class="grid-2">

          <label>

            Nome completo

            <input
              id="profileFullName"
              disabled
            >

          </label>


          <label>

            CPF

            <input
              id="profileCpf"
              disabled
            >

          </label>

        </div>


        <label>

          Data de nascimento

          <input
            id="profileBirthDate"
            type="date"
            disabled
          >

        </label>

      </fieldset>


      <fieldset class="form-section">

        <legend>
          Contato
        </legend>


        <div class="grid-2">

          <label>

            Celular

            <input
              id="profileMobile"
              name="mobile"
              type="tel"
              required
            >

          </label>


          <label>

            E-mail

            <input
              id="profileEmail"
              name="email"
              type="email"
              required
            >

          </label>

        </div>

      </fieldset>


      <fieldset class="form-section">

        <legend>
          Endereço
        </legend>


        <div class="grid-2">

          <label>

            CEP

            <input
              id="profilePostalCode"
              name="postalCode"
              required
            >

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

          <input
            id="profileStreet"
            name="street"
            required
          >

        </label>


        <div class="grid-2">

          <label>

            Número

            <input
              id="profileNumber"
              name="number"
              required
            >

          </label>


          <label>

            Complemento

            <input
              id="profileComplement"
              name="complement"
            >

          </label>

        </div>


        <div class="grid-2">

          <label>

            Bairro

            <input
              id="profileDistrict"
              name="district"
              required
            >

          </label>


          <label>

            Cidade

            <input
              id="profileCity"
              name="city"
              required
            >

          </label>

        </div>

      </fieldset>


      <div
        id="profileMessage"
        class="error-message"
        role="status"
      ></div>


      <button
        class="btn"
        type="submit"
      >
        Salvar alterações
      </button>

    </form>


    <form
      id="passwordForm"
      class="card"
    >

      <div class="header">

        <div>

          <h3>
            Alterar senha
          </h3>

          <p class="summary-context">
            Confirme sua senha atual antes
            de definir uma nova.
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


      <div
        id="passwordMessage"
        class="error-message"
        role="status"
      ></div>


      <button
        class="btn"
        type="submit"
      >
        Alterar senha
      </button>

    </form>
  `;


  document
    .getElementById(
      'profileForm'
    )
    .addEventListener(
      'submit',
      handleProfileUpdate
    );


  document
    .getElementById(
      'passwordForm'
    )
    .addEventListener(
      'submit',
      handlePasswordChange
    );


  loadProfile()
    .catch(
      error =>
        showApiError(
          document.getElementById(
            'profileMessage'
          ),
          error
        )
    );
}


async function loadProfile() {
  const profile =
    await api(
      '/api/v1/auth/profile'
    );

  document
    .getElementById(
      'profileFullName'
    )
    .value =
      profile.fullName ??
      '';

  document
    .getElementById(
      'profileCpf'
    )
    .value =
      formatCpf(
        profile.cpf
      );

  document
    .getElementById(
      'profileBirthDate'
    )
    .value =
      profile.birthDate ??
      '';

  document
    .getElementById(
      'profileMobile'
    )
    .value =
      profile.mobile ??
      '';

  document
    .getElementById(
      'profileEmail'
    )
    .value =
      profile.email ??
      '';

  document
    .getElementById(
      'profilePostalCode'
    )
    .value =
      profile.address?.postalCode ??
      '';

  document
    .getElementById(
      'profileStreet'
    )
    .value =
      profile.address?.street ??
      '';

  document
    .getElementById(
      'profileNumber'
    )
    .value =
      profile.address?.number ??
      '';

  document
    .getElementById(
      'profileComplement'
    )
    .value =
      profile.address?.complement ??
      '';

  document
    .getElementById(
      'profileDistrict'
    )
    .value =
      profile.address?.district ??
      '';

  document
    .getElementById(
      'profileCity'
    )
    .value =
      profile.address?.city ??
      '';

  document
    .getElementById(
      'profileState'
    )
    .value =
      profile.address?.state ??
      '';

  return profile;
}


async function handleProfileUpdate(
  event
) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const message =
    document.getElementById(
      'profileMessage'
    );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  const payload = {
    mobile:
      data.mobile.trim(),

    email:
      data.email.trim(),

    address: {
      postalCode:
        data.postalCode.replace(
          /\D/g,
          ''
        ),

      street:
        data.street.trim(),

      number:
        data.number.trim(),

      complement:
        data.complement?.trim()
          ? data.complement.trim()
          : null,

      district:
        data.district.trim(),

      city:
        data.city.trim(),

      state:
        data.state
          .trim()
          .toUpperCase()
    }
  };

  clearMessage(
    message
  );

  button.disabled =
    true;

  button.textContent =
    'Salvando…';

  try {
    await api(
      '/api/v1/auth/profile',
      {
        method: 'PUT',

        body:
          JSON.stringify(
            payload
          )
      }
    );

    await loadProfile();

    showSuccess(
      message,
      'Dados atualizados com sucesso.'
    );

  } catch (error) {
    showApiError(
      message,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Salvar alterações';
  }
}


async function handlePasswordChange(
  event
) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const message =
    document.getElementById(
      'passwordMessage'
    );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  clearMessage(
    message
  );

  if (
    data.newPassword !==
    data.newPasswordConfirmation
  ) {
    showApiError(
      message,
      new Error(
        'A confirmação da nova senha deve ser igual à nova senha.'
      )
    );

    return;
  }

  button.disabled =
    true;

  button.textContent =
    'Alterando…';

  try {
    await api(
      '/api/v1/auth/password',
      {
        method: 'PUT',

        body:
          JSON.stringify({
            currentPassword:
              data.currentPassword,

            newPassword:
              data.newPassword,

            newPasswordConfirmation:
              data.newPasswordConfirmation
          })
      }
    );

    showSuccess(
      message,
      'Senha alterada com sucesso.'
    );

  } catch (error) {
    showApiError(
      message,
      error
    );

  } finally {
    form.reset();

    button.disabled =
      false;

    button.textContent =
      'Alterar senha';
  }
}


/* =========================================================
   NAVEGAÇÃO DO CLIENTE
========================================================= */

function loadPage(page) {
  if (
    page === 'dashboard'
  ) {
    renderDashboard();
    return;
  }

  if (
    page === 'transfer'
  ) {
    renderTransferPage();
    return;
  }

  if (
    page === 'extrato'
  ) {
    renderExtratoPage();
    return;
  }

  if (
    page === 'profile'
  ) {
    renderProfilePage();
    return;
  }

  if (
    page === 'pix'
  ) {
    renderFeaturePlaceholder(
      'Pix',
      'O módulo Pix do SPBank será integrado ao backend em sua etapa própria.'
    );

    return;
  }

  if (
    page === 'invest'
  ) {
    renderFeaturePlaceholder(
      'Investimentos',
      'O módulo de investimentos do SPBank ainda não possui API oficial.'
    );

    return;
  }

  if (
    page === 'cartao'
  ) {
    renderFeaturePlaceholder(
      'Cartão',
      'O módulo de cartão do SPBank ainda não possui API oficial.'
    );

    return;
  }

  renderDashboard();
}


/* =========================================================
   LOGIN GERENCIAL
========================================================= */

function showAdminApp(
  administrator
) {
  document
    .getElementById(
      'loginScreen'
    )
    .classList
    .add('hidden');

  document
    .getElementById(
      'appShell'
    )
    .classList
    .add('hidden');

  document
    .getElementById(
      'adminShell'
    )
    .classList
    .remove('hidden');

  document
    .getElementById(
      'currentAdministratorName'
    )
    .textContent =
      administrator.displayName ??
      'Gerente SPBank';
}


async function handleAdminLogin(
  event
) {
  event.preventDefault();

  const form =
    event.currentTarget;

  const button =
    form.querySelector(
      'button[type="submit"]'
    );

  const errorBox =
    document.getElementById(
      'adminLoginError'
    );

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  clearMessage(
    errorBox
  );

  button.disabled =
    true;

  button.textContent =
    'Entrando…';

  try {
    const session =
      await adminApi(
        '/api/v1/admin/auth/login',
        {
          method: 'POST',

          body:
            JSON.stringify({
              username:
                data.username,

              password:
                data.password
            })
        }
      );

    sessionStorage.setItem(
      adminAuthTokenKey,
      session.accessToken
    );

    sessionStorage.setItem(
      adminNameKey,
      session.displayName
    );

    sessionStorage.setItem(
      activeContextKey,
      'admin'
    );

    showAdminApp(
      session
    );

    form.reset();

    await loadAdminAccounts();

  } catch (error) {
    showApiError(
      errorBox,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      'Entrar na área gerencial';
  }
}


/* =========================================================
   LOGOUT GERENCIAL
========================================================= */

async function performAdminLogout() {
  try {
    await adminApi(
      '/api/v1/admin/auth/logout',
      {
        method: 'POST'
      }
    );

  } catch (_) {
    /*
     * O contexto local será encerrado
     * mesmo que o backend não responda.
     */

  } finally {
    clearAdminSession();

    showPublicArea(
      'admin'
    );
  }
}


/* =========================================================
   CONTAS DA ADMINISTRAÇÃO
========================================================= */

async function loadAdminAccounts() {
  const container =
    document.getElementById(
      'adminAccounts'
    );

  const errorBox =
    document.getElementById(
      'adminGlobalError'
    );

  clearMessage(
    errorBox
  );

  if (container) {
    container.innerHTML = `
      <p>
        Carregando contas…
      </p>
    `;
  }

  try {
    adminAccounts =
      await adminApi(
        '/api/v1/admin/accounts'
      );

    renderAdminAccounts();

  } catch (error) {
    if (container) {
      container.innerHTML = `
        <p>
          Não foi possível carregar as contas.
        </p>
      `;
    }

    showApiError(
      errorBox,
      error
    );

    throw error;
  }
}


function renderAdminAccounts() {
  const container =
    document.getElementById(
      'adminAccounts'
    );

  if (!container) {
    return;
  }

  if (
    adminAccounts.length === 0
  ) {
    container.innerHTML = `
      <p>
        Nenhuma conta disponível.
      </p>
    `;

    return;
  }

  container.innerHTML =
    adminAccounts
      .map(
        account => {
          const targetPlan =
            account.accountPlan ===
            'PLUS'
              ? 'STANDARD'
              : 'PLUS';

          const actionLabel =
            targetPlan === 'PLUS'
              ? 'Promover para Plus'
              : 'Rebaixar para Standard';

          return `
            <article class="admin-account-card">

              <div class="admin-account-head">

                <div>

                  <h3>
                    ${
                      escapeHtml(
                        account.holderName
                      )
                    }
                  </h3>

                  <p class="summary-context">
                    CPF:
                    ${
                      escapeHtml(
                        account.maskedDocument
                      )
                    }
                  </p>

                </div>


                <span
                  class="
                    plan-badge
                    ${
                      account.accountPlan ===
                      'PLUS'
                        ? 'plus'
                        : 'standard'
                    }
                  "
                >
                  ${
                    escapeHtml(
                      accountPlanLabel(
                        account.accountPlan
                      )
                    )
                  }
                </span>

              </div>


              <div class="statement-details">

                <div class="statement-detail">

                  <span>
                    Modalidade
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        accountTypeLabel(
                          account.accountType
                        )
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Agência
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        account.branch
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Conta
                  </span>

                  <strong>
                    ${
                      escapeHtml(
                        account.accountNumber
                      )
                    }
                  </strong>

                </div>


                <div class="statement-detail">

                  <span>
                    Situação
                  </span>

                  <strong>
                    ${
                      account.active
                        ? 'Ativa'
                        : 'Inativa'
                    }
                  </strong>

                </div>

              </div>


              <div class="admin-plan-action">

                <label>

                  Motivo da alteração

                  <input
                    type="text"
                    maxlength="240"
                    data-plan-reason="${
                      escapeHtml(
                        account.accountId
                      )
                    }"
                    placeholder="Informe o motivo da decisão"
                    ${
                      account.active
                        ? ''
                        : 'disabled'
                    }
                  >

                </label>


                <div class="actions">

                  <button
                    class="btn"
                    type="button"
                    data-plan-action="${
                      escapeHtml(
                        account.accountId
                      )
                    }"
                    data-target-plan="${
                      escapeHtml(
                        targetPlan
                      )
                    }"
                    ${
                      account.active
                        ? ''
                        : 'disabled'
                    }
                  >
                    ${actionLabel}
                  </button>


                  <button
                    class="btn-outline"
                    type="button"
                    data-plan-history="${
                      escapeHtml(
                        account.accountId
                      )
                    }"
                    data-account-name="${
                      escapeHtml(
                        account.holderName
                      )
                    }"
                  >
                    Ver histórico
                  </button>

                </div>


                <p
                  class="error-message admin-row-message"
                  data-plan-message="${
                    escapeHtml(
                      account.accountId
                    )
                  }"
                ></p>

              </div>

            </article>
          `;
        }
      )
      .join('');


  container
    .querySelectorAll(
      '[data-plan-action]'
    )
    .forEach(
      button => {
        button.addEventListener(
          'click',
          () =>
            changeAdminAccountPlan(
              button
            )
        );
      }
    );


  container
    .querySelectorAll(
      '[data-plan-history]'
    )
    .forEach(
      button => {
        button.addEventListener(
          'click',
          () =>
            openPlanHistory(
              button.dataset
                .planHistory,

              button.dataset
                .accountName
            )
        );
      }
    );
}


async function changeAdminAccountPlan(
  button
) {
  const accountId =
    button.dataset
      .planAction;

  const targetPlan =
    button.dataset
      .targetPlan;

  const reasonInput =
    document.querySelector(
      `[data-plan-reason="${accountId}"]`
    );

  const message =
    document.querySelector(
      `[data-plan-message="${accountId}"]`
    );

  const reason =
    reasonInput?.value
      .trim() ??
    '';

  clearMessage(
    message
  );

  if (!reason) {
    showApiError(
      message,
      new Error(
        'Informe o motivo da alteração do plano.'
      )
    );

    reasonInput?.focus();

    return;
  }

  button.disabled =
    true;

  const originalLabel =
    button.textContent;

  button.textContent =
    'Salvando…';

  try {
    await adminApi(
      `/api/v1/admin/accounts/${accountId}/plan`,
      {
        method: 'PUT',

        body:
          JSON.stringify({
            accountPlan:
              targetPlan,

            reason
          })
      }
    );


    /*
     * Primeiro atualizamos a lista.
     * Só depois mostramos a mensagem,
     * evitando que loadAdminAccounts()
     * a apague imediatamente.
     */
    await loadAdminAccounts();


    showSuccess(
      document.getElementById(
        'adminGlobalError'
      ),
      'Plano alterado e histórico registrado com sucesso.'
    );

  } catch (error) {
    showApiError(
      message,
      error
    );

  } finally {
    button.disabled =
      false;

    button.textContent =
      originalLabel;
  }
}


/* =========================================================
   HISTÓRICO DE PLANOS
========================================================= */

async function openPlanHistory(
  accountId,
  accountName
) {
  const dialog =
    document.getElementById(
      'planHistoryDialog'
    );

  const content =
    document.getElementById(
      'planHistoryContent'
    );

  const account =
    document.getElementById(
      'planHistoryAccount'
    );

  account.textContent =
    accountName ||
    '';

  content.innerHTML = `
    <p>
      Carregando histórico…
    </p>
  `;

  dialog.showModal();

  try {
    const history =
      await adminApi(
        `/api/v1/admin/accounts/${accountId}/plan-history`
      );

    renderPlanHistory(
      history
    );

  } catch (error) {
    content.innerHTML = `
      <p
        id="planHistoryError"
        class="error-message"
      ></p>
    `;

    showApiError(
      document.getElementById(
        'planHistoryError'
      ),
      error
    );
  }
}


function renderPlanHistory(
  history
) {
  const content =
    document.getElementById(
      'planHistoryContent'
    );

  if (
    history.length === 0
  ) {
    content.innerHTML = `
      <p>
        Nenhuma alteração de plano registrada.
      </p>
    `;

    return;
  }

  content.innerHTML = `
    <div class="plan-history-list">

      ${
        history
          .map(
            change => `
              <article class="plan-history-item">

                <div class="row">

                  <strong>
                    ${
                      escapeHtml(
                        accountPlanLabel(
                          change.previousPlan
                        )
                      )
                    }
                    →
                    ${
                      escapeHtml(
                        accountPlanLabel(
                          change.newPlan
                        )
                      )
                    }
                  </strong>

                  <span>
                    ${
                      escapeHtml(
                        formatDateTime(
                          change.changedAt
                        )
                      )
                    }
                  </span>

                </div>


                <div class="statement-details">

                  <div class="statement-detail">

                    <span>
                      Gerente
                    </span>

                    <strong>
                      ${
                        escapeHtml(
                          change.administratorName
                        )
                      }
                    </strong>

                  </div>


                  <div class="statement-detail">

                    <span>
                      Motivo
                    </span>

                    <strong>
                      ${
                        escapeHtml(
                          change.reason
                        )
                      }
                    </strong>

                  </div>

                </div>

              </article>
            `
          )
          .join('')
      }

    </div>
  `;
}


/* =========================================================
   INICIALIZAÇÃO
========================================================= */

async function initializeApp() {
  /* -------------------------------------------------------
     TEMA
  ------------------------------------------------------- */

  initializeTheme();


  /* -------------------------------------------------------
     ACESSOS DA TELA PÚBLICA
  ------------------------------------------------------- */

  document
    .querySelectorAll(
      '[data-public-tab]'
    )
    .forEach(
      control => {
        control.addEventListener(
          'click',
          () =>
            showAccessPanel(
              control.dataset
                .publicTab
            )
        );
      }
    );


  /* -------------------------------------------------------
     FORMULÁRIOS PÚBLICOS
  ------------------------------------------------------- */

  document
    .getElementById(
      'loginForm'
    )
    .addEventListener(
      'submit',
      handleLogin
    );


  document
    .getElementById(
      'registerForm'
    )
    .addEventListener(
      'submit',
      handleRegistration
    );


  document
    .getElementById(
      'adminLoginForm'
    )
    .addEventListener(
      'submit',
      handleAdminLogin
    );


  /* -------------------------------------------------------
     CLIENTE
  ------------------------------------------------------- */

  document
    .getElementById(
      'logoutButton'
    )
    .addEventListener(
      'click',
      performLogout
    );


  document
    .getElementById(
      'accountSelector'
    )
    .addEventListener(
      'change',
      handleAccountSelection
    );


  document
    .getElementById(
      'openMissingAccountButton'
    )
    .addEventListener(
      'click',
      openMissingAccountDialog
    );


  document
    .getElementById(
      'openAccountForm'
    )
    .addEventListener(
      'submit',
      handleOpenAccount
    );


  document
    .getElementById(
      'cancelOpenAccountButton'
    )
    .addEventListener(
      'click',
      () => {
        document
          .getElementById(
            'openAccountDialog'
          )
          .close();
      }
    );


  /* -------------------------------------------------------
     ADMINISTRAÇÃO
  ------------------------------------------------------- */

  document
    .getElementById(
      'adminLogoutButton'
    )
    .addEventListener(
      'click',
      performAdminLogout
    );


  document
    .getElementById(
      'refreshAdminAccountsButton'
    )
    .addEventListener(
      'click',
      () =>
        loadAdminAccounts()
          .catch(
            () => {}
          )
    );


  document
    .getElementById(
      'closePlanHistoryButton'
    )
    .addEventListener(
      'click',
      () => {
        document
          .getElementById(
            'planHistoryDialog'
          )
          .close();
      }
    );


  /* -------------------------------------------------------
     RECUPERAÇÃO DO CONTEXTO
  ------------------------------------------------------- */

  const context =
    sessionStorage.getItem(
      activeContextKey
    );

  const clientToken =
    sessionStorage.getItem(
      authTokenKey
    );

  const adminToken =
    sessionStorage.getItem(
      adminAuthTokenKey
    );


  /* -------------------------------------------------------
     ADMIN ATIVO
  ------------------------------------------------------- */

  if (
    context === 'admin' &&
    adminToken
  ) {
    const displayName =
      sessionStorage.getItem(
        adminNameKey
      ) ||
      'Gerente SPBank';

    showAdminApp({
      displayName
    });

    try {
      await loadAdminAccounts();

      return;

    } catch (error) {
      if (
        error.status === 401
      ) {
        return;
      }

      showApiError(
        document.getElementById(
          'adminGlobalError'
        ),
        error
      );

      return;
    }
  }


  /* -------------------------------------------------------
     CLIENTE ATIVO
  ------------------------------------------------------- */

  if (clientToken) {
    try {
      const user =
        await api(
          '/api/v1/auth/me'
        );

      sessionStorage.setItem(
        activeContextKey,
        'client'
      );

      saldo = 0;
      extrato = [];

      showAuthenticatedApp(
        user
      );

      await Promise.all([
        loadCustomerAccounts(),
        loadAccountSummary(),
        refreshOfficialStatement()
      ]);

      loadPage(
        'dashboard'
      );

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


  /* -------------------------------------------------------
     ADMIN SEM CONTEXTO SALVO
  ------------------------------------------------------- */

  if (adminToken) {
    const displayName =
      sessionStorage.getItem(
        adminNameKey
      ) ||
      'Gerente SPBank';

    try {
      sessionStorage.setItem(
        activeContextKey,
        'admin'
      );

      showAdminApp({
        displayName
      });

      await loadAdminAccounts();

      return;

    } catch (_) {
      clearAdminSession();

      showPublicArea(
        'admin'
      );

      return;
    }
  }


  /* -------------------------------------------------------
     SEM SESSÃO
  ------------------------------------------------------- */

  showPublicArea(
    'login'
  );
}


/* =========================================================
   START
========================================================= */

initializeApp();