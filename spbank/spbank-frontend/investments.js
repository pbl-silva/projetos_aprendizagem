/* Investment amounts, conversion dates and quantities are supplied by the API. */
let investmentRetry = null;
async function renderInvestmentsPage() {
  const content=document.getElementById('content');
  content.innerHTML='<section id="investmentsPage" class="card" aria-live="polite">Carregando investimentos…</section>';
  const page=document.getElementById('investmentsPage');
  try {
    const [products,positions]=await Promise.all([api('/api/v1/investments/products'),api('/api/v1/investments/positions')]);
    if(!page.isConnected)return;
    const account=selectedCustomerAccount();
    page.innerHTML='<h2>Investimentos</h2><p>Produtos fictícios do laboratório. Valores brutos, sem cálculo de impostos ou avaliação do perfil do investidor. Não representam recomendação.</p>'+
      '<div class="investment-products">'+products.map(product=>'<article class="card"><h3>'+escapeHtml(product.name)+'</h3><p>Aplicação mínima: '+formatarMoeda(product.minimumApplication)+'</p>'+(product.type==='FUND'?'<p>Permanência mínima: '+formatarMoeda(product.minimumRemaining)+'</p>':'')+'<p>'+escapeHtml(product.type==='FUND'?'Fundo por cotas · sem cobertura do FGC':'CDB · elegível ao FGC como informação de produto; cobertura não calculada')+'</p><p>'+escapeHtml(product.type==='FUND'?'Cotização D+0 e pagamento de resgate D+1 útil':product.cdb.cdiPercent+'% do CDI simulado · '+product.cdb.termDays+' dias corridos · liquidez no vencimento')+'</p>'+(product.quote?'<p>Cota de referência: '+escapeHtml(product.quote.price)+' em '+escapeHtml(product.quote.date)+'</p>':'')+'</article>').join('')+'</div>'+
      '<form id="investmentForm"><h3>Aplicar pela conta selecionada</h3><p>'+escapeHtml(account?account.branch+' · '+account.accountNumber:'Selecione uma conta própria no cabeçalho')+'</p><label>Produto<select name="productId" required><option value="">Selecione</option>'+products.map(p=>'<option value="'+escapeHtml(p.id)+'">'+escapeHtml(p.name)+'</option>').join('')+'</select></label><label>Valor<input name="amount" type="number" min="0.01" step="0.01" required></label><button type="button" class="btn-outline" id="investmentPreview">Consultar prévia</button><div id="investmentPreviewResult"></div><label>Senha de login<input name="confirmationPassword" type="password" autocomplete="off" maxlength="128" required></label><button type="submit" class="btn" disabled>Confirmar aplicação</button><div id="investmentMessage" role="status"></div></form>'+
      '<h3>Minhas posições</h3><p>Valores brutos com data de referência, sem impostos. A valorização não altera o saldo da conta e não garante retorno futuro.</p><div>'+positions.map(p=>'<article class="card" data-investment-position><h4>'+escapeHtml(p.product.name)+'</h4><p>Principal remanescente: '+formatarMoeda(p.principal)+' · aplicação em '+escapeHtml(p.effectiveDate)+'</p>'+(p.units!==undefined?'<p>'+escapeHtml(p.units)+' cotas · cota de aquisição '+escapeHtml(p.unitPrice)+' em '+escapeHtml(p.quoteDate)+'</p>':'<p>Vencimento contratual: '+escapeHtml(p.maturityDate)+' · '+escapeHtml(p.product.cdb.cdiPercent)+'% do CDI simulado</p>')+renderInvestmentValuation(p)+'</article>').join('')+(positions.length?'':'<p>Nenhuma posição nesta titularidade.</p>')+'</div>';
    await attachInvestmentRedemptions(page,positions);
    const form=page.querySelector('#investmentForm');
    const intent=()=>({accountId:selectedCustomerAccount()?.id,productId:form.elements.productId.value,amount:form.elements.amount.value});
    let previewed=null;
    const invalidate=()=>{previewed=null;form.querySelector('[type=submit]').disabled=true;form.querySelector('#investmentPreviewResult').replaceChildren();};
    form.elements.productId.addEventListener('change',invalidate);form.elements.amount.addEventListener('input',invalidate);
    form.querySelector('#investmentPreview').addEventListener('click',async()=>{
      const message=form.querySelector('#investmentMessage');clearMessage(message);const data=intent();
      try{const result=await api('/api/v1/investments/preview',{method:'POST',body:JSON.stringify(data)});
        if(!page.isConnected)return;previewed=JSON.stringify(data);
        form.querySelector('#investmentPreviewResult').textContent='Valor: '+formatarMoeda(result.amount)+' · Data efetiva: '+result.effectiveDate+(result.estimatedUnits!==undefined?' · Cotas estimadas: '+result.estimatedUnits:'')+(result.estimatedMaturity?' · Vencimento estimado: '+result.estimatedMaturity:'')+'. '+result.notice;
        form.querySelector('[type=submit]').disabled=false;
      }catch(error){showApiError(message,error);}finally{form.elements.confirmationPassword.value='';}
    });
    form.addEventListener('submit',async event=>{
      event.preventDefault();const data=intent(),serialized=JSON.stringify(data),message=form.querySelector('#investmentMessage'),button=form.querySelector('[type=submit]');
      if(serialized!==previewed){invalidate();message.textContent='Consulte uma nova prévia para a conta e os valores atuais.';return;}
      if(!investmentRetry||investmentRetry.intent!==serialized)investmentRetry={intent:serialized,key:crypto.randomUUID()};
      const password=form.elements.confirmationPassword.value;form.elements.confirmationPassword.value='';button.disabled=true;
      try{const result=await api('/api/v1/investments',{method:'POST',headers:{'Idempotency-Key':investmentRetry.key},body:JSON.stringify({...data,confirmationPassword:password})});
        if(!page.isConnected)return;investmentRetry=null;
        const notice=result.status==='PENDING'?'Aplicação pendente para '+result.effectiveDate+'. Nenhum valor foi debitado.':'Aplicação concluída. Valor: '+formatarMoeda(result.amount)+'.';
        await loadAccountSummary();await renderInvestmentsPage();const next=document.getElementById('investmentMessage');if(next)next.textContent=notice;
      }catch(error){if(page.isConnected){showApiError(message,error);button.disabled=false;}}finally{form.elements.confirmationPassword.value='';}
    });
  }catch(error){if(page.isConnected){page.textContent='Não foi possível carregar os investimentos.';const errorBox=document.createElement('p');page.append(errorBox);showApiError(errorBox,error);}}
}

