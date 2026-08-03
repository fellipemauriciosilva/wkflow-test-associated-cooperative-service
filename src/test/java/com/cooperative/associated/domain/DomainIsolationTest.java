package com.cooperative.associated.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lightweight, source-scanning complement to {@code ArchitectureTest}: ensures the
 * {@code domain} package's source files never reference forbidden framework/infrastructure
 * packages, catching leaks even if a class isn't yet reachable by the ArchUnit import scope.
 */
class DomainIsolationTest {

    private static final List<String> FORBIDDEN_PREFIXES = List.of(
            "import org.springframework.",
            "import jakarta.persistence.",
            "import jakarta.servlet.",
            "import jakarta.ws.rs.",
            "import org.apache.kafka.",
            "import java.net.http.",
            "import javax.servlet."
    );

    @Test
    void domainSourceFilesShouldNotImportForbiddenPackages() throws IOException {
        Path domainDir = Paths.get("src", "main", "java", "com", "cooperative", "associated", "domain");
        assertTrue(Files.exists(domainDir), "domain source directory should exist: " + domainDir);

        try (Stream<Path> paths = Files.walk(domainDir)) {
            List<Path> javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();

            assertTrue(!javaFiles.isEmpty(), "expected at least one .java file under domain package");

            for (Path file : javaFiles) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    String trimmed = line.trim();
                    for (String forbidden : FORBIDDEN_PREFIXES) {
                        assertTrue(
                                !trimmed.startsWith(forbidden),
                                "Forbidden import '" + trimmed + "' found in domain source file: " + file
                        );
                    }
                }
            }
        }
    }
}


