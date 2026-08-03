package com.cooperative.associated.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests ensuring the hexagonal isolation of the {@code domain} package:
 * it must not depend on Spring, JPA, Kafka or HTTP/servlet classes.
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .importPackages("com.cooperative.associated");
    }

    @Test
    void domainClassesShouldNotDependOnSpringFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

        rule.check(importedClasses);
    }

    @Test
    void domainClassesShouldNotDependOnJpa() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..");

        rule.check(importedClasses);
    }

    @Test
    void domainClassesShouldNotDependOnKafka() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.apache.kafka..");

        rule.check(importedClasses);
    }

    @Test
    void domainClassesShouldNotDependOnHttpOrServletApis() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.servlet..", "jakarta.ws.rs..", "org.springframework.web..");

        rule.check(importedClasses);
    }

    @Test
    void domainPackageShouldNotDependOnInfrastructureOrApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..application..");

        rule.check(importedClasses);
    }
}

