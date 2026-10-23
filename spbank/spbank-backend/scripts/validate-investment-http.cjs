const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const base='http://127.0.0.1:28081',target=path.resolve(__dirname,'../target');let checks=0;
const credentials=name=>JSON.parse(fs.readFileSync(path.join(target,'study-'+name+'.json'),'utf8'));
async function call(route,token,body,status=200,key=crypto.randomUUID()){
 const response=await fetch(base+route,{method:body?'POST':'GET',headers:{'Content-Type':'application/json','Idempotency-Key':key,...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});
 const data=await response.json();assert.equal(response.status,status,route+' '+(data.errorCode||data.detail||''));checks++;return data;
}
(async()=>{
 assert.equal((await call('/actuator/health')).status,'UP');await call('/api/v1/investments/products',null,null,401);
 const client=await call('/api/v1/auth/login',null,credentials('lucas'));
 const other=await call('/api/v1/auth/login',null,credentials('marina'));
 const token=client.accessToken,password=credentials('lucas').password;
 const before=await call('/api/v1/accounts/me/summary',token);const lotsBefore=await call('/api/v1/investments/positions',token);
 const products=await call('/api/v1/investments/products',token);assert.equal(products.length,2);
 const fund=products.find(p=>p.type==='FUND'),cdb=products.find(p=>p.type==='CDB');assert.equal(fund.fgcEligible,false);assert.equal(cdb.fgcEligible,true);assert.ok(!('fund'in cdb)&&!('quote'in cdb)&&!('cdb'in fund));assert.equal(cdb.cdb.cdiPercent,110);
 const intent={accountId:client.accountId,productId:fund.id,amount:100};
 const preview=await call('/api/v1/investments/preview',token,intent);assert.equal(preview.estimatedUnits,10);
 assert.equal((await call('/api/v1/accounts/me/summary',token)).balance,before.balance);
 assert.equal((await call('/api/v1/investments/positions',token)).length,lotsBefore.length);
 await call('/api/v1/investments/preview',other.accessToken,intent,422);
 await call('/api/v1/investments',token,{...intent,amount:100.001,confirmationPassword:password},400);
 const key=crypto.randomUUID();await call('/api/v1/investments',token,{...intent,confirmationPassword:'incorreta'},422,key);
 const body={...intent,confirmationPassword:password};
 const first=await call('/api/v1/investments',token,body,201,key);assert.equal(first.status,'COMPLETED');
 const replay=await call('/api/v1/investments',token,body,201,key);assert.equal(first.id,replay.id);
 await call('/api/v1/investments',token,{...body,amount:200},409,key);
 await call('/api/v1/investments/applications/'+first.id,other.accessToken,null,404);
 const deposit=await call('/api/v1/investments',token,{...body,productId:cdb.id,amount:500},201);
 const positions=await call('/api/v1/investments/positions',token);assert.equal(positions.length,lotsBefore.length+2);
 const cdbLot=positions.find(p=>p.id===deposit.positionId),fundLot=positions.find(p=>p.id===first.positionId);
 assert.ok(!('units'in cdbLot)&&!('unitPrice'in cdbLot)&&!('quoteDate'in cdbLot));assert.ok(cdbLot.maturityDate);assert.equal(fundLot.units,10);assert.equal(fundLot.quoteDate,'2026-10-23');
 const after=await call('/api/v1/accounts/me/summary',token);assert.equal(after.balance,before.balance-600);
 const entries=await call('/api/v1/accounts/me/entries?limit=100',token);assert.equal(entries.filter(e=>[first.id,deposit.id].includes(e.referenceId)&&e.type==='INVESTMENT_APPLICATION').length,2);
 const parallelKey=crypto.randomUUID();const parallel=await Promise.all([call('/api/v1/investments',token,body,201,parallelKey),call('/api/v1/investments',token,body,201,parallelKey)]);assert.equal(parallel[0].id,parallel[1].id);
 assert.equal((await call('/api/v1/accounts/me/summary',token)).balance,before.balance-700);
 fs.writeFileSync(path.join(target,'investment-browser-session.json'),JSON.stringify({token,accountId:client.accountId}));
 fs.writeFileSync(path.join(target,'investment-http-result.json'),JSON.stringify({status:'passed',checks,verified:['typed products','read-only preview','ownership','password','fractional cents','idempotency conflict and concurrent replay','fund and CDB snapshots','balance and bank entries']},null,2));
 console.log('PASS: '+checks+' chamadas HTTP de investimentos, incluindo replay concorrente.');
})().catch(error=>{console.error(error.message);process.exit(1)});
