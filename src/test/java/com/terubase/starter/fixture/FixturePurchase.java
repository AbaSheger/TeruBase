package com.terubase.starter.fixture;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "fixture_purchase")
public class FixturePurchase {

    @Id
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(
            name = "account_id",
            referencedColumnName = "id",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private FixtureAccount account;

    @ManyToMany
    @JoinTable(
            name = "fixture_purchase_label",
            joinColumns = @JoinColumn(name = "purchase_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<FixtureLabel> labels;
}
