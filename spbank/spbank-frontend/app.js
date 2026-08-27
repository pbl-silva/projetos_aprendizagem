let saldo = 0;
let filtroAtual = 'todos';
let extrato = [];

const apiBaseUrl = window.SPBANK_API_URL ?? 'http://localhost:8080';
const apiTimeoutMs = 10_000;
const authTokenKey = 'spbank.access-token';

let currentUser = null;
let pendingTransfer = null;
let pendingIdempotencyKey = null;

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

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  });
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

function setText(id, value) {
  const element = document.getElementById(id);

  if (element) {
    element.textContent = value ?? '—';
  }
}

function showApiError(element, error) {
  if (!element) return;

  const message =
    error?.message ||
    'Não foi possível concluir a operação.';

  element.textContent = error?.correlationId
    ? `${message} (código de atendimento: ${error.correlationId})`
    : message;
}

async function api(path, options = {}) {
  const controller = new AbortController();

  const timeoutId = setTimeout(
    () => controller.abort(),
    apiTimeoutMs
  );

  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,

      signal: controller.signal,

      headers: {
        'Content-Type': 'application/json',

        ...(sessionStorage.getItem(authTokenKey)
          ? {
              Authorization:
                `Bearer ${sessionStorage.getItem(authTokenKey)}`
            }
          : {}),

        ...(options.headers || {})
      }
    });

    const contentType =
      response.headers.get('content-type') ?? '';

    const isJson =
      contentType.includes('application/json') ||
      contentType.includes('application/problem+json');

    const body =
      response.status === 204 || !isJson
        ? null
        : await response.json();

    if (!response.ok) {

      if (
        response.status === 401 &&
        path !== '/api/v1/auth/login'
      ) {
        sessionStorage.removeItem(authTokenKey);

        currentUser = null;

        showLogin();
      }

      const error = new Error(
        body?.detail ||
        'Serviço temporariamente indisponível. Consulte a operação antes de reenviar.'
      );

      error.status = response.status;
      error.code = body?.errorCode;
      error.correlationId = body?.correlationId;

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

function showLogin(message = '') {

  document
    .getElementById('appShell')
    .classList
    .add('hidden');

  document
    .getElementById('loginScreen')
    .classList
    .remove('hidden');

  const errorBox =
    document.getElementById('loginError');

  if (errorBox) {
    errorBox.textContent = message;
  }
}

function showAuthenticatedApp(user) {

  currentUser = user;

  document
    .getElementById('currentUserName')
    .textContent = user.holderName;

  document
    .getElementById('loginScreen')
    .classList
    .add('hidden');

  document
    .getElementById('appShell')
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
    document.getElementById('loginError');

  const data =
    Object.fromEntries(
      new FormData(form)
    );

  button.disabled = true;
  button.textContent = 'Entrando…';

  errorBox.textContent = '';

  try {

    const session =
      await api(
        '/api/v1/auth/login',
        {
          method: 'POST',
          body: JSON.stringify(data)
        }
      );

    sessionStorage.setItem(
      authTokenKey,
      session.accessToken
    );

    saldo = 0;
    extrato = [];

    showAuthenticatedApp(session);

    await Promise.all([
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);

    loadPage('dashboard');

    form.reset();

  } catch (error) {

    showApiError(
      errorBox,
      error
    );

  } finally {

    button.disabled = false;
    button.textContent = 'Entrar';
  }
}

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
     * mesmo se o servidor estiver
     * indisponível.
     */

  } finally {

    sessionStorage.removeItem(
      authTokenKey
    );

    currentUser = null;

    pendingTransfer = null;
    pendingIdempotencyKey = null;

    saldo = 0;
    extrato = [];

    showLogin();
  }
}

async function loadAccountSummary() {

  const summary =
    await api(
      '/api/v1/accounts/me/summary'
    );

  saldo =
    Number(summary.balance);

  const element =
    document.getElementById(
      'transferBalance'
    );

  if (element) {

    element.textContent =
      formatarMoeda(saldo);
  }

  return summary;
}

