/* Shared client workspace components. Financial values remain API-owned. */
const bankWorkspaceInfo = {
  dashboard:['VISÃO GERAL','Início','Resumo da conta selecionada e últimas movimentações.'],
  pix:['MOVIMENTAÇÕES','Pix','Envie, agende e acompanhe suas operações.'],
  transfer:['MOVIMENTAÇÕES','Transferências','Envie para outra conta e acompanhe seus agendamentos.'],
  payments:['PAGAMENTOS','Pagamentos','Boletos, contas e recargas em um só lugar.'],
  invest:['INVESTIMENTOS','Investimentos','Conheça os produtos, aplique e acompanhe seus valores brutos.'],
  cards:['CARTÕES','Cartões','Gerencie seus instrumentos, limites e faturas.'],
  statement:['MOVIMENTAÇÕES','Extrato','Consulte as movimentações oficiais da conta selecionada.'],
  profile:['MINHA CONTA','Meus dados','Mantenha seus contatos, endereço e acesso atualizados.']
};
const bankTabMemory = Object.create(null);
function bankWorkspaceHeading(key) {
  const [category,title,description]=bankWorkspaceInfo[key];
  return `<div class="workspace-heading"><div><span class="workspace-eyebrow">${category}</span><h2>${title}</h2><p>${description}</p></div><span class="workspace-badge">Ambiente de estudo</span></div>`;
}
function standardizeBankModule(page,key) {
  page.classList.add('bank-workspace');page.classList.remove('card');page.dataset.workspace=key;
  const oldHeading=page.querySelector(':scope > .pix-heading, :scope > .workspace-heading');
  if(oldHeading)oldHeading.remove();
  else {const title=page.querySelector(':scope > h2');if(title){const intro=title.nextElementSibling;if(intro?.tagName==='P')intro.remove();title.remove();}}
  page.insertAdjacentHTML('afterbegin',bankWorkspaceHeading(key));
  decorateBankMoneyInputs(page);
  return page;
}
function wrapBankContent(key) {
  const content=document.getElementById('content');
  const root=document.createElement('section');root.id=key+'Workspace';root.className='bank-workspace';root.dataset.workspace=key;
  root.append(...content.childNodes);content.append(root);
  const first=root.firstElementChild;
  if(first?.matches('.header,.card.highlight'))first.remove();
  root.insertAdjacentHTML('afterbegin',bankWorkspaceHeading(key));
  decorateBankMoneyInputs(root);
  return root;
}
function announceBankWorkspace(root,text) {
  let notice=root.querySelector(':scope > .bank-notice');
  if(!notice){notice=document.createElement('p');notice.className='bank-notice';notice.setAttribute('role','status');root.querySelector('.workspace-heading').after(notice);}
  notice.textContent=text;
}
function setBankTab(root,key) {
  bankTabMemory[root.dataset.workspace]=key;
  root.querySelectorAll(':scope > .bank-tabs [data-bank-tab]').forEach(button=>{const selected=button.dataset.bankTab===key;button.setAttribute('aria-selected',String(selected));button.tabIndex=selected?0:-1;});
  root.querySelectorAll(':scope > [data-bank-panel]').forEach(panel=>{
    panel.hidden=panel.dataset.bankPanel!==key;
    if(panel.hidden){panel.querySelectorAll('input[type=password]').forEach(input=>{input.value='';});panel.querySelectorAll('dialog[open]').forEach(dialog=>dialog.close());}
  });
  if(typeof hideRevealedCardData==='function')hideRevealedCardData();
}
function mountBankTabs(root,items,defaultTab=items[0].key) {
  const tabs=document.createElement('div');tabs.className='bank-tabs';tabs.setAttribute('role','tablist');tabs.setAttribute('aria-label','Áreas de '+bankWorkspaceInfo[root.dataset.workspace][1]);
  const prefix=root.dataset.workspace;
  tabs.innerHTML=items.map(item=>`<button type="button" role="tab" id="${prefix}-tab-${item.key}" data-bank-tab="${item.key}" aria-controls="${prefix}-panel-${item.key}"><span class="material-symbols-outlined" aria-hidden="true">${item.icon}</span><span>${item.label}</span></button>`).join('');
  root.querySelector('.workspace-heading').after(tabs);
  for(const item of items){const panel=document.createElement('section');panel.id=prefix+'-panel-'+item.key;panel.dataset.bankPanel=item.key;panel.setAttribute('role','tabpanel');panel.setAttribute('aria-labelledby',prefix+'-tab-'+item.key);panel.append(...item.nodes);root.append(panel);}
  const buttons=[...tabs.querySelectorAll('button')];
  buttons.forEach((button,index)=>{button.onclick=()=>setBankTab(root,button.dataset.bankTab);button.onkeydown=event=>{let next;if(event.key==='ArrowRight')next=(index+1)%buttons.length;if(event.key==='ArrowLeft')next=(index+buttons.length-1)%buttons.length;if(event.key==='Home')next=0;if(event.key==='End')next=buttons.length-1;if(next!==undefined){event.preventDefault();buttons[next].click();buttons[next].focus();}};});
  const remembered=bankTabMemory[prefix];setBankTab(root,items.some(item=>item.key===remembered)?remembered:defaultTab);
}
function bankField(control,text) {
  const label=document.createElement('label');label.append(document.createTextNode(text),control);return label;
}
function bankFieldRow(...nodes) {const row=document.createElement('div');row.className='bank-field-row';row.append(...nodes);return row;}
function bankPanel(title,description='') {const panel=document.createElement('section');panel.className='bank-panel';panel.innerHTML=`<h3>${escapeHtml(title)}</h3>${description?'<p class="bank-hint">'+escapeHtml(description)+'</p>':''}`;return panel;}
function prepareFinancialSteps(form) {
  const confirmation=form.querySelector('[data-confirmation]');if(!confirmation)return null;
  form.classList.add('bank-form');confirmation.classList.add('bank-review-panel');
  const title=form.querySelector(':scope > h3,:scope > h4');
  const heading=document.createElement('div');heading.className='bank-form-heading';
  const step=document.createElement('span');step.className='workspace-eyebrow';step.textContent='01 · DADOS DA OPERAÇÃO';heading.append(step);if(title)heading.append(title);form.prepend(heading);
  const originalTitle=title?.textContent;
  const message=form.querySelector(':scope > [role=status]');
  const entry=document.createElement('div');entry.className='bank-entry';
  for(const node of [...form.children])if(node!==heading&&node!==confirmation&&node!==message)entry.append(node);
  heading.after(entry);
  const editButton=document.createElement('button');editButton.type='button';editButton.className='btn-outline bank-edit';editButton.textContent='Voltar aos dados';confirmation.append(editButton);
  editButton.onclick=()=>{form.dispatchEvent(new Event('input',{bubbles:true}));entry.querySelector('input,select')?.focus();};
  const labels=form.id==='billForm'?['Revisar pagamento','Confirmar pagamento']:form.id==='rechargeForm'?['Revisar recarga','Confirmar recarga']:form.id==='investmentForm'?['Revisar aplicação','Confirmar aplicação']:['Revisar resgate','Confirmar resgate'];
  const preview=form.querySelector('[data-preview]');if(preview){preview.textContent=labels[0];preview.classList.remove('btn-outline');preview.classList.add('btn');}form.querySelector('[type=submit]').textContent=labels[1];
  decorateBankMoneyInputs(form);
  return {entry,editButton,editing(){entry.hidden=false;step.textContent='01 · DADOS DA OPERAÇÃO';if(title)title.textContent=originalTitle;},reviewing(){entry.hidden=true;step.textContent='02 · REVISÃO E CONFIRMAÇÃO';if(title)title.textContent='Confira os dados';confirmation.querySelector('input[type=password]')?.focus();}};
}
function organizeDashboardWorkspace() {
  const root=wrapBankContent('dashboard');root.querySelector('.grid-3')?.classList.add('bank-metrics');
  const actions=root.querySelector('.menu-grid');actions?.classList.add('bank-quick-actions');
  actions?.querySelectorAll('.menu-item').forEach(item=>{const button=document.createElement('button');button.type='button';button.className='menu-item';button.setAttribute('onclick',item.getAttribute('onclick'));button.append(...item.childNodes);item.replaceWith(button);});
}
function organizeTransferWorkspace() {
  const balance=document.getElementById('transferBalance');const root=wrapBankContent('transfer');
  const form=root.querySelector('#transferForm'),dialog=root.querySelector('#transferReview');form.className='bank-form';
  const controls=form.elements;
  const institution=controls.bankCode.closest('label'),date=controls.scheduledFor.closest('label');
  const destination=bankPanel('Dados da transferência');destination.className='bank-form-heading';destination.insertAdjacentHTML('afterbegin','<span class="workspace-eyebrow">01 · DESTINO E VALOR</span>');
  const recipient=bankFieldRow(bankField(controls.recipientName,'Nome do destinatário'),bankField(controls.recipientDocument,'CPF ou CNPJ'));
  const account=bankFieldRow(bankField(controls.branch,'Agência'),bankField(controls.accountNumber,'Conta'));account.classList.add('bank-account-row');
  const bank=bankFieldRow(institution,bankField(controls.accountType,'Tipo da conta'));
  const timing=document.createElement('div');timing.className='bank-timing';timing.innerHTML='<span>Quando enviar?</span><div class="bank-segment" role="group" aria-label="Quando enviar"><button type="button" data-timing="now" aria-pressed="true">Agora</button><button type="button" data-timing="scheduled" aria-pressed="false">Agendar</button></div>';
  const value=bankFieldRow(bankField(controls.amount,'Valor em reais'),timing);value.classList.add('bank-value-row');
  date.hidden=true;controls.scheduledFor.disabled=true;
  timing.querySelectorAll('button').forEach(button=>button.onclick=()=>{const scheduled=button.dataset.timing==='scheduled';date.hidden=!scheduled;controls.scheduledFor.disabled=!scheduled;controls.scheduledFor.required=scheduled;timing.querySelectorAll('button').forEach(item=>item.setAttribute('aria-pressed',String(item===button)));});
  const message=form.querySelector('#transferError'),submit=form.querySelector('[type=submit]');
  form.replaceChildren(destination,bank,recipient,account,value,date,message,submit);
  const aside=bankPanel('Sua conta de origem','O débito será feito na conta selecionada no cabeçalho.');aside.classList.add('bank-account-note');aside.insertAdjacentHTML('beforeend','<span class="bank-hint">Saldo disponível</span>');aside.append(balance);aside.insertAdjacentHTML('beforeend','<p class="bank-hint">Confira os dados do destinatário na revisão. O agendamento não reserva saldo.</p>');
  const layout=document.createElement('div');layout.className='bank-layout';layout.append(form,aside);
  const scheduled=root.querySelector('[aria-labelledby=scheduledTitle]');scheduled.classList.add('bank-panel');scheduled.classList.remove('card');
  mountBankTabs(root,[{key:'send',label:'Enviar',icon:'north_east',nodes:[layout,dialog]},{key:'scheduled',label:'Agendamentos',icon:'event',nodes:[scheduled]}]);
}
function organizeStatementWorkspace() {
  const root=wrapBankContent('statement'),filters=root.querySelector('.statement-toolbar');filters.classList.remove('card');filters.classList.add('bank-filter-tabs');filters.setAttribute('role','group');filters.setAttribute('aria-label','Filtrar movimentações');
  filters.querySelectorAll('button').forEach(button=>{button.setAttribute('aria-pressed',String(button.classList.contains('ativo')));button.addEventListener('click',()=>filters.querySelectorAll('button').forEach(item=>item.setAttribute('aria-pressed',String(item===button))));});
}
function organizeProfileWorkspace() {
  const root=wrapBankContent('profile'),form=root.querySelector('#profileForm'),security=root.querySelector('#passwordForm');
  form.classList.remove('card');form.classList.add('bank-form');security.classList.remove('card');security.classList.add('bank-form');
  const identity=form.querySelector('fieldset');identity.remove();identity.classList.add('bank-identity');
  const aside=bankPanel('Sua identificação','Estes dados identificam o titular e não podem ser alterados por aqui.');aside.append(identity);
  form.querySelector('h3').textContent='Contato e endereço';form.querySelector('.summary-context').textContent='Mantenha seus dados atualizados.';
  const layout=document.createElement('div');layout.className='bank-layout';layout.append(form,aside);
  form.querySelectorAll('fieldset').forEach(fieldset=>fieldset.classList.add('bank-form-section'));
  const securityLayout=document.createElement('div');securityLayout.className='bank-layout';securityLayout.append(security,bankPanel('Acesso à sua conta','Sua senha de login também confirma operações. Informe a senha atual para cadastrar uma nova.'));
  mountBankTabs(root,[{key:'profile',label:'Cadastro',icon:'person',nodes:[layout]},{key:'security',label:'Segurança',icon:'lock',nodes:[securityLayout]}]);
}
function organizeCardWorkspace() {
  const root=wrapBankContent('cards');
  const instruments=root.querySelector('#cardInstruments'),credit=root.querySelector('#creditAllocationPage'),finance=root.querySelector('#cardFinance');
  [instruments,credit,finance].forEach(section=>{section.classList.remove('card');section.classList.add('bank-module-panel');});
  mountBankTabs(root,[{key:'instruments',label:'Meus cartões',icon:'credit_card',nodes:[instruments]},{key:'credit',label:'Crédito',icon:'tune',nodes:[credit]},{key:'finance',label:'Faturas e operações',icon:'receipt_long',nodes:[finance]}]);
}
function decorateCreditAllocation(page,overview,allocation) {
  page.querySelector('h2').outerHTML='<h3>Distribuir limite entre bandeiras</h3>';
  const paragraphs=[...page.querySelectorAll(':scope > p')];paragraphs[1]?.remove();paragraphs[2]?.remove();paragraphs[0]?.classList.add('bank-hint');paragraphs[3]?.classList.add('bank-hint');
  const metrics=document.createElement('div');metrics.className='bank-metrics';metrics.innerHTML=[['Total aprovado',allocation.approvedTotal],['Distribuído',allocation.allocatedTotal],['A distribuir',allocation.unallocatedTotal]].map(([label,amount])=>'<div class="bank-panel bank-metric"><span>'+label+'</span><strong>'+formatarMoeda(amount)+'</strong></div>').join('');page.querySelector('form').before(metrics);
  const form=page.querySelector('form');form.classList.add('bank-form');
  const fieldset=form.querySelector('fieldset'),grid=document.createElement('div');grid.className='bank-brand-grid';
  for(const label of [...fieldset.querySelectorAll(':scope > label')]){
    if(!label.querySelector('input[type=number]'))continue;
    const description=label.nextElementSibling,tile=document.createElement('article');tile.className='bank-brand-tile';tile.append(label);if(description?.tagName==='P')tile.append(description);grid.append(tile);
  }
  fieldset.querySelector('legend').after(grid);
  const footer=document.createElement('div');footer.className='bank-form-footer';footer.append(fieldset.querySelector('input[type=password]').closest('label'),fieldset.querySelector('[type=submit]'));fieldset.append(footer);decorateBankMoneyInputs(form);
}
function decorateCardInstruments(container) {
  const heading=container.querySelector(':scope > h2');if(heading)heading.outerHTML='<h3>Meus cartões</h3>';
  const issue=container.querySelector(':scope > form');if(issue){const disclosure=document.createElement('details');disclosure.className='bank-action-disclosure';disclosure.open=!container.querySelector('[data-card-article]');disclosure.innerHTML='<summary>Emitir novo cartão</summary>';issue.before(disclosure);disclosure.append(issue);issue.classList.add('bank-form');issue.querySelector('fieldset').classList.add('bank-issue-fields');}
}
function decorateCardFinance(container) {
  const heading=container.querySelector(':scope > h2');if(heading)heading.outerHTML='<h3>Faturas e operações</h3>';
  const groups=new Map();
  for(const detail of [...container.querySelectorAll(':scope > details')]){
    const action=detail.querySelector('[data-finance-action]')?.dataset.financeAction;
    const group=!action?'Histórico':action.startsWith('credit-')?'Solicitações de crédito':['proposal','proposal-account','proposal-boleto','anticipation','reconcile'].includes(action)?'Acordos e antecipações':'Operações do cartão';
    if(!groups.has(group)){const section=bankPanel(group);section.classList.add('bank-finance-group');const grid=document.createElement('div');grid.className='bank-operation-grid';section.append(grid);groups.set(group,grid);container.append(section);}
    groups.get(group).append(detail);
  }
  container.querySelectorAll('form').forEach(form=>form.classList.add('bank-form'));decorateBankMoneyInputs(container);
}
function organizeInvestmentWorkspace(page,products) {
  const intro=page.querySelector(':scope > p');intro?.remove();standardizeBankModule(page,'invest');
  const catalog=page.querySelector('.investment-products'),form=page.querySelector('#investmentForm'),positions=page.querySelector('.bank-investment-positions'),history=page.querySelector('.bank-redemption-history');
  if(intro){intro.className='bank-note';catalog.before(intro);}
  catalog.querySelectorAll(':scope > article').forEach((card,index)=>{card.classList.add('bank-panel');const button=document.createElement('button');button.type='button';button.className='btn-outline';button.textContent='Aplicar neste produto';button.onclick=()=>{setBankTab(page,'apply');form.elements.productId.value=products[index].id;form.elements.productId.dispatchEvent(new Event('change'));form.elements.amount.focus();};card.append(button);});
  const grid=bankFieldRow(form.elements.productId.closest('label'),form.elements.amount.closest('label'));form.querySelector('#investmentPreview').before(grid);
  form.querySelector('#investmentPreview').dataset.preview='';
  const confirmation=document.createElement('div');confirmation.dataset.confirmation='';confirmation.className='bank-review-panel';confirmation.hidden=true;confirmation.append(form.querySelector('#investmentPreviewResult'),form.elements.confirmationPassword.closest('label'),form.querySelector('[type=submit]'));form.querySelector('#investmentMessage').before(confirmation);
  const step=prepareFinancialSteps(form);
  const guide=bankPanel('Antes de aplicar','A prévia informa valores e datas, sem reservar saldo.');
  guide.insertAdjacentHTML('beforeend','<div data-product-guide class="bank-hint">Selecione um produto para ver suas condições.</div><p class="bank-hint">Valores brutos, sem tributação ou suitability. Cota e CDI são simulados.</p>');
  form.elements.productId.addEventListener('change',()=>{const product=products.find(p=>p.id===form.elements.productId.value);guide.querySelector('[data-product-guide]').textContent=product?product.name+' · aplicação mínima '+formatarMoeda(product.minimumApplication):'Selecione um produto para ver suas condições.';});
  const applyLayout=document.createElement('div');applyLayout.className='bank-layout';applyLayout.append(form,guide);
  mountBankTabs(page,[{key:'overview',label:'Produtos',icon:'view_agenda',nodes:[...(intro?[intro]:[]),catalog]},{key:'apply',label:'Aplicar',icon:'add_chart',nodes:[applyLayout]},{key:'positions',label:'Minhas posições',icon:'monitoring',nodes:[positions]},{key:'redemptions',label:'Resgates',icon:'south_west',nodes:[history]}]);
  return step;
}
function bankReviewMarkup(values,extraClass='') {
  return '<dl class="bank-review '+extraClass+'">'+Object.entries(values).filter(([,value])=>value!==undefined&&value!==null&&value!=='').map(([label,value])=>'<div class="'+(/^(Valor|Total)/.test(label)?'is-value':'')+'"><dt>'+escapeHtml(label)+'</dt><dd>'+escapeHtml(String(value))+'</dd></div>').join('')+'</dl>';
}
function renderBankFinancialPreview(target,response,form) {
  let values;
  if(form.id==='billForm')values={Beneficiário:response.beneficiary,Documento:response.maskedBeneficiaryDocument,Cobrança:response.targetMask,Valor:formatarMoeda(response.amount),Vencimento:response.dueDate,Execução:response.scheduledFor,'Saldo estimado':response.estimatedBalanceAfter==null?null:formatarMoeda(response.estimatedBalanceAfter)};
  else if(form.id==='rechargeForm')values={Operadora:response.beneficiary,Celular:response.targetMask,Valor:formatarMoeda(response.amount),'Saldo estimado':formatarMoeda(response.estimatedBalanceAfter)};
  else if(form.hasAttribute('data-redemption-form'))values={'Valor bruto estimado':formatarMoeda(response.estimatedAmount),'Cotas resgatadas':response.units,'Cotas remanescentes':response.remainingUnits,Cotização:response.conversionDate,Crédito:response.paymentDate};
  if(values)target.innerHTML=bankReviewMarkup(values);
}

function organizeTransferReceiptWorkspace(){
 const root=wrapBankContent('transfer');root.querySelectorAll(':scope > section > h2').forEach(title=>{const next=document.createElement('h3');next.id=title.id;next.textContent=title.textContent;title.replaceWith(next);});
}
function showBankLoadError(page,key,error){
 page.replaceChildren();standardizeBankModule(page,key);const panel=bankPanel('Não foi possível carregar esta área');const message=document.createElement('p');message.setAttribute('role','alert');panel.append(message);page.append(panel);showApiError(message,error);
}

function decorateBankMoneyInputs(root){
 const names=new Set(['amount','principal','requestedTotal','brisa','mistershard','elu']);
 root.querySelectorAll('input[type=number]').forEach(input=>{if(!names.has(input.name)||input.closest('.bank-money-input,.pix-money-input'))return;
  const wrapper=document.createElement('span');wrapper.className='bank-money-input';wrapper.innerHTML='<span aria-hidden="true">R$</span>';input.before(wrapper);wrapper.append(input);
 });
}
