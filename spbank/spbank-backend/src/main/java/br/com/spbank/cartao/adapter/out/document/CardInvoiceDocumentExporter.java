package br.com.spbank.cartao.adapter.out.document;
import br.com.spbank.cartao.application.usecase.CardFinanceService.InvoiceView;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;

@Component
public final class CardInvoiceDocumentExporter {
    private List<List<String>> rows(InvoiceView view){var rows=new ArrayList<List<String>>();
        rows.add(List.of("TIPO","REFERENCIA","PRINCIPAL","JUROS","SALDO"));
        rows.add(List.of("FATURA",view.invoice().brand()+" "+view.invoice().reference(),"","",view.remaining().toPlainString()));
        for(var row:view.obligations())rows.add(List.of(row.sourceType(),row.sourceId()+" #"+row.sequence(),row.principal().toPlainString(),row.interest().toPlainString(),row.remainingPrincipal().add(row.remainingInterest()).toPlainString()));
        for(var charge:view.charges())rows.add(List.of("MULTA_MORA",charge.obligationId().toString(),charge.fineCharged().toPlainString(),charge.moraCharged().toPlainString(),charge.remainingFine().add(charge.remainingMora()).toPlainString()));
        for(var allocation:view.allocations())rows.add(List.of(allocation.kind(),allocation.paymentId().toString(),allocation.principalPaid().toPlainString(),allocation.interestPaid().add(allocation.finePaid()).add(allocation.moraPaid()).toPlainString(),allocation.discount().toPlainString()));
        rows.add(List.of("SALDO_ATUAL",view.message(),"","",view.remaining().toPlainString()));return rows;}
    public byte[] csv(InvoiceView view){var builder=new StringBuilder("\ufeff");for(var row:rows(view)){
        for(int index=0;index<row.size();index++){if(index>0)builder.append(';');var value=row.get(index);
            if(index<2&&!value.isEmpty()&&"=+-@\t\r\n".indexOf(value.charAt(0))>=0)value="'"+value;
            builder.append('"').append(value.replace("\"","\"\"")).append('"');}builder.append("\r\n");}
        return builder.toString().getBytes(StandardCharsets.UTF_8);}
    public byte[] pdf(InvoiceView view){try(var document=new PDDocument();var output=new ByteArrayOutputStream()){
        var font=new PDType1Font(Standard14Fonts.FontName.HELVETICA);PDPageContentStream content=null;int line=0;
        try{for(var row:rows(view)){
            String text=Normalizer.normalize(String.join(" | ",row),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]"," ");
            for(int offset=0;offset<text.length();offset+=105){
                if(content==null||line==45){if(content!=null){content.endText();content.close();}var page=new PDPage();document.addPage(page);content=new PDPageContentStream(document,page);content.beginText();content.setFont(font,9);content.setLeading(15);content.newLineAtOffset(35,750);line=0;}
                content.showText(text.substring(offset,Math.min(text.length(),offset+105)));content.newLine();line++;
            }
        }}finally{if(content!=null){content.endText();content.close();}}
        document.save(output);return output.toByteArray();
    }catch(IOException ex){throw new IllegalStateException("Não foi possível exportar a fatura",ex);}}
}
