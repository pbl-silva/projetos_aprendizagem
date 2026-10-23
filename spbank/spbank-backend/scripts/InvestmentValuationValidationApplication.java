import br.com.spbank.SpbankBackendApplication;
import java.nio.file.*;
import java.io.IOException;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.*;
/** Integration fixture only; never included in the application artifact. */
public class InvestmentValuationValidationApplication {
    public static void main(String[] args){new SpringApplication(SpbankBackendApplication.class,ValidationClock.class).run(args);}
    @Configuration(proxyBeanMethods=false)
    public static class ValidationClock {
        @Bean @Primary
        Clock validationClock(@Value("${validation.clock-file}") String file){
            var target=Path.of("target").toAbsolutePath().normalize();var clockFile=Path.of(file).toAbsolutePath().normalize();
            if(!clockFile.startsWith(target))throw new IllegalArgumentException("Clock fixture must remain inside target");
            return new Clock(){
                public ZoneId getZone(){return ZoneOffset.UTC;}
                public Clock withZone(ZoneId zone){return this;}
                public Instant instant(){try{return Instant.parse(Files.readString(clockFile).strip());}catch(IOException e){throw new IllegalStateException("Validation clock unavailable",e);}}
            };
        }
    }
}
