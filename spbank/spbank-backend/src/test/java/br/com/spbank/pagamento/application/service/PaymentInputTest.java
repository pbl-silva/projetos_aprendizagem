package br.com.spbank.pagamento.application.service;
import static org.assertj.core.api.Assertions.*;
import br.com.spbank.shared.application.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
class PaymentInputTest {
    @ParameterizedTest @ValueSource(strings={"11999991234","+55 (11) 99999-1234","(99) 91234-5678"})
    void validPhones(String value){assertThat(MobilePhoneNormalizer.normalize(value)).hasSize(11);}
    @ParameterizedTest @ValueSource(strings={"20999991234","1199991234","11899991234","abc11999991234","+111999991234","00999991234"})
    void invalidPhones(String value){assertThatExceptionOfType(BusinessException.class).isThrownBy(()->MobilePhoneNormalizer.normalize(value));}
    @ParameterizedTest @ValueSource(strings={"abc","123","00000000000000000000000000000000000000000000","99999999999999999999999999999999999999999999"})
    void invalidCodes(String value){assertThatExceptionOfType(BusinessException.class).isThrownBy(()->PaymentCodeParser.parse(value));}
    @Test void boletoGeneralDvAndLineDvsAreChecked(){
        String original="99990000000000004000123456789012345678901234";
        String code=original.substring(0,4)+PaymentCodeParser.boletoDv(original)+original.substring(5);
        assertThat(PaymentCodeParser.parse(code)).isEqualTo(code);
        String a=code.substring(0,4)+code.substring(19,24),b=code.substring(24,34),c=code.substring(34);
        String line=a+PaymentCodeParser.mod10(a)+b+PaymentCodeParser.mod10(b)+c+PaymentCodeParser.mod10(c)+code.charAt(4)+code.substring(5,19);
        assertThat(PaymentCodeParser.parse(line)).isEqualTo(code);
        String changed=line.substring(0,9)+(line.charAt(9)=='0'?'1':'0')+line.substring(10);
        assertThatExceptionOfType(BusinessException.class).isThrownBy(()->PaymentCodeParser.parse(changed));
    }
    @Test void collectionBarcodeAnd48DigitLine(){
        String original="81600000000000000000000000000000000000000000";
        String code=original.substring(0,3)+PaymentCodeParser.mod10(original.substring(0,3)+original.substring(4))+original.substring(4);
        StringBuilder line=new StringBuilder();
        for(int i=0;i<44;i+=11){String b=code.substring(i,i+11);line.append(b).append(PaymentCodeParser.mod10(b));}
        assertThat(PaymentCodeParser.parse(line.toString())).isEqualTo(code);
    }
}
