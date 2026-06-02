package com.terubase.starter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terubase/api/export")
@CrossOrigin(origins = "*")
public class TeruBaseExportController {

    private final TeruBaseExportService exportService;

    public TeruBaseExportController(TeruBaseExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/sql")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> exportSql(@RequestBody ExportRequest request) {
        try {
            return ResponseEntity.ok(exportService.exportSql(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(TeruBaseController.error("invalid_export_request", ex));
        }
    }

    @PostMapping("/json")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> exportJson(@RequestBody ExportRequest request) {
        try {
            return ResponseEntity.ok(exportService.exportJson(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(TeruBaseController.error("invalid_export_request", ex));
        }
    }
}
