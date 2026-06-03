package com.terubase.starter.maven.fixture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_plan_fixture")
class InvoicePlanFixture {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String customerName;
}
