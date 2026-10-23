const fs=require('fs'),path=require('path'),assert=require('assert/strict');const target=path.resolve(__dirname,'../target'),base='http://127.0.0.1:28081';
const session=JSON.parse(fs.readFileSync(path.join(target,'investment-browser-session.json'),'utf8'));const credentials=JSON.parse(fs.readFileSync(path.join(target,'study-lucas.json'),'utf8'));
async function call(route,body){const response=await fetch(base+route,{method:body?'POST':'GET',headers:{'Content-Type':'application/json',Authorization:'Bearer '+session.token,'Idempotency-Key':crypto.randomUUID()},...(body?{body:JSON.stringify(body)}:{})});return {status:response.status,data:await response.json()};}
(async()=>{const before=(await call('/api/v1/accounts/me/summary')).data,positions=(await call('/api/v1/investments/positions')).data;
 const amount=Math.floor(before.balance*0.6);assert.ok(amount>=100&&amount*2>before.balance,'This fixture needs sufficient disposable study balance.');
 const product=(await call('/api/v1/investments/products')).data.find(p=>p.type==='FUND');const body={accountId:session.accountId,productId:product.id,amount,confirmationPassword:credentials.password};
 const results=await Promise.all([call('/api/v1/investments',body),call('/api/v1/investments',body)]);assert.deepEqual(results.map(r=>r.status).sort(),[201,422]);
 const after=(await call('/api/v1/accounts/me/summary')).data;assert.equal(after.balance,before.balance-amount);assert.ok(after.balance>=0);assert.equal((await call('/api/v1/investments/positions')).data.length,positions.length+1);
 fs.writeFileSync(path.join(target,'investment-concurrency-result.json'),JSON.stringify({status:'passed',verified:['distinct concurrent keys cannot overspend the account','one application and one debit only','nonnegative final balance']},null,2));console.log('PASS: duas intenções concorrentes; apenas uma aplicação e saldo não negativo.');
})().catch(e=>{console.error(e.message);process.exit(1)});
