package com.traintrack.coreapi.enrolment.bulk;

import com.traintrack.coreapi.enrolment.bulk.dto.BulkEnrolmentJobResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class BulkEnrolmentController {

    private final BulkEnrolmentService bulkEnrolmentService;

    public BulkEnrolmentController(BulkEnrolmentService bulkEnrolmentService) {
        this.bulkEnrolmentService = bulkEnrolmentService;
    }

    @PostMapping(value = "/api/v1/enrolments/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('ENROLMENT_CREATE')")
    public BulkEnrolmentJobResponse submit(@RequestParam("file") MultipartFile file) {
        return bulkEnrolmentService.submit(file);
    }

    @GetMapping("/api/v1/jobs/{id}")
    @PreAuthorize("hasAuthority('ENROLMENT_VIEW_ALL')")
    public BulkEnrolmentJobResponse getStatus(@PathVariable UUID id) {
        return bulkEnrolmentService.getStatus(id);
    }
}
