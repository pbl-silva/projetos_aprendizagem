package br.com.spbank.pagamento.application.usecase;
import br.com.spbank.pagamento.application.model.PaymentModels.*;
import br.com.spbank.pagamento.application.port.in.PaymentUseCase;
import br.com.spbank.pagamento.application.port.out.*;
import br.com.spbank.pagamento.application.service.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional
public class PaymentService implements PaymentUseCase {
 private final PaymentPersistence store;private final PaymentSettlement settlement;private final AuthenticationPersistence customers;
 private final AccountPersistence accounts;private final AccountEntryPersistence entries;private final Clock clock;
 public PaymentService(PaymentPersistence store,PaymentSettlement settlement,AuthenticationPersistence customers,AccountPersistence accounts,AccountEntryPersistence entries,Clock clock){this.store=store;this.settlement=settlement;this.customers=customers;this.accounts=accounts;this.entries=entries;this.clock=clock;}
 private LocalDate today(){return LocalDate.ofInstant(clock.instant(),ZoneId.of("America/Sao_Paulo"));}
 public GeneratedCharge generate(UUID customer,String name,BigDecimal value){active(customer,true);value=money(value);
  name=name.strip().replaceAll("\s+"," ");if(name.length()<2||name.length()>100||name.chars().anyMatch(Character::isISOControl))throw error("NAME");
  String entropy=UUID.randomUUID().toString().replace("-","");StringBuilder free=new StringBuilder();for(int i=0;i<25;i++)free.append(Character.digit(entropy.charAt(i),16)%10);
  String code="99990"+"0000"+String.format(Locale.ROOT,"%010d",value.movePointRight(2).longValueExact())+free;
  code=code.substring(0,4)+PaymentCodeParser.boletoDv(code)+code.substring(5);PaymentCodeParser.parse(code);
  var charge=new Charge(PasswordHasher.tokenHash(code),mask(code),name,"***.***.***/****-**",value,today().plusDays(30),true);store.save(charge);return new GeneratedCharge(code,charge);
 }
 @Transactional(readOnly=true) public Preview preview(UUID customer,UUID account,String code,@Nullable LocalDate date){active(customer,false);var a=own(customer,account,false);var c=charge(code);var day=date(date,c.dueDate());return new Preview(account,"BILL",c.beneficiary(),c.maskedDocument(),c.mask(),c.amount(),c.dueDate(),day,day.equals(today())?a.getBalance().subtract(c.amount()):null,true);}
 @Transactional(readOnly=true) public Preview rechargePreview(UUID customer,UUID account,UUID product,String phone){active(customer,false);var a=own(customer,account,false);var p=product(product);var normalized=MobilePhoneNormalizer.normalize(phone);a.requireAvailable(p.amount());return new Preview(account,"RECHARGE",p.carrier(),"",MobilePhoneNormalizer.mask(normalized),p.amount(),today(),today(),a.getBalance().subtract(p.amount()),true);}
 public Payment pay(UUID customer,UUID key,Command cmd){active(customer,true);password(customer,cmd.confirmationPassword());
  String hash=PasswordHasher.tokenHash(PaymentCodeParser.parse(cmd.code()));var day=cmd.scheduledFor()==null?today():Objects.requireNonNull(cmd.scheduledFor());
  String intent=PasswordHasher.tokenHash("BILL|"+cmd.accountId()+"|"+hash+"|"+(cmd.scheduledFor()==null?"IMMEDIATE":day.toString()));var replay=replay(customer,key,intent);if(replay!=null)return replay;
  var a=own(customer,cmd.accountId(),true);var c=store.charge(hash).filter(v->v.active()).orElseThrow(()->error("CHARGE"));date(day,c.dueDate());
  if(store.payments(customer).stream().anyMatch(p->p.kind().equals("BILL")&&p.targetHash().equals(hash)&&p.status()!=Status.CANCELLED&&p.status()!=Status.FAILED))throw error("DUPLICATE");
  var row=new Payment(UUID.randomUUID(),customer,a.getId(),accountMask(a),key,intent,"BILL",hash,c.mask(),c.beneficiary(),c.amount(),c.dueDate(),day,Status.SCHEDULED,clock.instant(),null,null,null,null);
  if(day.equals(today()))row=complete(row,a);store.save(row);return row;
 }
 public Payment recharge(UUID customer,UUID key,RechargeCommand cmd){active(customer,true);password(customer,cmd.confirmationPassword());
  var phone=MobilePhoneNormalizer.normalize(cmd.phone());String hash=PasswordHasher.tokenHash(phone);String intent=PasswordHasher.tokenHash("RECHARGE|"+cmd.accountId()+"|"+cmd.productId()+"|"+hash);var replay=replay(customer,key,intent);if(replay!=null)return replay;
  var a=own(customer,cmd.accountId(),true);var p=product(cmd.productId());var row=new Payment(UUID.randomUUID(),customer,a.getId(),accountMask(a),key,intent,"RECHARGE",hash,MobilePhoneNormalizer.mask(phone),p.carrier(),p.amount(),today(),today(),Status.PROCESSING,clock.instant(),null,null,null,null);
  row=complete(row,a);store.save(row);return row;
 }
 @Transactional(readOnly=true) public List<Product> products(UUID customer){active(customer,false);return store.products().stream().filter(p->p.active()).toList();}
 @Transactional(readOnly=true) public List<Payment> history(UUID customer){active(customer,false);return store.payments(customer);}
 @Transactional(readOnly=true) public Payment get(UUID customer,UUID id){active(customer,false);return find(customer,id);}
 public Payment cancel(UUID customer,UUID id){active(customer,true);var row=find(customer,id);if(row.status()!=Status.SCHEDULED)throw error("STATE");var cancelled=state(row,Status.CANCELLED,null,null,null,null);store.save(cancelled);return cancelled;}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void execute(UUID id){var owner=store.owner(id);if(owner.isEmpty())return;var customer=owner.orElseThrow();customers.findCustomerByIdForUpdate(customer);var row=find(customer,id);if(row.status()!=Status.SCHEDULED||row.scheduledFor().isAfter(today()))return;
  Account a;try{active(customer,false);a=own(customer,row.accountId(),true);var c=store.charge(row.targetHash()).filter(v->v.active()).orElseThrow(()->error("CHARGE"));
   if(c.amount().compareTo(row.amount())!=0)throw error("AMOUNT");date(today(),c.dueDate());a.requireAvailable(row.amount());
  }catch(BusinessException failure){store.save(state(row,Status.FAILED,null,null,null,failure.getCode()));return;}
  store.save(state(row,Status.PROCESSING,null,null,null,null));store.save(complete(row,a));
 }
 private @org.eclipse.jdt.annotation.NonNull Payment complete(Payment row,Account a){a.requireAvailable(row.amount());String reference=settlement.settle(row.id(),row.kind(),row.targetHash(),row.amount());a.debit(row.amount());accounts.saveAll(List.of(a));
  entries.saveAll(List.of(AccountEntry.create(a,row.id(),EntryReferenceType.PAYMENT,row.kind().equals("BILL")?EntryType.PAYMENT_OUT:EntryType.MOBILE_RECHARGE_OUT,EntryDirection.DEBIT,row.amount(),(row.kind().equals("BILL")?"Pagamento: ":"Recarga: ")+row.beneficiary(),clock.instant())));
  return state(row,Status.COMPLETED,clock.instant(),a.getBalance(),reference,null);
 }
 private static @org.eclipse.jdt.annotation.NonNull Payment state(Payment p,Status status,@Nullable Instant done,@Nullable BigDecimal balance,@Nullable String reference,@Nullable String failure){return new Payment(p.id(),p.customerId(),p.accountId(),p.maskedAccount(),p.key(),p.fingerprint(),p.kind(),p.targetHash(),p.targetMask(),p.beneficiary(),p.amount(),p.dueDate(),p.scheduledFor(),status,p.createdAt(),done,balance,reference,failure);}
 private @Nullable Payment replay(UUID customer,UUID key,String intent){var row=store.payments(customer).stream().filter(p->p.key().equals(key)).findFirst();if(row.isEmpty())return null;var p=row.orElseThrow();if(!p.fingerprint().equals(intent))throw new IdempotencyConflictException();return p;}
 private Payment find(UUID customer,UUID id){return store.payments(customer).stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new NotFoundException("PAYMENT_NOT_FOUND"));}
 private Charge charge(String code){return store.charge(PasswordHasher.tokenHash(PaymentCodeParser.parse(code))).filter(c->c.active()).orElseThrow(()->error("CHARGE"));}
 private Product product(UUID id){return store.product(id).filter(p->p.active()).orElseThrow(()->error("PRODUCT"));}
 private LocalDate date(@Nullable LocalDate date,LocalDate due){var day=date==null?today():date;if(day.isBefore(today())||day.isAfter(due))throw error("DATE");return day;}
 private void active(UUID id,boolean lock){(lock?customers.findCustomerByIdForUpdate(id):customers.findCustomerById(id)).filter(c->c.active()).orElseThrow(UnauthorizedException::new);}
 private void password(UUID id,String value){if(customers.findCredentialByCustomerId(id).filter(c->c.active()&&PasswordHasher.matches(value,c.passwordHash())).isEmpty())throw error("PASSWORD");}
 private Account own(UUID customer,UUID id,boolean lock){var a=lock?accounts.findAllForUpdate(List.of(id)).get(id):accounts.findById(id).orElse(null);if(a==null||!a.isActive()||!a.getCustomerId().equals(customer))throw error("ACCOUNT");return a;}
 private static String accountMask(Account a){var n=a.getAccountNumber();return "****"+n.substring(Math.max(0,n.length()-4));}
 private static String mask(String value){return value.substring(0,4)+" **** **** "+value.substring(value.length()-4);}
 private static BigDecimal money(BigDecimal value){if(value==null||value.signum()<=0||value.scale()>2||value.compareTo(new BigDecimal("99999999.99"))>0)throw error("AMOUNT");return value.setScale(2);}
 private static BusinessException error(String suffix){return new BusinessException("PAYMENT_"+suffix,"payment."+suffix.toLowerCase(Locale.ROOT));}
}
