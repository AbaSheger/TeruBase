package com.terubase.starter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/terubase/api/scenarios")
@CrossOrigin(origins = "*")
public class TeruBaseScenarioController {

    private final ScenarioTemplateService scenarioTemplateService;

    public TeruBaseScenarioController(ScenarioTemplateService scenarioTemplateService) {
        this.scenarioTemplateService = scenarioTemplateService;
    }

    @GetMapping
    @CrossOrigin(origins = "*")
    public List<ScenarioTemplate> scenarios() {
        return scenarioTemplateService.findAll();
    }

    @GetMapping("/{id}")
    @CrossOrigin(origins = "*")
    public ResponseEntity<ScenarioTemplate> scenario(@PathVariable String id) {
        return scenarioTemplateService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

