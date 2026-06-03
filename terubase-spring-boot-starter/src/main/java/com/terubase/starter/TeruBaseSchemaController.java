package com.terubase.starter;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/terubase/api")
@CrossOrigin(origins = "*")
public class TeruBaseSchemaController {

    private final TeruBaseSchemaService schemaService;

    public TeruBaseSchemaController(TeruBaseSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping("/entities")
    @CrossOrigin(origins = "*")
    public List<Map<String, Object>> entities() {
        return schemaService.entities();
    }
}
