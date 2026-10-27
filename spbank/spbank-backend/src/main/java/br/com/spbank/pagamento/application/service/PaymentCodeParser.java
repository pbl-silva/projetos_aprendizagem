package br.com.spbank.pagamento.application.service;
import br.com.spbank.shared.application.exception.BusinessException;
public final class PaymentCodeParser {
 private PaymentCodeParser(){}
 public static String parse(String value){
  if(value==null||!value.matches("[0-9 .-]+"))throw invalid();
  String digits=value.replaceAll("[ .-]","");
  if(digits.length()==47){
   check10(digits.substring(0,9),digits.charAt(9));check10(digits.substring(10,20),digits.charAt(20));check10(digits.substring(21,31),digits.charAt(31));
   digits=digits.substring(0,4)+digits.charAt(32)+digits.substring(33)+digits.substring(4,9)+digits.substring(10,20)+digits.substring(21,31);
  }else if(digits.length()==48){
   boolean mod10=digits.charAt(2)=='6'||digits.charAt(2)=='7';
   if(!mod10&&digits.charAt(2)!='8'&&digits.charAt(2)!='9')throw invalid();
   var code=new StringBuilder();for(int i=0;i<48;i+=12){var block=digits.substring(i,i+11);int dv=mod10?mod10(block):collection11(block);if(dv!=digits.charAt(i+11)-'0')throw invalid();code.append(block);}digits=code.toString();
  }
  if(digits.length()!=44)throw invalid();
  if(digits.charAt(0)=='8'){
   char algorithm=digits.charAt(2);if(algorithm!='6'&&algorithm!='7'&&algorithm!='8'&&algorithm!='9')throw invalid();
   String payload=digits.substring(0,3)+digits.substring(4);int dv=algorithm=='6'||algorithm=='7'?mod10(payload):collection11(payload);
   if(dv!=digits.charAt(3)-'0')throw invalid();
  }else if(boletoDv(digits)!=digits.charAt(4)-'0')throw invalid();
  return digits;
 }
 public static int boletoDv(String code){int sum=0,weight=2;for(int i=43;i>=0;i--){if(i==4)continue;sum+=(code.charAt(i)-'0')*weight;weight=weight==9?2:weight+1;}int dv=11-sum%11;return dv==0||dv==10||dv==11?1:dv;}
 public static int mod10(String code){int sum=0,weight=2;for(int i=code.length()-1;i>=0;i--){int v=(code.charAt(i)-'0')*weight;sum+=v/10+v%10;weight=3-weight;}return (10-sum%10)%10;}
 private static int collection11(String code){int sum=0,weight=2;for(int i=code.length()-1;i>=0;i--){sum+=(code.charAt(i)-'0')*weight;weight=weight==9?2:weight+1;}int rest=sum%11;return rest==0||rest==1?0:rest==10?1:11-rest;}
 private static void check10(String block,char digit){if(mod10(block)!=digit-'0')throw invalid();}
 private static BusinessException invalid(){return new BusinessException("PAYMENT_INVALID_CODE","payment.invalid-code");}
}