function renderInvestmentValuation(position) {
  if(position.valuationStatus!=='AVAILABLE')return '<p role="status">Valorização indisponível: faltam dados de referência válidos. O valor aplicado e as condições do lote estão preservados.</p>';
  const source=position.currentUnitPrice!==undefined?'Cota atual: '+escapeHtml(position.currentUnitPrice)+' em '+escapeHtml(position.currentQuoteDate):'CDI interno · base de 252 dias úteis · '+escapeHtml(position.accruedBusinessDays)+' dias de rendimento';
  return '<p><strong>Valor bruto: '+formatarMoeda(position.grossValue)+'</strong> · Rendimento bruto: '+formatarMoeda(position.grossEarnings)+'</p><p>Referência: '+escapeHtml(position.referenceDate)+' · calculado até '+escapeHtml(position.calculatedThrough)+'</p><p>'+source+'</p>';
}

async function attachInvestmentRedemptions(page,positions){
 const history=await api('/api/v1/investments/redemptions');if(!page.isConnected)return;
 const cards=page.querySelectorAll('[data-investment-position]');positions.forEach((position,index)=>{
  if(position.product.type!=='FUND')return;
  const pending=history.some(r=>r.positionId===position.id&&!['COMPLETED','FAILED'].includes(r.status));
  const form=document.createElement('form');form.dataset.financialForm='';form.dataset.redemptionForm='';
  form.innerHTML='<h4>Resgatar valor bruto</h4><p>O crédito será realizado na conta de origem em D+1 útil da conversão. Pode haver variação até a cotização.</p>'+(pending?'<p>Este lote já possui um resgate pendente.</p>':'<label>Valor em reais<input name="amount" type="number" min="0.01" step="0.01" required></label>'+financeReviewFields());cards[index].append(form);if(pending)return;
  bindFinancialForm(form,{intent:()=>({positionId:position.id,amount:form.elements.amount.value}),previewPath:'/api/v1/investments/redemptions/preview',confirmPath:'/api/v1/investments/redemptions',previewText:r=>'Resgate estimado: '+formatarMoeda(r.estimatedAmount)+' · cotas restantes: '+r.remainingUnits+' · conversão: '+r.conversionDate+' · crédito: '+r.paymentDate,onSuccess:async row=>{await renderInvestmentsPage();const message=document.getElementById('investmentMessage');if(message)message.textContent='Resgate registrado: '+(financialStatuses[row.status]||row.status)+'. Crédito previsto em '+row.paymentDate;}});
 });
 const section=document.createElement('section');section.innerHTML='<h3>Histórico de resgates e vencimentos</h3>'+(history.length?history.map(r=>'<details class="card"><summary>'+formatarMoeda(r.amount)+' · '+escapeHtml(financialStatuses[r.status]||r.status)+'</summary><p>Lote: '+escapeHtml(r.positionId)+'</p><p>Cotas: '+escapeHtml(r.units)+' · Conversão: '+escapeHtml(r.conversionDate)+' · Crédito: '+escapeHtml(r.paymentDate)+'</p><p>Identificador: '+escapeHtml(r.id)+'</p></details>').join(''):'<p>Nenhum resgate nesta titularidade.</p>');page.append(section);
}
