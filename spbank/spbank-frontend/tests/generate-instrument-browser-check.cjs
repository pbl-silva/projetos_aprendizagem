// Generates an isolated real-DOM browser check using the production functions and a simulated API.
const fs = require('node:fs');
const path = require('node:path');
const app = fs.readFileSync(path.join(__dirname, '..', 'app.js'), 'utf8');
const functions = app.slice(app.indexOf('function hideRevealedCardData()'), app.indexOf('function loadPage(page)'));
const events = app.slice(app.indexOf("document.addEventListener('visibilitychange'"), app.lastIndexOf('initializeApp();'));
const script = `
let cardRevealEpoch=0; let delayed=false; let resolveReveal; let refusePin=false; const calls=[];
const card={id:'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',brand:'BRISA',lastFour:'1234',state:'ACTIVE',allocated:2000,available:1800,pinBlocked:false,voluntaryBlocked:false,administrativeBlocked:false};
const details={holderName:'Cliente <seguro>',pan:'9901000000001234',cvv:'007',expiresOn:'2029-10-31'};
function formatarMoeda(v){return new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(v)}
function escapeHtml(v){const e=document.createElement('span');e.textContent=v;return e.innerHTML.replaceAll('"','&quot;')}
function clearMessage(e){e.textContent=''} function showApiError(e,v){e.textContent=v.message}
function cardBrandLogo(brand){return escapeHtml(brand)}
async function renderAdminCardFinance(){}
async function api(url,options={}){
  calls.push({url,...options}); if(url==='/api/v1/cards')return [card];
  if(url.endsWith('/details')){
    if(refusePin){const e=new Error('PIN bloqueado');e.code='CARD_PIN_BLOCKED';throw e}
    if(delayed)return await new Promise(resolve=>{resolveReveal=resolve}); return details;
  } return card;
}
async function adminApi(url,options={}){calls.push({url,...options});return options.method?card:[card]}
${functions}
${events}
const assert=(condition,message)=>{if(!condition)throw new Error(message)};
(async()=>{try{
  const container=document.getElementById('cardInstruments'); await renderCardInstruments(container);
  assert(!container.textContent.includes(details.pan),'summary hides full PAN');
  let form=container.querySelector('[data-path="details"]'); form.elements.pin.value='0012';
  await handleCardInstrumentAction({preventDefault(){},currentTarget:form});
  assert(container.querySelector('.card-sensitive').textContent.includes(details.pan),'reveals after confirmation');
  assert(form.elements.pin.value==='','PIN cleared');
  assert(!container.querySelector('.card-sensitive seguro'),'holder name inserted as text');
  window.dispatchEvent(new Event('pagehide'));
  assert(!container.textContent.includes(details.pan),'page exit clears PAN');
  form.elements.pin.value='0012'; delayed=true;
  const inFlight=handleCardInstrumentAction({preventDefault(){},currentTarget:form});
  assert(typeof resolveReveal==='function','request in flight'); hideRevealedCardData(); resolveReveal(details); await inFlight;
  assert(!container.textContent.includes(details.pan),'late response cannot undo hiding');
  delayed=false; refusePin=true; form.elements.pin.value='9999';
  await handleCardInstrumentAction({preventDefault(){},currentTarget:form});
  assert(container.querySelector('[role="alert"]').textContent.includes('bloqueado'),'refusal displayed after summary refresh');
  assert(!container.textContent.includes(details.pan),'refusal reveals no secrets');
  const admin=document.getElementById('adminCards'); await renderAdminCardInstruments('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',admin);
  const adminForm=admin.querySelector('form');
  assert(adminForm.querySelector('h4').textContent.includes('BRISA'),'administrator selects by brand and last four');
  assert(!adminForm.querySelector('input[name="cardId"]'),'no manual UUID');
  assert(localStorage.length===0&&sessionStorage.length===0,'no persistent browser credentials');
  assert(calls.filter(c=>c.url.endsWith('/details')).every(c=>c.method==='POST'&&!c.url.includes('0012')),'PIN only sent in POST body');
  document.getElementById('result').textContent='PASS: masked summary, protected reveal, PIN clearing, page-exit hiding, late-response guard, refusal refresh, administrator selection, no browser storage';
  document.body.dataset.result='passed';
}catch(e){document.getElementById('result').textContent='FAIL: '+e.stack;document.body.dataset.result='failed'}})();`;
const output = path.resolve(__dirname, '../../spbank-backend/target/instrument-browser-check.html');
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, '<!doctype html><meta charset="utf-8"><title>Instrument browser checks</title>'
  + '<section id="cardInstruments"></section><section id="adminCards"></section><pre id="result">RUNNING</pre><script>' + script + '</script>');
console.log(output);
