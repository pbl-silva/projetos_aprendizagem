const fs=require('fs'),path=require('path'),assert=require('assert/strict');const target=path.resolve(__dirname,'../target'),base='http://127.0.0.1:28082';let checks=0;
const credentials=name=>JSON.parse(fs.readFileSync(path.join(target,'study-'+name+'.json'),'utf8'));
async function call(route,token,body,status=200,key=crypto.randomUUID()){const r=await fetch(base+route,{method:body?'POST':'GET',headers:{'Content-Type':'application/json','Idempotency-Key':key,...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});const value=await r.json();assert.equal(r.status,status,route+' '+(value.errorCode||''));checks++;return value;}
const sessionFile=path.join(target,'inv4-session.json');
(async()=>{const phase=process.argv[2];
 if(phase==='create'){
  assert.equal((await call('/actuator/health')).status,'UP');await call('/api/v1/investments/positions',null,null,401);
  const client=await call('/api/v1/auth/login',null,credentials('lucas'));const before=await call('/api/v1/accounts/me/summary',client.accessToken);const password=credentials('lucas').password;
  const fund=await call('/api/v1/investments',client.accessToken,{accountId:client.accountId,productId:'d0000000-0000-0000-0000-000000000001',amount:100,confirmationPassword:password},201);
  const cdb=await call('/api/v1/investments',client.accessToken,{accountId:client.accountId,productId:'d0000000-0000-0000-0000-000000000002',amount:500,confirmationPassword:password},201);
  const positions=await call('/api/v1/investments/positions',client.accessToken);assert.ok(positions.every(p=>p.valuationStatus==='AVAILABLE'&&p.grossEarnings===0));
  fs.writeFileSync(sessionFile,JSON.stringify({token:client.accessToken,accountId:client.accountId,balance:before.balance-600,fundId:fund.positionId,cdbId:cdb.positionId,checks}));console.log('PASS: aplicações no relógio de 23/10; avaliações iniciais sem juros.');return;
 }
 const state=JSON.parse(fs.readFileSync(sessionFile,'utf8'));const client=await call('/api/v1/auth/login',null,credentials('lucas'));state.token=client.accessToken;
 const positions=await call('/api/v1/investments/positions',client.accessToken);const fund=positions.find(p=>p.id===state.fundId),cdb=positions.find(p=>p.id===state.cdbId);
 assert.equal(fund.grossValue,107.50);assert.equal(fund.grossEarnings,7.50);assert.equal(fund.unitPrice,10);assert.equal(fund.currentUnitPrice,10.75);assert.equal(fund.currentQuoteDate,'2026-10-27');assert.equal(fund.referenceDate,'2026-10-27');
 if(phase==='missing'){assert.equal(cdb.valuationStatus,'DATA_UNAVAILABLE');assert.ok(!('grossValue'in cdb)&&!('grossEarnings'in cdb));assert.equal(cdb.valuationErrorCode,'INVESTMENT_CDI_MISSING');}
 else {assert.equal(cdb.valuationStatus,'AVAILABLE');assert.equal(cdb.grossValue,phase==='advance'?500.50:500.58);assert.equal(cdb.grossEarnings,phase==='advance'?0.50:0.58);assert.equal(cdb.calculatedThrough,'2026-10-27');assert.equal(cdb.accruedBusinessDays,2);assert.ok(!('units'in cdb)&&!('currentUnitPrice'in cdb));assert.equal(cdb.product.cdb.cdiPercent,110);if(phase!=='advance')assert.deepEqual(cdb.cdiSources.map(p=>p.version),['STUDY-CDI-2026-B','STUDY-CDI-2026-B2']);}
 assert.equal((await call('/api/v1/accounts/me/summary',client.accessToken)).balance,state.balance);
 const entries=await call('/api/v1/accounts/me/entries?limit=100',client.accessToken);assert.equal(entries.filter(e=>e.type==='INVESTMENT_APPLICATION').length,2);
 const other=await call('/api/v1/auth/login',null,credentials('marina'));assert.equal((await call('/api/v1/investments/positions',other.accessToken)).length,0);
 if(phase==='restored'){const repeated=await Promise.all([call('/api/v1/investments/positions',client.accessToken),call('/api/v1/investments/positions',client.accessToken)]);assert.deepEqual(repeated[0],repeated[1]);}
 state.checks+=checks;fs.writeFileSync(sessionFile,JSON.stringify(state));fs.writeFileSync(path.join(target,'inv4-http-result.json'),JSON.stringify({status:'passed',lastPhase:phase,checks:state.checks,verified:['dated fund quote and original acquisition price','110% simulated daily CDI in business days','versioned sources and gaps','read-only balances and bank entries','ownership isolation','repeatable concurrent reads']},null,2));
 console.log('PASS: INV-4 '+phase+'; valorização conciliada sem movimentação de conta.');
})().catch(error=>{console.error(error.message);process.exit(1)});
