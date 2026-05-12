package pl.cafteo.jdgflow.ocr.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.cafteo.jdgflow.common.security.AuthenticatedUser;
import pl.cafteo.jdgflow.ocr.api.dto.ReceiptUploadResponse;
import pl.cafteo.jdgflow.ocr.domain.Receipt;
import pl.cafteo.jdgflow.ocr.service.ReceiptOcrService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptOcrService ocrService;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptUploadResponse> upload(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @RequestParam("file") MultipartFile file) {
        ReceiptUploadResponse body = ReceiptUploadResponse.from(ocrService.processUpload(user.id(), file));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/receipts/" + body.id() + "/image"))
                .body(body);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable UUID id) {
        Receipt receipt = ocrService.getOwnedReceipt(user.id(), id);
        byte[] data = ocrService.downloadFile(receipt);

        String filename = receipt.getOriginalFilename() != null
                ? receipt.getOriginalFilename()
                : "receipt";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(data);
    }
}