async function loadBanks() {

  const banks =
    await api(
      '/api/v1/banks'
    );

  const select =
    document.querySelector(
      '[name="bankCode"]'
    );

  if (!select) return;

  const placeholder =
    new Option(
      'Selecione a instituição',
      ''
    );

  placeholder.disabled = true;
  placeholder.selected = true;

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

function renderDashboard() {

  const content =
    document.getElementById(
      'content'
    );

  const recentEntries =
    extrato
      .slice()
      .reverse()
      .slice(0, 5);


  content.innerHTML = `

    <div class="header">

      <h2>
        Dashboard
      </h2>

    </div>


    <div class="grid-3">

      <div class="card highlight">

        <p>
          Saldo total
        </p>

        <h2>
          ${formatarMoeda(saldo)}
        </h2>

      </div>


      <div class="card">

        <p>
          Cartão
        </p>

        <h2>
          Em adaptação
        </h2>

      </div>


      <div class="card">

        <p>
          Investimentos
        </p>

        <h2>
          Em adaptação
        </h2>

      </div>

    </div>


    <div class="menu-grid">


      <div
        class="menu-item"
        onclick="loadPage('pix')"
      >

        <div class="icon">

          <span
            class="material-symbols-outlined"
          >
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

          <span
            class="material-symbols-outlined"
          >
            sync_alt
          </span>

        </div>

        <span>
          Transferência
        </span>

      </div>


      <div
        class="menu-item"
        onclick="loadPage('invest')"
      >

        <div class="icon">

          <span
            class="material-symbols-outlined"
          >
            monitoring
          </span>

        </div>

        <span>
          Renda Fixa
        </span>

      </div>


      <div
        class="menu-item"
        onclick="loadPage('cartao')"
      >

        <div class="icon">

          <span
            class="material-symbols-outlined"
          >
            credit_card
          </span>

        </div>

        <span>
          Cartões
        </span>

      </div>

    </div>


    <div class="card">

      <h3>
        Resumo financeiro
      </h3>

      <div id="graficoDash"></div>

    </div>


    <div class="card">

      <div class="row">

        <h3>
          Transações recentes
        </h3>

        <button
          class="btn-outline btn-compact"
          type="button"
          onclick="loadPage('extrato')"
        >
          Ver mais
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
              .map(item => `

                <div class="row">

                  <span>
                    ${escapeHtml(item.tipo)}
                  </span>

                  <strong
                    style="
                      color:
                      ${
                        item.valor < 0
                          ? '#ef4444'
                          : '#22c55e'
                      }
                    "
                  >
                    ${formatarMoeda(item.valor)}
                  </strong>

                </div>

              `)
              .join('')
      }

    </div>

  `;


  renderGraficoDashboard();
}

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

        <dt>
          Destinatário
        </dt>

        <dd id="reviewRecipient"></dd>


        <dt>
          Instituição
        </dt>

        <dd id="reviewInstitution"></dd>


        <dt>
          Modalidade
        </dt>

        <dd id="reviewType"></dd>


        <dt>
          Conta
        </dt>

        <dd id="reviewAccount"></dd>


        <dt>
          Valor
        </dt>

        <dd id="reviewAmount"></dd>


        <dt>
          Tarifa
        </dt>

        <dd id="reviewFee"></dd>


        <dt>
          Total
        </dt>

        <dd id="reviewTotal"></dd>


        <dt>
          Data efetiva
        </dt>

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
    .catch(error =>

      showApiError(
        document.getElementById(
          'transferError'
        ),
        error
      )
    );
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

    </div>


    <div class="card">

      <button
        id="btnTodos"
        class="filtro-btn ativo"
        onclick="filtrarExtrato('todos', this)"
      >
        Todos
      </button>


      <button
        id="btnTransfer"
        class="filtro-btn"
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

      <div id="graficoExtrato"></div>

      <div
        id="resumoValores"
        class="resumo-valores"
      ></div>

    </div>


    <div id="listaExtrato"></div>

  `;


  renderExtrato();

  renderGraficoExtrato();

  atualizarFiltroAtivo();


  refreshOfficialStatement()

    .then(() => {

      if (
        document.getElementById(
          'listaExtrato'
        )
      ) {

        renderExtrato();

        renderGraficoExtrato();
      }
    })

    .catch(error => {

      const lista =
        document.getElementById(
          'listaExtrato'
        );

      if (lista) {

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
    });
}

function loadPage(page) {

  if (page === 'dashboard') {

    renderDashboard();

    return;
  }


  if (page === 'transfer') {

    renderTransferPage();

    return;
  }


  if (page === 'extrato') {

    renderExtratoPage();

    return;
  }


  if (page === 'pix') {

    renderFeaturePlaceholder(

      'Pix',

      'O módulo Pix do SPBank está em adaptação para integração com o backend oficial.'
    );

    return;
  }


  if (page === 'invest') {

    renderFeaturePlaceholder(

      'Investimentos',

      'O módulo de investimentos do SPBank está em adaptação para integração com o backend oficial.'
    );

    return;
  }


  if (page === 'cartao') {

    renderFeaturePlaceholder(

      'Cartão',

      'O módulo de cartão do SPBank está em adaptação para integração com o backend oficial.'
    );

    return;
  }


  renderDashboard();
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
      data
        .recipientDocument
        .replace(/\D/g, ''),

    amount:
      Number(data.amount),

    scheduledFor:
      data.scheduledFor || null
  };


  button.disabled = true;

  button.textContent =
    'Calculando…';

  errorBox.textContent = '';


  try {

    const preview =
      await api(

        '/api/v1/transfers/preview',

        {
          method: 'POST',

          body:
            JSON.stringify(payload)
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
      ] || preview.type
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


    document
      .getElementById(
        'reviewError'
      )
      .textContent = '';


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

    button.disabled = false;

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


  const errorBox =
    document.getElementById(
      'transferError'
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

    reviewError.textContent =
      'Informe sua senha para confirmar a transferência.';

    passwordInput.focus();

    return;
  }


  button.disabled = true;

  button.textContent =
    'Processando…';

  reviewError.textContent = '';


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
      loadScheduledTransfers()
    ]);


  } catch (error) {

    passwordInput.value = '';


    showApiError(
      reviewError,
      error
    );


    if (errorBox) {

      errorBox.textContent = '';
    }


    passwordInput.focus();


  } finally {

    button.disabled = false;

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


  if (transfers.length === 0) {

    container.innerHTML =
      '<p>Nenhuma transferência agendada.</p>';

    return;
  }


  container.innerHTML = `

    <div class="table-responsive">

      <table>

        <thead>

          <tr>

            <th>
              Data
            </th>

            <th>
              Destinatário
            </th>

            <th>
              Modalidade
            </th>

            <th>
              Valor
            </th>

            <th>
              Ação
            </th>

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
                          transfer.scheduledFor
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
                          ] || transfer.type
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
    .forEach(button => {

      button.addEventListener(

        'click',

        () =>
          cancelScheduledTransfer(
            button
          )
      );
    });
}

