/* Card workflows: values, eligibility and calculations always come from the API. */
function cardBrandLogo(brand) {
  const files = { BRISA: 'brisa', MISTERSHARD: 'mistershard', ELU: 'elu' };
  return files[brand] ? `<img class="card-brand-logo" src="img/marcas/logo-${files[brand]}.png" alt="">` : '';
}
function cardFinanceOptions(rows, label, empty = 'Selecione') {
  return `<option value="">${escapeHtml(empty)}</option>` + rows.map(row => `<option value="${escapeHtml(row.id)}">${escapeHtml(label(row))}</option>`).join('');
}
function cardFinanceForm(action, title, fields, preview = false) {
  return `<details><summary>${escapeHtml(title)}</summary><form data-finance-action="${action}"><fieldset><legend>${escapeHtml(title)}</legend>
    ${fields}${preview ? '<button type="button" data-finance-preview class="btn-outline">Consultar prévia</button><pre data-finance-preview-result></pre>' : ''}
    <button type="submit" class="btn" ${preview ? 'disabled' : ''}>Confirmar</button></fieldset><div data-finance-message role="status"></div></form></details>`;
}
function cardFinancePassword() { return '<label>Senha de login<input type="password" name="confirmationPassword" autocomplete="current-password" maxlength="128" required></label>'; }
function cardFinanceBrand() { return '<label>Bandeira<select name="brand" required><option>BRISA</option><option>MISTERSHARD</option><option>ELU</option></select></label>'; }
function cardFinanceMoney(name, label) { return `<label>${escapeHtml(label)}<input type="number" name="${name}" step="0.01" min="0.01" required></label>`; }
function cardFinanceAccount() {
  return `<label>Conta para débito<select name="accountId"><option value="">Usar conta preferida</option>${customerAccounts.map(account => `<option value="${escapeHtml(account.accountId)}">${escapeHtml(accountTypeLabel(account.accountType))} · ${escapeHtml(account.accountNumber)}</option>`).join('')}</select></label>`;
}
function cardFinanceHistory(title, rows, describe) {
  return `<details><summary>${escapeHtml(title)} (${rows.length})</summary>${rows.length ? rows.map(row => `<p>${escapeHtml(describe(row))}</p>`).join('') : '<p>Nenhum registro.</p>'}</details>`;
}
async function renderCardFinance(container) {
  if (!container) return;
  try {
    const [state, credit] = await Promise.all([api('/api/v1/cards/finance'), api('/api/v1/cards/credit')]);
    if (!container.isConnected) return;
    const brand = cardFinanceBrand(), password = cardFinancePassword(), account = cardFinanceAccount();
    const purchaseSelect = `<label>Compra<select name="purchaseId" required>${cardFinanceOptions(state.purchases, row => `${row.merchant} · ${row.brand} · ${formatarMoeda(row.principal)}`)}</select></label>`;
    const proposalSelect = `<label>Proposta<select name="proposalId" required>${cardFinanceOptions(state.proposals.filter(row => row.status === 'PROPOSED'), row => `${row.brand} · entrada ${formatarMoeda(row.entry)} · válida em ${row.validOn}`)}</select></label>`;
    const sources = [...state.purchases.map(row => ({ id: `PURCHASE:${row.id}`, label: `Compra: ${row.merchant} · ${row.brand}` })), ...state.agreements.filter(row => row.status === 'ACTIVE').map(row => ({ id: `AGREEMENT:${row.id}`, label: `Acordo ${row.brand} · ${row.createdAt.slice(0,10)}` }))];
    container.innerHTML = `<h2>Compras, faturas e pagamentos</h2><p>Cada bandeira tem sua dívida. Instrumentos substitutos compartilham a mesma linha.</p>
      ${state.billingLines.map(line => `<p>${cardBrandLogo(line.brand)} ${escapeHtml(line.brand)} · Saldo credor: ${formatarMoeda(line.creditBalance)} · Dia ${line.dueDay}${line.pendingDueDay ? `; novo dia ${line.pendingDueDay} a partir de ${escapeHtml(line.effectiveReference)}` : ''}</p>`).join('')}
      ${cardFinanceForm('credit-request', 'Solicitar crédito ou aumento', cardFinanceMoney('requestedTotal', 'Novo total desejado') + '<label>Motivo<input name="reason" maxlength="240" required></label>')}
      ${credit.requests.map(request => `<article class="card"><p>Crédito solicitado: ${formatarMoeda(request.requestedTotal)} · ${escapeHtml(request.status)}${request.offeredTotal ? ` · Oferta: ${formatarMoeda(request.offeredTotal)}` : ''}</p>
        ${request.status === 'OFFERED' ? cardFinanceForm(`credit-accept:${request.id}`, 'Aceitar oferta de crédito', password) : ''}
        ${['REQUESTED','OFFERED'].includes(request.status) ? cardFinanceForm(`credit-cancel:${request.id}`, 'Cancelar solicitação de crédito', '') : ''}</article>`).join('')}
      ${cardFinanceForm('purchase', 'Simular e confirmar compra', '<label>Número do cartão<input type="password" name="pan" inputmode="numeric" pattern="[0-9]{16}" maxlength="16" autocomplete="off" required></label><label>Validade<input type="month" name="expiry" required></label><label>CVV<input type="password" name="cvv" pattern="[0-9]{3}" maxlength="3" autocomplete="off" required></label><label>Estabelecimento<input name="merchant" maxlength="240" required></label>' + cardFinanceMoney('amount','Valor de principal') + '<label>Quantidade de parcelas<input type="number" name="installments" min="1" max="12" value="1" required></label><label>PIN<input type="password" name="pin" inputmode="numeric" pattern="[0-9]{4}" maxlength="4" autocomplete="off" required></label>', true)}
      <h3>Faturas</h3>${state.invoices.map(invoice => `<article class="card"><p>${escapeHtml(invoice.brand)} · ${escapeHtml(invoice.reference)} · ${invoice.closed ? 'Fechada' : 'Aberta'} · Vencimento ${escapeHtml(invoice.effectiveDue)}</p>
        <button type="button" data-invoice-view="${escapeHtml(invoice.id)}" class="btn-outline">Detalhes e saldo</button>
        <button type="button" data-invoice-export="${escapeHtml(invoice.id)}" data-format="csv" class="btn-outline">CSV</button>
        <button type="button" data-invoice-export="${escapeHtml(invoice.id)}" data-format="pdf" class="btn-outline">PDF</button><div data-invoice-result role="status"></div></article>`).join('')}
      ${cardFinanceForm('due-day', 'Alterar vencimento dos ciclos futuros', brand + '<label>Novo dia<select name="dueDay"><option>5</option><option>10</option><option>15</option><option>20</option><option>25</option></select></label>' + password)}
      ${cardFinanceForm('preference', 'Salvar conta preferida (sem débito automático)', account + password)}
      ${cardFinanceForm('account-payment', 'Pagar fatura com saldo de conta', brand + cardFinanceMoney('amount', 'Valor a pagar') + account + password)}
      ${cardFinanceForm('boleto', 'Emitir boleto de fatura', '<p>A emissão não quita dívida. O gerente confirma o recebimento.</p>' + brand + cardFinanceMoney('amount','Valor do boleto') + password)}
      ${cardFinanceForm('refund', 'Solicitar estorno', purchaseSelect + cardFinanceMoney('principal','Principal a devolver') + '<label>Motivo<input name="reason" maxlength="240" required></label>' + password)}
      ${cardFinanceForm('anticipation', 'Antecipar parcelas inteiras', brand + `<label>Compra ou acordo<select name="source" required>${cardFinanceOptions(sources,row=>row.label)}</select></label><label>Quantidade de próximas parcelas<input type="number" name="count" min="1" max="12" required></label><input type="hidden" name="expectedAmount">` + account + password, true)}
      ${cardFinanceForm('proposal', 'Simular proposta de acordo ou renegociação', '<p>A quantidade inclui a entrada. O primeiro período curto conta como mês inteiro. O acordo só vigora após a entrada recebida.</p>' + brand + `<label>Faturas abrangidas<select name="invoiceIds" multiple>${state.invoices.map(row=>`<option value="${escapeHtml(row.id)}">${escapeHtml(row.brand)} · ${escapeHtml(row.reference)}</option>`).join('')}</select></label><label>Acordo anterior (opcional)<select name="previousAgreementId">${cardFinanceOptions(state.agreements.filter(row=>row.status==='ACTIVE'),row=>`${row.brand} · ${row.createdAt.slice(0,10)}`,'Nenhum')}</select></label><label>Quantidade incluindo entrada<input type="number" name="installments" min="2" max="12" required></label>` + password)}
      ${cardFinanceHistory('Propostas de acordo',state.proposals,row=>`${row.brand} · ${row.status} · dívida ${formatarMoeda(row.debt)} · juros ${formatarMoeda(row.interest)} · entrada ${formatarMoeda(row.entry)} · total ${formatarMoeda(row.total)} · válida em ${row.validOn}`)}
      ${cardFinanceForm('proposal-account','Pagar entrada de acordo com conta','<p>Entrada recebida depois da validade ou com dívida alterada é pagamento avulso; não ativa o acordo.</p>' + proposalSelect + account + password)}
      ${cardFinanceForm('proposal-boleto','Emitir boleto da entrada de acordo',proposalSelect + password)}
      ${cardFinanceForm('reconcile','Reconciliar saldo credor da bandeira','<p>Usa crédito que já pertence ao cliente para quitar obrigações fechadas. Não debita conta.</p>' + brand + password)}
      ${cardFinanceHistory('Compras',state.purchases,row=>`${row.merchant} · ${row.brand} · ${formatarMoeda(row.principal)} + juros ${formatarMoeda(row.interest)} · ${row.installments} parcelas`)}
      ${cardFinanceHistory('Tentativas',state.attempts,row=>`${row.merchant} · ${row.status} · ${row.reason}`)}
      ${cardFinanceHistory('Parcelas',state.installments,row=>`${row.sourceType} · ${row.brand} · parcela ${row.sequence} · vence ${row.effectiveDue} · principal pendente ${formatarMoeda(row.remainingPrincipal)} · juros pendentes ${formatarMoeda(row.remainingInterest)} · ${row.status}`)}
      ${cardFinanceHistory('Pagamentos e boletos',state.payments,row=>`${row.brand} · ${row.method} · ${row.status} · ${formatarMoeda(row.amount)} · excedente ${formatarMoeda(row.creditedAmount)}${row.code ? ` · código ${row.code}` : ''}`)}
      ${cardFinanceHistory('Solicitações de estorno',state.refundRequests,row=>`${row.brand} · ${formatarMoeda(row.principal)} · ${row.status} · ${row.reason}${row.decisionReason ? ` · decisão: ${row.decisionReason}` : ''}`)}
      ${cardFinanceHistory('Acordos',state.agreements,row=>`${row.brand} · ${row.status} · principal ${formatarMoeda(row.principal)} · juros ${formatarMoeda(row.interest)} · ${row.installments} parcelas`)}
      ${cardFinanceHistory('Encargos por atraso',state.lateEpisodes,row=>`${row.brand} · multa ${formatarMoeda(row.remainingFine)} · mora ${formatarMoeda(row.remainingMora)} · plano na origem ${row.plan}`)}
    `;
    container.querySelectorAll('[data-finance-action]').forEach(form => {
      form.addEventListener('submit', handleCardFinanceSubmit);
      form.querySelector('[data-finance-preview]')?.addEventListener('click', () => previewCardFinance(form));
      form.addEventListener('input', event => { if (form.querySelector('[data-finance-preview]') && !['pin','cvv','pan','confirmationPassword'].includes(event.target.name)) form.querySelector('button[type="submit"]').disabled = true; });
    });
    container.querySelectorAll('[data-invoice-view]').forEach(button => button.addEventListener('click', async () => {
      const output=button.parentElement.querySelector('[data-invoice-result]');
      try { const view=await api(`/api/v1/cards/invoices/${encodeURIComponent(button.dataset.invoiceView)}`); if(!output.isConnected)return;
        output.textContent=`${view.message} · Saldo: ${formatarMoeda(view.remaining)} · ${view.obligations.length} obrigações · ${view.allocations.length} alocações. ` + view.allocations.map(row=>`${row.kind}: principal ${formatarMoeda(row.principalPaid)}, juros ${formatarMoeda(row.interestPaid)}, multa ${formatarMoeda(row.finePaid)}, mora ${formatarMoeda(row.moraPaid)}, desconto ${formatarMoeda(row.discount)}`).join(' | ');
      } catch(error){showApiError(output,error);}
    }));
    container.querySelectorAll('[data-invoice-export]').forEach(button=>button.addEventListener('click',()=>downloadCardInvoice(button)));
  } catch(error){if(container.isConnected){container.innerHTML='<h2>Cartão</h2><div role="alert"></div>';showApiError(container.querySelector('[role="alert"]'),error);}}
}

