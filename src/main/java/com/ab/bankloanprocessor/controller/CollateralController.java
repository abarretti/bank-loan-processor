package com.ab.bankloanprocessor.controller;

import com.ab.bankloanprocessor.controller.request.AddCollateralRequest;
import com.ab.bankloanprocessor.domain.CollateralStatus;
import com.ab.bankloanprocessor.service.CollateralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/collateral")
@RequiredArgsConstructor
public class CollateralController {

    private final CollateralService service;

    @PostMapping("/{loan_id}")
    public ResponseEntity<List<Map<UUID, CollateralStatus>>> add(
            @PathVariable(name = "loan_id") UUID loanId,
            @Valid @RequestBody AddCollateralRequest request
    ) {
        var collateralIds = service.add(loanId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collateralIds);
    }

    @PatchMapping("/reserve/{collateral_id}")
    public ResponseEntity<List<Map<UUID, CollateralStatus>>> reserve(
            @PathVariable(name = "collateral_id") UUID collateralId
    ) {
        var collateralIds = service.reserve(collateralId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(collateralIds);
    }

    @PatchMapping("/pledge/{collateral_id}")
    public ResponseEntity<List<Map<UUID, CollateralStatus>>> pledge(
            @PathVariable(name = "collateral_id") UUID collateralId
    ) {
        var collateralIds = service.pledge(collateralId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(collateralIds);
    }

    @PatchMapping("/release/{collateral_id}")
    public ResponseEntity<List<Map<UUID, CollateralStatus>>> release(
            @PathVariable(name = "collateral_id") UUID collateralId
    ) {
        var collateralIds = service.release(collateralId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(collateralIds);
    }
}
