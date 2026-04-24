package com.ab.bankloanprocessor.controller;

import com.ab.bankloanprocessor.controller.request.CreatePartyRequest;
import com.ab.bankloanprocessor.entity.Party;
import com.ab.bankloanprocessor.service.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/party")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService service;

    @PostMapping
    public ResponseEntity<Party> createParty(@Valid @RequestBody CreatePartyRequest request) {
        var party = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(party);
    }
}
