package com.ab.bankloanprocessor.service;

import com.ab.bankloanprocessor.controller.request.CreatePartyRequest;
import com.ab.bankloanprocessor.entity.Party;
import com.ab.bankloanprocessor.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository repository;

    public Party create(CreatePartyRequest request) {
        var party = Party.builder()
                .role(request.role())
                .metadata(request.metadata())
                .build();

        var postParty = repository.save(party);
        return postParty;
    }
}