function cardFinancePayload(form, preview = false) {
  const values=Object.fromEntries(new FormData(form));
  for(const name of ['installments','count','dueDay'])if(name in values)values[name]=Number(values[name]);
  for(const name of ['accountId','previousAgreementId'])if(values[name]==='')values[name]=null;
  let action=form.dataset.financeAction, path='', method='POST';
  if(action==='purchase')path='/purchases'+(preview?'/preview':'');
  else if(action==='credit-request')path='/credit/requests';
  else if(action.startsWith('credit-accept:'))path=`/credit/requests/${encodeURIComponent(action.split(':')[1])}/accept`;
  else if(action.startsWith('credit-cancel:'))path=`/credit/requests/${encodeURIComponent(action.split(':')[1])}/cancel`;
  else if(action==='due-day'){path='/due-day';method='PUT';}
  else if(action==='preference'){path='/invoices/payments/preference';method='PUT';}
  else if(action==='account-payment')path='/invoices/payments/account';
  else if(action==='boleto')path='/invoices/payments/boletos';
  else if(action==='refund'){path=`/purchases/${encodeURIComponent(values.purchaseId)}/refund-requests`;delete values.purchaseId;}
  else if(action==='anticipation'){
    const [type,id]=values.source.split(':');delete values.source;values.sourceId=id;
    path='/installments/anticipation'+(type==='AGREEMENT'?'/agreements':'')+(preview?'/preview':'/account');
    if(preview)for(const name of ['expectedAmount','accountId','confirmationPassword'])delete values[name];
  } else if(action==='proposal'){path='/agreements/proposals';values.invoiceIds=new FormData(form).getAll('invoiceIds');}
  else if(action==='proposal-account'||action==='proposal-boleto'){path=`/agreements/proposals/${encodeURIComponent(values.proposalId)}/${action==='proposal-account'?'account':'boletos'}`;delete values.proposalId;}
  else if(action==='reconcile'){path=`/credit/reconciliation/${encodeURIComponent(values.brand)}`;delete values.brand;}
  return { path:'/api/v1/cards'+path,method,values };
}
function clearCardFinanceSecrets(form){form.querySelectorAll('input[type="password"]').forEach(input=>{input.value='';});}
async function previewCardFinance(form){
  if(!form.reportValidity())return;
  const output=form.querySelector('[data-finance-preview-result]');const request=cardFinancePayload(form,true);
  try {const response=await api(request.path,{method:request.method,body:JSON.stringify(request.values)});if(!form.isConnected)return;
    if(form.dataset.financeAction==='purchase')output.textContent=`Principal ${formatarMoeda(response.principal)} · Juros ${formatarMoeda(response.interest)} · Total ${formatarMoeda(response.total)}. Parcelas: ${response.installments.map(part=>`${formatarMoeda(part.principal)} principal + ${formatarMoeda(part.interest)} juros`).join('; ')}`;
    else {output.textContent=`Obrigação ${formatarMoeda(response.originalAmount)} · Desconto ${formatarMoeda(response.discount)} · Débito ${formatarMoeda(response.amount)}`;form.elements.expectedAmount.value=response.amount;}
    form.querySelector('button[type="submit"]').disabled=false;
  } catch(error){if(output.isConnected)showApiError(output,error);}finally{clearCardFinanceSecrets(form);}
}
async function handleCardFinanceSubmit(event){
  event.preventDefault();const form=event.currentTarget, output=form.querySelector('[data-finance-message]'),fieldset=form.querySelector('fieldset');
  const request=cardFinancePayload(form), intent={...request.values};
  for(const name of ['confirmationPassword','pin','cvv'])delete intent[name];
  if(intent.pan){const digest=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(intent.pan));intent.pan=Array.from(new Uint8Array(digest),byte=>byte.toString(16).padStart(2,'0')).join('');}
  const signature=JSON.stringify({path:request.path,intent});if(form.pendingCardFinance?.signature!==signature)form.pendingCardFinance={signature,key:crypto.randomUUID()};
  const body=JSON.stringify(request.values);clearCardFinanceSecrets(form);fieldset.disabled=true;clearMessage(output);
  try {const result=await api(request.path,{method:request.method,headers:{'Idempotency-Key':form.pendingCardFinance.key},body});
    form.pendingCardFinance=null;if(!form.isConnected)return;
    output.textContent=`${result.status ?? 'Concluído'} · ${result.message ?? ''}${result.amount!=null?' · Valor '+formatarMoeda(result.amount):''}`;
    const refresh=document.createElement('button');refresh.type='button';refresh.className='btn-outline';refresh.textContent='Atualizar dados do cartão';refresh.onclick=()=>renderCardAllocationPage();output.append(refresh);
  }catch(error){if(output.isConnected)showApiError(output,error);}finally{fieldset.disabled=false;clearCardFinanceSecrets(form);}
}
async function downloadCardInvoice(button){
  const output=button.parentElement.querySelector('[data-invoice-result]');button.disabled=true;
  try {const response=await fetch(`${apiBaseUrl}/api/v1/cards/invoices/${encodeURIComponent(button.dataset.invoiceExport)}/export.${button.dataset.format}`,{headers:{Authorization:`Bearer ${sessionStorage.getItem(authTokenKey)}`}});
    if(!response.ok)throw new Error('Não foi possível exportar a fatura.');const blob=await response.blob();const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=`fatura.${button.dataset.format}`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
  }catch(error){showApiError(output,error);}finally{button.disabled=false;}
}

