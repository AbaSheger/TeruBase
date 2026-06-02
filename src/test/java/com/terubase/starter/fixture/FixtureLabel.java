package com.terubase.starter.fixture;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fixture_label")
public class FixtureLabel {

    @Id
    private Long id;
}
