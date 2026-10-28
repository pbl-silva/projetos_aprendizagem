let activePixTab = 'send';
let pixCurrentReceipt = null;
let pixAccountContext = null;
const pixKeyLabels = {CPF_CNPJ:'CPF/CNPJ',EMAIL:'E-mail',PHONE:'Celular',RANDOM:'Aleatória'};

function pixKeyOptions(selected = 'CPF_CNPJ') {
  return Object.entries(pixKeyLabels).map(([value,label]) => `<option value="${value}" ${value===selected?'selected':''}>${label}</option>`).join('');
}
function pixEmpty(icon,title,text) {
  return `<div class="pix-empty"><span class="material-symbols-outlined" aria-hidden="true">${icon}</span><strong>${escapeHtml(title)}</strong><p>${escapeHtml(text)}</p></div>`;
}
function setPixTab(page,id) {
  activePixTab=id;
  page.querySelectorAll('[data-pix-tab]').forEach(button=>{
    const selected=button.dataset.pixTab===id;
    button.setAttribute('aria-selected',String(selected));button.tabIndex=selected?0:-1;
  });
  page.querySelectorAll('[data-pix-panel]').forEach(panel=>{
    panel.hidden=panel.dataset.pixPanel!==id;
    if(panel.hidden)panel.querySelectorAll('[type=password]').forEach(input=>{input.value='';});
  });
}

