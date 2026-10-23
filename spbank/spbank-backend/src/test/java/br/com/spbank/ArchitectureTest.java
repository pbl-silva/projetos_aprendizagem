package br.com.spbank;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private final JavaClasses classes =
            new ClassFileImporter()
                    .importPackages(
                            "br.com.spbank"
                    );

    @Test
    void investmentsMustNotAccessExternalMarketDataOverTheNetwork() {
        noClasses().that().resideInAPackage("br.com.spbank.investimento..")
            .should().dependOnClassesThat().resideInAnyPackage("java.net.http..",
                "org.springframework.web.client..","org.springframework.web.reactive.function.client..",
                "okhttp3..","retrofit2..")
            .check(classes);
        noClasses().that().resideInAPackage("br.com.spbank.investimento..")
            .should().dependOnClassesThat().haveNameMatching("java\\.net\\.(Socket|URL|URLConnection|HttpURLConnection)")
            .check(classes);
    }

    @Test
    void adaptersOfEveryBusinessModuleStayIsolated() {
        var modules=classes.stream().map(type -> type.getPackageName())
            .filter(name -> name.startsWith("br.com.spbank.") && name.contains(".adapter."))
            .map(name -> name.split("\\.")[3]).filter(name -> !name.equals("shared"))
            .collect(java.util.stream.Collectors.toSet());
        for(var source:modules)for(var target:modules)if(!source.equals(target)) {
            noClasses().that().resideInAPackage("br.com.spbank."+source+"..adapter..")
                .should().dependOnClassesThat().resideInAPackage("br.com.spbank."+target+"..adapter..")
                .check(classes);
        }
    }

    @Test
    void applicationMustNotDependOnAdaptersOrFrameworkBoundaries() {

        noClasses()
                .that()
                .resideInAPackage(
                        "..application.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..adapter..",
                        "jakarta.persistence..",
                        "org.springframework.web.."
                )
                .check(classes);
    }

    @Test
    void JpaEntitiesMustStayInThePersistenceDataPackage() {

        classes()
                .that()
                .areAnnotatedWith(
                        jakarta.persistence.Entity.class
                )
                .should()
                .resideInAPackage(
                        "..adapter.out.persistence..data.."
                )
                .andShould()
                .haveSimpleNameEndingWith(
                        "Data"
                )
                .check(classes);
    }

    @Test
    void accountMustNotDependOnTransferModule() {

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.conta.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "br.com.spbank.transferencia.."
                )
                .check(classes);
    }

    @Test
    void pixMustNotDependOnTransferModule() {

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.pix.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        "br.com.spbank.transferencia.."
                )
                .check(classes);
    }

    @Test
    void sharedMustNotDependOnBusinessModules() {

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.shared.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.spbank.administracao..",
                        "br.com.spbank.autenticacao..",
                        "br.com.spbank.conta..",
                        "br.com.spbank.transferencia..",
                        "br.com.spbank.pix..",
                        "br.com.spbank.cartao..",
                        "br.com.spbank.investimento.."
                )
                .check(classes);
    }

    @Test
    void adaptersMustNotDependOnAdaptersFromOtherBusinessModules() {

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.administracao..adapter.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.spbank.autenticacao..adapter..",
                        "br.com.spbank.conta..adapter..",
                        "br.com.spbank.transferencia..adapter..",
                        "br.com.spbank.pix..adapter.."
                )
                .check(classes);

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.autenticacao..adapter.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.spbank.administracao..adapter..",
                        "br.com.spbank.conta..adapter..",
                        "br.com.spbank.transferencia..adapter..",
                        "br.com.spbank.pix..adapter.."
                )
                .check(classes);

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.conta..adapter.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.spbank.administracao..adapter..",
                        "br.com.spbank.autenticacao..adapter..",
                        "br.com.spbank.transferencia..adapter..",
                        "br.com.spbank.pix..adapter.."
                )
                .check(classes);

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.transferencia..adapter.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.spbank.administracao..adapter..",
                        "br.com.spbank.autenticacao..adapter..",
                        "br.com.spbank.conta..adapter..",
                        "br.com.spbank.pix..adapter.."
                )
                .check(classes);

        noClasses()
                .that()
                .resideInAPackage(
                        "br.com.spbank.pix..adapter.."
                )
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.spbank.administracao..adapter..",
                        "br.com.spbank.autenticacao..adapter..",
                        "br.com.spbank.conta..adapter..",
                        "br.com.spbank.transferencia..adapter.."
                )
                .check(classes);
    }

    @Test
    void topLevelModulesMustBeFreeOfCycles() {

        slices()
                .matching(
                        "br.com.spbank.(*).."
                )
                .should()
                .beFreeOfCycles()
                .check(classes);
    }

    @Test
    void transactionalServicesMustBeProxyable() {

        classes()
                .that()
                .areAnnotatedWith(
                        org.springframework.transaction.annotation.Transactional.class
                )
                .should()
                .notHaveModifier(
                        com.tngtech.archunit.core.domain.JavaModifier.FINAL
                )
                .check(classes);
    }

    @Test
    void modulePackagesMustNotRepeatTheFeatureName() {

        noClasses()
                .should()
                .resideInAnyPackage(
                        "br.com.spbank.transferencia.application.model.transfer..",
                        "br.com.spbank.transferencia.application.service.transfer..",
                        "br.com.spbank.transferencia.application.usecase.transfer..",
                        "br.com.spbank.transferencia.adapter.in.api.rest.controller.transfer..",
                        "br.com.spbank.transferencia.adapter.out.log.negocio..",
                        "br.com.spbank.conta.adapter.in.api.rest.controller.account..",
                        "br.com.spbank.conta.adapter.in.api.rest.controller.bank.."
                )
                .check(classes);
    }
}