package com.cooperative.associated.infrastructure.rest;

import com.cooperative.associated.application.AssociateService;
import com.cooperative.associated.domain.Associate;
import com.cooperative.associated.infrastructure.rest.dto.AssociateRequest;
import com.cooperative.associated.infrastructure.rest.dto.AssociateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/associates")
public class AssociateController {

    private final AssociateService service;

    public AssociateController(AssociateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AssociateResponse> create(@RequestBody AssociateRequest request) {
        Associate created = service.create(request.name(), request.document(), request.email());
        AssociateResponse body = AssociateResponse.from(created);
        return ResponseEntity.created(URI.create("/associates/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssociateResponse> getById(@PathVariable UUID id) {
        Associate associate = service.findById(id);
        return ResponseEntity.ok(AssociateResponse.from(associate));
    }

    @GetMapping
    public ResponseEntity<List<AssociateResponse>> list() {
        List<AssociateResponse> body = service.findAll().stream()
                .map(AssociateResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssociateResponse> update(@PathVariable UUID id, @RequestBody AssociateRequest request) {
        Associate updated = service.update(id, request.name(), request.document(), request.email());
        return ResponseEntity.ok(AssociateResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}



