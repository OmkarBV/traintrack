package com.traintrack.coreapi.enrolment;

import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.enrolment.dto.EnrolmentCreateRequest;
import com.traintrack.coreapi.enrolment.dto.EnrolmentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrolments")
@Validated
public class EnrolmentController {

    private final EnrolmentService enrolmentService;

    public EnrolmentController(EnrolmentService enrolmentService) {
        this.enrolmentService = enrolmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ENROLMENT_CREATE')")
    public EnrolmentResponse create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody EnrolmentCreateRequest request) {
        return enrolmentService.createIdempotent(idempotencyKey, request);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('ENROLMENT_COMPLETE')")
    public CertificationResponse complete(@PathVariable UUID id) {
        return enrolmentService.complete(id);
    }
}