async function cancelScheduledTransfer(
  button
) {

  const id =
    button.dataset.cancelScheduled;


  const message =
    document.getElementById(
      'scheduledMessage'
    );


  button.disabled = true;


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


    button.disabled = false;
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


        <dt>
          Identificador
        </dt>

        <dd id="receiptId"></dd>


        <dt>
          Destinatário
        </dt>

        <dd id="receiptRecipient"></dd>


        <dt>
          Conta
        </dt>

        <dd id="receiptAccount"></dd>


        <dt>
          Modalidade
        </dt>

        <dd id="receiptType"></dd>


        <dt>
          Valor
        </dt>

        <dd id="receiptAmount"></dd>


        <dt>
          Tarifa
        </dt>

        <dd id="receiptFee"></dd>


        <dt>
          Data agendada
        </dt>

        <dd id="receiptDate"></dd>


        <dt>
          Solicitada em
        </dt>

        <dd id="receiptRequestedAt"></dd>


        <dt>
          Processada em
        </dt>

        <dd id="receiptProcessedAt"></dd>


        ${
          transfer.type === 'TED'

            ? `
              <dt>
                Liquidação
              </dt>

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

              <th>
                Data/hora
              </th>

              <th>
                Descrição
              </th>

              <th>
                Para/de
              </th>

              <th>
                Modalidade
              </th>

              <th>
                Movimento
              </th>

              <th>
                Valor
              </th>

              <th>
                Saldo após
              </th>

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
    ] || transfer.status
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
    ] || transfer.type
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

    transfer.scheduledFor ||
    'Imediata'
  );


  setText(

    'receiptRequestedAt',

    new Date(
      transfer.requestedAt
    )
      .toLocaleString(
        'pt-BR'
      )
  );


  setText(

    'receiptProcessedAt',

    transfer.processedAt

      ? new Date(
          transfer.processedAt
        )
          .toLocaleString(
            'pt-BR'
          )

      : 'Aguardando'
  );


  if (transfer.type === 'TED') {

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


  for (const entry of entries) {

    const row =
      document.createElement(
        'tr'
      );


    const values = [

      new Date(
        entry.occurredAt
      )
        .toLocaleString(
          'pt-BR'
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


    for (const value of values) {

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
    .forEach(btn => {

      btn.classList.remove(
        'ativo'
      );
    });


  if (botao) {

    botao.classList.add(
      'ativo'
    );
  }


  renderExtrato();

  renderGraficoExtrato();
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
    .forEach(btn => {

      btn.classList.remove(
        'ativo'
      );
    });


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

    .filter(item => {

      if (
        filtroAtual === 'todos'
      ) {
        return true;
      }


      if (
        filtroAtual === 'transfer'
      ) {

        return (
          item.category ===
          'transfer'
        );
      }


      return false;
    });
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
      .map(item => {

        const cor =
          item.valor < 0
            ? '#ef4444'
            : '#22c55e';


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
                ${
                  escapeHtml(
                    item.tipo
                  )
                }
              </strong>


              <span
                style="
                  color:${cor}
                "
              >
                ${
                  formatarMoeda(
                    item.valor
                  )
                }
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

                      new Date(
                        item.data
                      )
                        .toLocaleString(
                          'pt-BR'
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
      })
      .join('');
}

function renderGraficoDashboard() {

  const container =
    document.getElementById(
      'graficoDash'
    );


  if (!container) {
    return;
  }


  if (
    window.graficoDashboard &&
    typeof window.graficoDashboard.destroy ===
      'function'
  ) {

    window
      .graficoDashboard
      .destroy();
  }


  const movements =
    extrato.slice(-9);


  if (
    movements.length === 0
  ) {

    container.innerHTML = `

      <div
        style="
          height:280px;
          display:flex;
          align-items:center;
          justify-content:center;
          color:#94a3b8;
        "
      >
        Nenhuma movimentação disponível
      </div>

    `;

    return;
  }


  const entradas =
    movements.map(
      item =>
        item.valor >= 0
          ? item.valor
          : 0
    );


  const saidas =
    movements.map(
      item =>
        item.valor < 0
          ? Math.abs(
              item.valor
            )
          : 0
    );


  const categories =
    movements.map(item =>

      new Date(
        item.data
      )
        .toLocaleDateString(
          'pt-BR',
          {
            day: '2-digit',
            month: '2-digit'
          }
        )
    );


  window.graficoDashboard =
    new ApexCharts(

      container,

      {

        chart: {

          type:
            'area',

          height:
            350,

          toolbar: {
            show: false
          },

          background:
            'transparent'
        },


        series: [

          {
            name:
              'Entradas',

            data:
              entradas
          },

          {
            name:
              'Saídas',

            data:
              saidas
          }
        ],


        colors: [
          '#22c55e',
          '#E30E13'
        ],


        stroke: {

          curve:
            'smooth',

          width:
            3
        },


        fill: {

          opacity: [
            0.15,
            0.05
          ]
        },


        markers: {

          size:
            5,

          strokeWidth:
            2
        },


        xaxis: {

          categories,

          labels: {

            style: {
              colors:
                '#94a3b8'
            }
          }
        },


        yaxis: {

          labels: {

            style: {

              colors:
                '#94a3b8'
            },

            formatter:
              value =>
                formatarMoeda(
                  value
                )
          }
        },


        grid: {

          borderColor:
            'rgba(255,255,255,.08)'
        },


        legend: {

          labels: {

            colors:
              '#ffffff'
          }
        },


        tooltip: {

          theme:
            'dark'
        }
      }
    );


  window
    .graficoDashboard
    .render();
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
        ${
          formatarMoeda(
            entradas
          )
        }
      </strong>

    </div>


    <div class="resumo-item">

      <h4>
        Saídas no filtro
      </h4>

      <strong class="saida">
        ${
          formatarMoeda(
            saidas
          )
        }
      </strong>

    </div>


    <div class="resumo-item">

      <h4>
        Resultado no filtro
      </h4>

      <strong
        class="${resultadoClasse}"
      >
        ${
          formatarMoeda(
            resultado
          )
        }
      </strong>

    </div>


    <div class="resumo-item">

      <h4>
        Saldo atual da conta
      </h4>

      <strong>
        ${
          formatarMoeda(
            saldo
          )
        }
      </strong>

    </div>

  `;
}

function renderGraficoExtrato() {

  const container =
    document.getElementById(
      'graficoExtrato'
    );


  if (!container) {
    return;
  }


  const contexto =
    document.getElementById(
      'resumoContexto'
    );


  const nomesDosFiltros = {

    todos:
      'Todas as movimentações carregadas',

    transfer:
      'Somente transferências'
  };


  if (contexto) {

    contexto.textContent =
      `Filtro: ${
        nomesDosFiltros[
          filtroAtual
        ]
      }`;
  }


  if (
    window.graficoExtrato &&
    typeof window.graficoExtrato.destroy ===
      'function'
  ) {

    window
      .graficoExtrato
      .destroy();
  }


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


  if (
    entradas === 0 &&
    saidas === 0
  ) {

    container.innerHTML = `

      <div
        style="
          height:350px;
          display:flex;
          align-items:center;
          justify-content:center;
          color:#94a3b8;
        "
      >
        Nenhuma movimentação
        para o filtro selecionado
      </div>

    `;


    renderResumoValores(
      0,
      0
    );


    return;
  }


  container.innerHTML = '';


  window.graficoExtrato =
    new ApexCharts(

      container,

      {

        chart: {

          type:
            'donut',

          height:
            350
        },


        series: [
          entradas,
          saidas
        ],


        labels: [
          'Entradas',
          'Saídas'
        ],


        colors: [
          '#22c55e',
          '#ef4444'
        ],


        legend: {

          position:
            'bottom',

          labels: {

            colors:
              '#fff'
          }
        },


        dataLabels: {

          enabled:
            true
        },


        plotOptions: {

          pie: {

            donut: {

              size:
                '70%',

              labels: {

                show:
                  true,

                name: {

                  show:
                    true
                },

                value: {

                  show:
                    true,

                  formatter:
                    value =>
                      formatarMoeda(
                        value
                      )
                },

                total: {

                  show:
                    true,

                  label:
                    'Total movimentado',

                  formatter:
                    () =>
                      formatarMoeda(
                        entradas +
                        saidas
                      )
                }
              }
            }
          }
        },


        tooltip: {

          theme:
            'dark',

          custom:
            function ({
              series,
              seriesIndex,
              w
            }) {

              const valor =
                series[
                  seriesIndex
                ];


              const total =
                series.reduce(
                  (a, b) =>
                    a + b,
                  0
                );


              const percentual =

                total === 0

                  ? '0.0'

                  : (
                      valor /
                      total *
                      100
                    )
                      .toFixed(1);


              const label =
                w
                  .globals
                  .labels[
                    seriesIndex
                  ];


              return `

                <div
                  style="
                    padding:12px;
                    background:#111111;
                    color:#fff;
                    border-radius:10px;
                  "
                >

                  <strong>
                    ${label}
                  </strong>

                  <br>

                  ${
                    formatarMoeda(
                      valor
                    )
                  }

                  <br>

                  ${percentual}%

                </div>

              `;
            }
        }
      }
    );


  window
    .graficoExtrato
    .render();


  renderResumoValores(
    entradas,
    saidas
  );
}

async function initializeApp() {

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
      'logoutButton'
    )
    .addEventListener(

      'click',

      performLogout
    );


  const token =
    sessionStorage.getItem(
      authTokenKey
    );


  if (!token) {

    showLogin();

    return;
  }


  try {

    const user =
      await api(
        '/api/v1/auth/me'
      );


    saldo = 0;

    extrato = [];


    showAuthenticatedApp(
      user
    );


    await Promise.all([
      loadAccountSummary(),
      refreshOfficialStatement()
    ]);


    loadPage(
      'dashboard'
    );


  } catch (error) {

    sessionStorage.removeItem(
      authTokenKey
    );


    showLogin(

      error.status === 401

        ? 'Sua sessão expirou. Entre novamente.'

        : 'Não foi possível conectar ao SPBank. Verifique o backend.'
    );
  }
}

initializeApp();