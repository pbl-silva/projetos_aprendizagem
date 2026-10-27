package br.com.spbank.pagamento.application.service;
import br.com.spbank.shared.application.exception.BusinessException;
import java.util.Set;
public final class MobilePhoneNormalizer {
 private static final Set<String> DDDS=Set.of("11","12","13","14","15","16","17","18","19","21","22","24","27","28","31","32","33","34","35","37","38","41","42","43","44","45","46","47","48","49","51","53","54","55","61","62","63","64","65","66","67","68","69","71","73","74","75","77","79","81","82","83","84","85","86","87","88","89","91","92","93","94","95","96","97","98","99");
 private MobilePhoneNormalizer(){}
 public static String normalize(String input){if(input==null||!input.matches("[+0-9 ()-]+"))throw invalid();String value=input.replaceAll("[+ ()-]","");if(value.length()==13&&value.startsWith("55"))value=value.substring(2);if(value.length()!=11||!DDDS.contains(value.substring(0,2))||value.charAt(2)!='9')throw invalid();return value;}
 public static String mask(String value){return "("+value.substring(0,2)+") 9****-"+value.substring(7);}
 private static BusinessException invalid(){return new BusinessException("PAYMENT_INVALID_PHONE","payment.invalid-phone");}
}
