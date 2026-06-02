package com.terubase.starter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terubase/api")
@CrossOrigin(origins = "*")
public class TeruBaseSeedPlanController {

    private final SeedPlanService seedPlanService;

    public TeruBaseSeedPlanController(SeedPlanService seedPlanService) {
        this.seedPlanService = seedPlanService;
    }

    @GetMapping("/seed-plan")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> seedPlan(
            @RequestParam(required = false) String scenarioId,
            @RequestParam(required = false) String scenario,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "h2-postgresql-mode") String dialect
    ) {
        try {
            return ResponseEntity.ok(seedPlanService.generate(scenarioId, scenario, count, dialect));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(TeruBaseController.error("invalid_seed_plan_request", ex));
        }
    }
}