async function renderPixPage() {
  const content=document.getElementById('content');
  content.innerHTML='<section id="pixPage" class="pix-workspace" aria-busy="true">Carregando Pix…</section>';
  const page=document.getElementById('pixPage');
  const accountId=selectedCustomerAccount()?.id;
  if(pixAccountContext!==accountId){pixAccountContext=accountId;pixCurrentReceipt=null;activePixTab='send';}
  try {
    const [keys,scheduled,favorites,latestReceipt]=await Promise.all([
      api('/api/v1/pix/keys'),api('/api/v1/pix/scheduled'),api('/api/v1/pix/favorites'),
      pixCurrentReceipt?api('/api/v1/pix/'+encodeURIComponent(pixCurrentReceipt.id)).catch(()=>null):Promise.resolve(null)
    ]);
    if(!page.isConnected)return;
    pixCurrentReceipt=latestReceipt;
    const tabs=[['send','Enviar Pix','Enviar','north_east'],['keys','Minhas chaves','Chaves','key'],['scheduled','Agendamentos','Agenda','event'],['favorites','Favoritos','Favoritos','contacts'],['receipts','Comprovantes','Recibos','receipt_long']];
    page.innerHTML=`
      <div class="pix-heading"><div><span class="workspace-eyebrow">TRANSFERÊNCIAS</span><h2>Seu espaço Pix</h2><p>Envie, agende e acompanhe em um só lugar.</p></div><span class="pix-study-badge">Ambiente de estudo</span></div>
      <div class="pix-tabs bank-tabs" role="tablist" aria-label="Serviços Pix">
        ${tabs.map(([id,label,short,icon])=>`<button type="button" role="tab" id="pix-tab-${id}" data-pix-tab="${id}" aria-label="${label}" aria-controls="pix-panel-${id}"><span class="material-symbols-outlined" aria-hidden="true">${icon}</span><span class="pix-tab-full">${label}</span><span class="pix-tab-short" aria-hidden="true">${short}</span></button>`).join('')}
      </div>
      <section id="pix-panel-send" data-pix-panel="send" role="tabpanel" aria-labelledby="pix-tab-send">
        <div class="pix-layout">
          <form id="pixPaymentForm" class="pix-send-card" data-financial-form>
            <div class="pix-form-heading"><div><span class="workspace-eyebrow" id="pixStepLabel">01 · DESTINATÁRIO E VALOR</span><h3 id="pixFormTitle">Para quem vamos enviar?</h3></div><span class="pix-action-icon material-symbols-outlined" aria-hidden="true">north_east</span></div>
            <div class="pix-entry">
              ${favorites.length?`<label class="pix-favorite-select">Usar um favorito<select name="favorite"><option value="">Escolher contato</option>${favorites.map((f,i)=>`<option value="${i}">${escapeHtml(f.recipientName)} · ${escapeHtml(f.maskedKey||'')}</option>`).join('')}</select></label>`:'<select name="favorite" hidden aria-label="Favorito"><option value="">Informar chave</option></select>'}
              <div class="pix-field-row pix-recipient-row"><label>Tipo da chave<select name="keyType">${pixKeyOptions()}</select></label><label>Chave do destinatário<input name="key" maxlength="120" autocomplete="off" required placeholder="Chave Pix"></label></div>
              <div class="pix-field-row pix-value-row"><label class="pix-amount-label">Valor do Pix<span class="pix-money-input"><span aria-hidden="true">R$</span><input name="amount" type="number" min="0.01" step="0.01" required placeholder="0,00" aria-label="Valor do Pix em reais"></span></label><fieldset class="pix-timing"><legend>Quando?</legend><div class="pix-segment"><label><input type="radio" name="timing" value="now" checked><span>Agora</span></label><label><input type="radio" name="timing" value="scheduled"><span>Agendar</span></label></div></fieldset></div>
              <div class="pix-schedule-options" hidden><div class="pix-field-row"><label>Data do envio<input type="date" name="scheduledFor" disabled></label><label>Repetição<select name="frequency" disabled><option value="">Somente este envio</option><option value="WEEKLY">Toda semana</option><option value="MONTHLY">Todo mês</option></select></label></div><label class="pix-occurrences" hidden>Quantidade de envios<input type="number" name="occurrences" min="2" max="24" value="2" disabled></label><p class="pix-field-hint">O saldo será verificado na data de cada envio.</p></div>
              <label class="pix-check-line"><input type="checkbox" name="saveFavorite"><span>Salvar destinatário nos favoritos</span></label>
            </div>
            ${financeReviewFields()}
          </form>
          <aside class="pix-overview" aria-label="Seus atalhos Pix">
            <section class="pix-side-card"><div class="pix-side-heading"><h3>Seus contatos</h3><button type="button" class="pix-text-button" data-pix-go="favorites">Ver todos</button></div>
              ${favorites.length?favorites.slice(0,3).map((f,i)=>`<button type="button" class="pix-contact" data-pix-contact="${i}"><span class="pix-avatar" aria-hidden="true">${escapeHtml(f.recipientName.trim().split(/\s+/).slice(0,2).map(n=>n[0]).join(''))}</span><span><strong>${escapeHtml(f.recipientName)}</strong><small>${escapeHtml(f.maskedKey||'Contato favorito')}</small></span><span class="material-symbols-outlined" aria-hidden="true">chevron_right</span></button>`).join(''):pixEmpty('person_add','Seus contatos, por perto','Salve um destinatário ao enviar um Pix.')}
            </section>
            <section class="pix-side-card"><div class="pix-side-heading"><h3>Para receber</h3><span class="material-symbols-outlined" aria-hidden="true">south_west</span></div>
              ${keys.length?`<p class="pix-field-hint">Suas chaves cadastradas nesta conta.</p>${keys.slice(0,2).map(k=>`<div class="pix-key-peek"><span>${escapeHtml(pixKeyLabels[k.type]||k.type)}</span><strong title="${escapeHtml(k.value)}">${escapeHtml(k.value)}</strong></div>`).join('')}`:'<p class="pix-field-hint">Cadastre uma chave para receber por aqui.</p>'}
              <button type="button" class="pix-text-button" data-pix-go="keys">${keys.length?'Gerenciar minhas chaves':'Cadastrar uma chave'} <span aria-hidden="true">↗</span></button>
            </section>
            <button type="button" class="pix-agenda-shortcut" data-pix-go="scheduled"><span class="material-symbols-outlined" aria-hidden="true">event</span><span>Agendamentos</span><strong>${scheduled.length}</strong></button>
          </aside>
        </div>
      </section>
      <section id="pix-panel-keys" data-pix-panel="keys" role="tabpanel" aria-labelledby="pix-tab-keys" hidden><div class="pix-section-heading"><h3>Minhas chaves</h3><p>As chaves cadastradas na conta selecionada.</p></div><div id="pixKeys" class="pix-record-grid"></div><details class="pix-new-key"><summary><span class="material-symbols-outlined" aria-hidden="true">add</span>Adicionar uma chave</summary><form id="pixKeyForm" data-financial-form><div class="pix-field-row"><label>Tipo da chave<select name="type">${pixKeyOptions('RANDOM')}</select></label><label data-key-value hidden>Chave<input name="value" maxlength="120" autocomplete="off" disabled></label></div><label>Senha de login<input name="confirmationPassword" type="password" autocomplete="off" maxlength="128" required></label><button class="btn" type="submit">Cadastrar chave</button><p role="status"></p></form></details></section>
      <section id="pix-panel-scheduled" data-pix-panel="scheduled" role="tabpanel" aria-labelledby="pix-tab-scheduled" hidden><div class="pix-section-heading"><h3>Próximos envios</h3><p>Acompanhe e cancele os envios que ainda estão agendados.</p></div><div id="pixScheduled"></div></section>
      <section id="pix-panel-favorites" data-pix-panel="favorites" role="tabpanel" aria-labelledby="pix-tab-favorites" hidden><div class="pix-section-heading"><h3>Seus favoritos</h3><p>Escolha um contato para começar um envio.</p></div><div id="pixFavorites" class="pix-record-grid"></div></section>
      <section id="pix-panel-receipts" data-pix-panel="receipts" role="tabpanel" aria-labelledby="pix-tab-receipts" hidden><form id="pixLookup" class="pix-lookup"><div><h3>Consultar comprovante</h3><p>Use o identificador do Pix para localizar os detalhes.</p></div><div class="pix-lookup-row"><label>Identificador<input name="id" autocomplete="off" required placeholder="Identificador da operação"></label><button class="btn-outline" type="submit">Consultar</button></div><p role="status"></p></form><section id="pixReceipt" aria-live="polite">${pixCurrentReceipt?'<h3>Comprovante Pix</h3>'+financeReceipt(pixCurrentReceipt):pixEmpty('receipt_long','Seus comprovantes','Após um envio, o comprovante aparece aqui.')}</section></section>`;
    standardizeBankModule(page,'pix');
    page.removeAttribute('aria-busy');
    const form=page.querySelector('#pixPaymentForm');
    const entry=form.querySelector('.pix-entry');
    const previewButton=form.querySelector('[data-preview]');
    previewButton.className='btn pix-continue';previewButton.textContent='Revisar Pix';
    entry.append(previewButton);
    form.classList.add('bank-form');
    const confirmation=form.querySelector('[data-confirmation]');confirmation.classList.add('bank-review-panel');confirmation.querySelector('[type=submit]').textContent='Confirmar Pix';
    const edit=document.createElement('button');edit.type='button';edit.className='btn-outline pix-edit';edit.textContent='Voltar aos dados';confirmation.append(edit);
    edit.onclick=()=>{form.dispatchEvent(new Event('input',{bubbles:true}));form.elements.key.focus();};
    function updateSchedule() {
      const scheduledMode=form.elements.timing.value==='scheduled';
      form.querySelector('.pix-schedule-options').hidden=!scheduledMode;
      form.elements.scheduledFor.disabled=!scheduledMode;form.elements.scheduledFor.required=scheduledMode;
      form.elements.frequency.disabled=!scheduledMode;
      const repeat=scheduledMode&&!!form.elements.frequency.value;
      form.querySelector('.pix-occurrences').hidden=!repeat;form.elements.occurrences.disabled=!repeat;form.elements.occurrences.required=repeat;
    }
    form.querySelectorAll('[name=timing]').forEach(input=>input.addEventListener('change',updateSchedule));
    form.elements.frequency.addEventListener('change',updateSchedule);
    const selectContact=index=>{const favorite=favorites[index];if(!favorite)return;setPixTab(page,'send');form.elements.favorite.value=String(index);form.elements.keyType.value=favorite.keyType;form.elements.key.value=favorite.key;form.elements.key.dispatchEvent(new Event('input',{bubbles:true}));form.elements.amount.focus();};
    form.elements.favorite.onchange=()=>{if(form.elements.favorite.value!=='')selectContact(Number(form.elements.favorite.value));};
    bindFinancialForm(form,{
      intent:()=>({keyType:form.elements.keyType.value,key:form.elements.key.value,amount:form.elements.amount.value,scheduledFor:form.elements.timing.value==='scheduled'?(form.elements.scheduledFor.value||null):null,recurrence:form.elements.timing.value==='scheduled'&&form.elements.frequency.value?{frequency:form.elements.frequency.value,occurrences:Number(form.elements.occurrences.value)}:null,saveFavorite:form.elements.saveFavorite.checked}),
      previewPath:'/api/v1/pix/preview',confirmPath:'/api/v1/pix',previewText:r=>r.recipientName+' · '+formatarMoeda(r.amount),confirmationBody:(pix,password)=>({pix,confirmationPassword:password}),
      onInvalidate:()=>{entry.hidden=false;form.querySelector('#pixStepLabel').textContent='01 · DESTINATÁRIO E VALOR';form.querySelector('#pixFormTitle').textContent='Para quem vamos enviar?';},
      onReviewed:r=>{entry.hidden=true;form.querySelector('#pixStepLabel').textContent='02 · REVISÃO E CONFIRMAÇÃO';form.querySelector('#pixFormTitle').textContent='Confira seu Pix';form.querySelector('[data-review]').innerHTML=bankReviewMarkup({Para:r.recipientName,Documento:r.maskedRecipientDocument,Instituição:r.destinationBankName,Valor:formatarMoeda(r.amount),Envio:r.scheduledFor||'Agora'},'pix-review');form.elements.confirmationPassword.focus();},
      onSuccess:async row=>{pixCurrentReceipt=row;activePixTab='receipts';await renderPixPage();}
    });
    page.querySelector('#pixKeys').innerHTML=keys.length?keys.map(k=>`<article class="pix-record"><span class="material-symbols-outlined" aria-hidden="true">key</span><div><small>${escapeHtml(pixKeyLabels[k.type]||k.type)}</small><strong>${escapeHtml(k.value)}</strong></div><button type="button" class="pix-text-button" data-key-delete="${escapeHtml(k.id)}">Remover</button></article>`).join(''):pixEmpty('key','Nenhuma chave cadastrada','Adicione uma chave para receber Pix nesta conta.');
    const keyForm=page.querySelector('#pixKeyForm');
    keyForm.elements.type.onchange=()=>{const random=keyForm.elements.type.value==='RANDOM';keyForm.querySelector('[data-key-value]').hidden=random;keyForm.elements.value.disabled=random;keyForm.elements.value.required=!random;keyForm.elements.confirmationPassword.value='';};
    keyForm.onsubmit=async event=>{event.preventDefault();const password=keyForm.elements.confirmationPassword.value;keyForm.elements.confirmationPassword.value='';const button=keyForm.querySelector('[type=submit]');button.disabled=true;try{await api('/api/v1/pix/keys',{method:'POST',body:JSON.stringify({type:keyForm.elements.type.value,value:keyForm.elements.type.value==='RANDOM'?null:keyForm.elements.value.value,confirmationPassword:password})});if(page.isConnected)await renderPixPage();}catch(error){if(page.isConnected)showApiError(keyForm.querySelector('[role=status]'),error);button.disabled=false;}};
    page.querySelector('#pixScheduled').innerHTML=scheduled.length?scheduled.map(p=>`<details class="pix-scheduled-item"><summary><span class="material-symbols-outlined" aria-hidden="true">event</span><span><strong>${escapeHtml(p.recipientName)}</strong><small>${escapeHtml(p.scheduledFor)}</small></span><strong>${formatarMoeda(p.amount)}</strong></summary>${financeReceipt(p)}<div class="pix-record-actions"><button class="btn-outline" data-pix-cancel="${escapeHtml(p.id)}">Cancelar envio</button>${p.recurrenceId?`<button class="btn-outline" data-recurrence="${escapeHtml(p.recurrenceId)}">Cancelar série</button>`:''}</div></details>`).join(''):pixEmpty('event_available','Nenhum envio agendado','Ao enviar um Pix, escolha Agendar para definir a data.');
    page.querySelector('#pixFavorites').innerHTML=favorites.length?favorites.map((f,i)=>`<article class="pix-record"><span class="material-symbols-outlined" aria-hidden="true">person</span><button type="button" class="pix-contact-name" data-pix-contact="${i}"><strong>${escapeHtml(f.recipientName)}</strong><small>${escapeHtml(f.maskedKey||'')}</small></button><button type="button" class="pix-text-button" data-favorite-delete="${escapeHtml(f.id)}">Remover</button></article>`).join(''):pixEmpty('contacts','Sua agenda começa aqui','Marque Salvar destinatário ao realizar um envio.');
    page.querySelectorAll('[data-pix-contact]').forEach(button=>button.onclick=()=>selectContact(Number(button.dataset.pixContact)));
    page.querySelectorAll('[data-pix-go]').forEach(button=>button.onclick=()=>setPixTab(page,button.dataset.pixGo));
    const tabButtons=[...page.querySelectorAll('[data-pix-tab]')];
    tabButtons.forEach((button,index)=>{button.onclick=()=>setPixTab(page,button.dataset.pixTab);button.onkeydown=event=>{let next;if(event.key==='ArrowRight')next=(index+1)%tabButtons.length;if(event.key==='ArrowLeft')next=(index+tabButtons.length-1)%tabButtons.length;if(event.key==='Home')next=0;if(event.key==='End')next=tabButtons.length-1;if(next!==undefined){event.preventDefault();tabButtons[next].click();tabButtons[next].focus();}};});
    for(const [selector,suffix,attribute] of [['[data-key-delete]','keys/','keyDelete'],['[data-favorite-delete]','favorites/','favoriteDelete'],['[data-pix-cancel]','','pixCancel'],['[data-recurrence]','recurrences/','recurrence']]) {
      page.querySelectorAll(selector).forEach(button=>button.onclick=async()=>{button.disabled=true;try{await api('/api/v1/pix/'+suffix+button.dataset[attribute],{method:'DELETE'});if(page.isConnected)await renderPixPage();}catch(error){const message=document.createElement('p');button.after(message);showApiError(message,error);button.disabled=false;}});
    }
    page.querySelector('#pixLookup').onsubmit=async event=>{event.preventDefault();const lookup=event.currentTarget;try{const row=await api('/api/v1/pix/'+encodeURIComponent(lookup.elements.id.value));if(!page.isConnected)return;pixCurrentReceipt=row;page.querySelector('#pixReceipt').innerHTML='<h3>Comprovante Pix</h3>'+financeReceipt(row);}catch(error){if(page.isConnected)showApiError(lookup.querySelector('[role=status]'),error);}};
    setPixTab(page,activePixTab);
  } catch(error) {if(page.isConnected){page.removeAttribute('aria-busy');showBankLoadError(page,'pix',error);}}
}