async function renderAdminCardFinance(customerId,container){
  try {const [credit,refunds,boletos]=await Promise.all([adminApi('/api/v1/admin/card-credit/requests'),adminApi(`/api/v1/admin/cards/customers/${encodeURIComponent(customerId)}/refund-requests`),adminApi(`/api/v1/admin/cards/customers/${encodeURIComponent(customerId)}/invoices/payments/boletos`)]);
    if(!container.isConnected)return;
    container.innerHTML=`<h4>Decisões de cartão</h4>${credit.filter(row=>row.customerId===customerId).map(row=>`<form data-admin-finance-path="/api/v1/admin/card-credit/customers/${customerId}/requests/${row.id}/decision"><p>Crédito: solicitado ${formatarMoeda(row.requestedTotal)} · ${escapeHtml(row.reason)}</p><label>Decisão<select name="decision"><option>OFFER</option><option>REJECT</option></select></label>${cardFinanceMoney('offeredTotal','Total ofertado')}<label>Motivo<input name="reason" maxlength="240" required></label><button type="submit">Decidir crédito</button><div role="status"></div></form>`).join('')}
      ${refunds.filter(row=>row.status==='REQUESTED').map(row=>`<form data-admin-finance-path="/api/v1/admin/cards/customers/${customerId}/refund-requests/${row.id}/decision"><p>Estorno: ${formatarMoeda(row.principal)} · ${escapeHtml(row.reason)}</p><label>Decisão<select name="approved"><option value="true">Aprovar</option><option value="false">Rejeitar</option></select></label><label>Motivo<input name="reason" maxlength="240" required></label><button type="submit">Decidir estorno</button><div role="status"></div></form>`).join('')}
      ${boletos.map(row=>`<form data-admin-finance-path="/api/v1/admin/cards/customers/${customerId}/invoices/payments/boletos/${row.id}/confirmation"><p>Recebimento de boleto ${escapeHtml(row.brand)} · ${formatarMoeda(row.amount)} · ${escapeHtml(row.purpose)} · ${escapeHtml(row.code)}</p><label>Motivo da confirmação<input name="reason" maxlength="240" required></label><button type="submit">Confirmar recebimento efetivo</button><div role="status"></div></form>`).join('')}`;
    container.querySelectorAll('form').forEach(form=>{
      const decision=form.elements.decision;if(decision)decision.addEventListener('change',()=>{form.elements.offeredTotal.required=decision.value==='OFFER';form.elements.offeredTotal.disabled=decision.value==='REJECT';});
      form.addEventListener('submit',async event=>{event.preventDefault();const values=Object.fromEntries(new FormData(form));if('approved'in values)values.approved=values.approved==='true';if(values.decision==='REJECT')delete values.offeredTotal;
        const signature=JSON.stringify(values);if(form.pendingDecision?.signature!==signature)form.pendingDecision={signature,key:crypto.randomUUID()};const button=form.querySelector('button');button.disabled=true;
        try{await adminApi(form.dataset.adminFinancePath,{method:'POST',headers:{'Idempotency-Key':form.pendingDecision.key},body:JSON.stringify(values)});if(form.isConnected)await renderAdminCardFinance(customerId,container);}
        catch(error){if(form.isConnected)showApiError(form.querySelector('[role="status"]'),error);}finally{button.disabled=false;}
      });
    });
  }catch(error){if(container.isConnected)showApiError(container,error);}
}
