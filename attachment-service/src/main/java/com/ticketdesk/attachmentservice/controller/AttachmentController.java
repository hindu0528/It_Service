package com.ticketdesk.attachmentservice.controller;

import com.ticketdesk.attachmentservice.dto.request.ConfirmAttachmentRequest;
import com.ticketdesk.attachmentservice.dto.request.PresignUrlRequest;
import com.ticketdesk.attachmentservice.dto.response.AttachmentResponse;
import com.ticketdesk.attachmentservice.dto.response.PresignUrlResponse;
import com.ticketdesk.attachmentservice.entity.Attachment;
import com.ticketdesk.attachmentservice.repository.AttachmentRepository;
import com.ticketdesk.attachmentservice.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachment Controller", description = "Endpoints for file uploading, presigned URLs, and attachment downloading")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload binary file attachment", description = "Uploads file attachment directly into storage for a ticket.")
    public ResponseEntity<AttachmentResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(name = "ticketId", required = false) Long ticketId) {
        AttachmentResponse response = attachmentService.uploadFile(file, ticketId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/presign")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate S3 presigned upload URL", description = "Issues a presigned upload URL stub for browser-direct file uploads.")
    public ResponseEntity<PresignUrlResponse> presignUrl(@Valid @RequestBody PresignUrlRequest request) {
        PresignUrlResponse response = attachmentService.generatePresignedUrl(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{ticketId}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm attachment for ticket ID", description = "Links uploaded attachment metadata to the specified ticket ID.")
    public ResponseEntity<AttachmentResponse> confirmAttachmentForTicket(@PathVariable("ticketId") Long ticketId,
                                                                         @RequestParam("attachmentId") Long attachmentId) {
        AttachmentResponse response = attachmentService.confirmAttachment(ticketId, attachmentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm attachment via body", description = "Links uploaded attachment metadata using ConfirmAttachmentRequest.")
    public ResponseEntity<AttachmentResponse> confirmAttachment(@Valid @RequestBody ConfirmAttachmentRequest request) {
        AttachmentResponse response = attachmentService.confirmAttachment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get attachments for a ticket", description = "Lists all linked attachment metadata for a given ticket.")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByTicketId(@PathVariable("ticketId") Long ticketId) {
        List<AttachmentResponse> response = attachmentService.getAttachmentsByTicketId(ticketId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/file/{id}")
    @Operation(summary = "Download / View raw file content", description = "Returns binary file bytes with inline headers for browser display.")
    public ResponseEntity<byte[]> getFileBytes(@PathVariable("id") Long id) {
        Attachment attachment = attachmentRepository.findById(id).orElse(null);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = attachment.getFileData();
        String contentType = attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream";

        if (data == null || data.length == 0) {
            data = generateFallbackFileBytes(attachment);
            if (contentType.startsWith("image/")) {
                contentType = "image/svg+xml";
            }
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(data);
    }

    private byte[] generateFallbackFileBytes(Attachment attachment) {
        if (attachment.getFileType() != null && attachment.getFileType().startsWith("image/")) {
            String svg = String.format("""
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="350" viewBox="0 0 600 350">
                  <rect width="600" height="350" fill="#1e293b" rx="16"/>
                  <rect x="20" y="20" width="560" height="310" fill="#0f172a" rx="12" stroke="#3b82f6" stroke-width="2" stroke-dasharray="8 8"/>
                  <text x="300" y="140" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="42" text-anchor="middle" fill="#3b82f6">🖼️</text>
                  <text x="300" y="195" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="#f8fafc">%s</text>
                  <text x="300" y="230" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="14" text-anchor="middle" fill="#94a3b8">Attachment Sample File • Target Ticket #%d</text>
                  <text x="300" y="260" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif" font-size="12" text-anchor="middle" fill="#4ade80">● Status: %s</text>
                </svg>
                """,
                attachment.getFileName(),
                attachment.getTicketId() != null ? attachment.getTicketId() : 0,
                attachment.getStatus()
            );
            return svg.getBytes(StandardCharsets.UTF_8);
        } else {
            String content = String.format("""
                =======================================================
                TICKETDESK ATTACHMENT DOCUMENT FILE
                =======================================================
                File Name : %s
                File Type : %s
                Ticket ID : #%d
                Status    : %s
                =======================================================
                This is a sample attachment file document preview.
                """,
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getTicketId() != null ? attachment.getTicketId() : 0,
                attachment.getStatus()
            );
            return content.getBytes(StandardCharsets.UTF_8);
        }
    }

    @GetMapping(value = "/preview/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String previewAttachment(@PathVariable("id") Long id) {
        Attachment attachment = attachmentRepository.findById(id).orElse(null);
        if (attachment == null) {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                  <title>Attachment Not Found</title>
                  <style>
                    body { font-family: 'Segoe UI', sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 2rem; display: flex; justify-content: center; align-items: center; min-height: 80vh; }
                    .card { background: #1e293b; border-radius: 12px; padding: 2rem; border: 1px solid #334155; text-align: center; max-width: 400px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h2 style="color:#ef4444;">⚠️ Attachment Not Found</h2>
                    <p style="color:#94a3b8;">The requested attachment record does not exist in the database.</p>
                  </div>
                </body>
                </html>
                """;
        }

        String fileUrl = "http://localhost:8080/api/v1/attachments/file/" + attachment.getId();
        boolean isImage = attachment.getFileType() != null && attachment.getFileType().startsWith("image/");
        String ticketInfo = attachment.getTicketId() != null ? "#" + attachment.getTicketId() : "Unlinked Draft";
        double sizeKb = attachment.getFileSize() != null ? attachment.getFileSize() / 1024.0 : 0.0;

        String mediaPreviewHtml = isImage
                ? String.format("<img src='%s' alt='%s' style='max-width:100%%; max-height:400px; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.5); object-fit:contain;' />", fileUrl, attachment.getFileName())
                : String.format("<div style='padding:2rem;'><div style='font-size:3rem;'>📄</div><div style='font-weight:700; color:#818cf8; margin-top:0.5rem;'>%s</div></div>", attachment.getFileName());

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <title>Attachment Viewer - %s</title>
              <style>
                body { font-family: 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 2.5rem 1rem; }
                .card { max-width: 680px; margin: 0 auto; background: #1e293b; border-radius: 16px; padding: 2rem; border: 1px solid #334155; box-shadow: 0 20px 40px rgba(0,0,0,0.6); }
                .badge-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
                .status-badge { padding: 4px 12px; border-radius: 9999px; background: rgba(34, 197, 94, 0.15); color: #4ade80; border: 1px solid rgba(34, 197, 94, 0.3); font-weight: 700; font-size: 0.8rem; }
                .title { font-size: 1.35rem; font-weight: 800; color: #3b82f6; margin-bottom: 1.5rem; word-break: break-all; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; background: #090d16; padding: 1.25rem; border-radius: 10px; border: 1px solid #334155; margin-bottom: 1.5rem; }
                .label { font-size: 0.75rem; text-transform: uppercase; color: #64748b; font-weight: 700; margin-bottom: 4px; }
                .val { font-size: 0.92rem; color: #e2e8f0; font-weight: 600; }
                .preview-box { background: #090d16; border-radius: 12px; padding: 1.5rem; border: 1px dashed #6366f1; text-align: center; margin-bottom: 1.5rem; }
                .download-btn { display: inline-flex; align-items: center; gap: 8px; padding: 12px 24px; background: linear-gradient(135deg, #2563eb, #3b82f6); color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 0.95rem; box-shadow: 0 4px 12px rgba(37,99,235,0.4); }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="badge-bar">
                  <span style="font-weight: 700; color: #94a3b8; font-size: 0.85rem;">TicketDesk Document Store</span>
                  <span class="status-badge">● %s</span>
                </div>
                <div class="title">📄 %s</div>
                
                <div class="preview-box">
                  %s
                </div>

                <div class="grid">
                  <div>
                    <div class="label">MIME File Type</div>
                    <div class="val">%s</div>
                  </div>
                  <div>
                    <div class="label">File Size</div>
                    <div class="val">%.1f KB</div>
                  </div>
                  <div>
                    <div class="label">Target Ticket</div>
                    <div class="val">%s</div>
                  </div>
                  <div>
                    <div class="label">Storage Provider</div>
                    <div class="val">Local Binary Database Store</div>
                  </div>
                </div>

                <div style="text-align: center; margin-top: 1rem;">
                  <a href="%s" target="_blank" download="%s" class="download-btn">
                    📥 Download Attachment File (%s)
                  </a>
                </div>
              </div>
            </body>
            </html>
            """,
            attachment.getFileName(),
            attachment.getStatus(),
            attachment.getFileName(),
            mediaPreviewHtml,
            attachment.getFileType(),
            sizeKb,
            ticketInfo,
            fileUrl,
            attachment.getFileName(),
            attachment.getFileName()
        );
    }
}
