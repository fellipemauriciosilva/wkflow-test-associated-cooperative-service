package com.cooperative.application.config;

import com.cooperative.application.usecases.CreateAssemblyUseCase;
import com.cooperative.application.usecases.CreateAssociateUseCase;
import com.cooperative.application.usecases.GetAssemblyUseCase;
import com.cooperative.application.usecases.GetAssociateUseCase;
import com.cooperative.application.usecases.ListAssembliesUseCase;
import com.cooperative.application.usecases.ListAssociatesUseCase;
import com.cooperative.domain.ports.AssemblyRepository;
import com.cooperative.domain.ports.AssociateRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application layer configuration.
 * Instantiates use cases and registers them as Spring beans.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public CreateAssociateUseCase createAssociateUseCase(AssociateRepository repository) {
        return new CreateAssociateUseCase(repository);
    }

    @Bean
    public GetAssociateUseCase getAssociateUseCase(AssociateRepository repository) {
        return new GetAssociateUseCase(repository);
    }

    @Bean
    public ListAssociatesUseCase listAssociatesUseCase(AssociateRepository repository) {
        return new ListAssociatesUseCase(repository);
    }

    @Bean
    public CreateAssemblyUseCase createAssemblyUseCase(AssemblyRepository repository) {
        return new CreateAssemblyUseCase(repository);
    }

    @Bean
    public GetAssemblyUseCase getAssemblyUseCase(AssemblyRepository repository) {
        return new GetAssemblyUseCase(repository);
    }

    @Bean
    public ListAssembliesUseCase listAssembliesUseCase(AssemblyRepository repository) {
        return new ListAssembliesUseCase(repository);
    }
}
