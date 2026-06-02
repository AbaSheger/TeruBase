package com.terubase.starter;

import java.util.List;
import java.util.Optional;

public class ScenarioTemplateService {

    private static final List<ScenarioTemplate> TEMPLATES = List.of(
            new ScenarioTemplate(
                    "ecommerce-demo",
                    "E-commerce Demo",
                    "A realistic storefront dataset with customers, products, orders, and payments.",
                    "Generate an e-commerce demo with varied customers, products, order states, line items, and payments.",
                    List.of("storefront demos", "order flows", "payment states"),
                    List.of("Customer", "Product", "Order", "OrderItem", "Payment")
            ),
            new ScenarioTemplate(
                    "saas-billing-demo",
                    "SaaS Billing Demo",
                    "A subscription billing dataset with plans, invoices, and payment outcomes.",
                    "Generate a SaaS billing demo with plans, active and canceled subscriptions, invoices, and payment outcomes.",
                    List.of("billing dashboards", "subscription flows", "invoice states"),
                    List.of("Account", "Plan", "Subscription", "Invoice", "Payment")
            ),
            new ScenarioTemplate(
                    "crm-demo",
                    "CRM Demo",
                    "A sales pipeline dataset with accounts, contacts, opportunities, and activities.",
                    "Generate a CRM demo with accounts, contacts, opportunities across pipeline stages, and recent activities.",
                    List.of("sales dashboards", "pipeline views", "activity feeds"),
                    List.of("Account", "Contact", "Opportunity", "Activity")
            ),
            new ScenarioTemplate(
                    "banking-lite-demo",
                    "Banking Lite Demo",
                    "A fictional retail banking dataset with accounts and transaction history.",
                    "Generate a fictional banking demo with customers, accounts, transfers, deposits, and card transactions.",
                    List.of("transaction views", "account summaries", "financial dashboards"),
                    List.of("Customer", "Account", "Transaction", "Transfer", "Card")
            ),
            new ScenarioTemplate(
                    "task-management-demo",
                    "Task Management Demo",
                    "A collaborative project dataset with teams, tasks, comments, and deadlines.",
                    "Generate a task management demo with teams, projects, tasks in varied states, comments, and deadlines.",
                    List.of("kanban boards", "team dashboards", "deadline views"),
                    List.of("User", "Team", "Project", "Task", "Comment")
            ),
            new ScenarioTemplate(
                    "qa-edge-cases",
                    "QA Edge Cases",
                    "A boundary-focused dataset for validating application behavior.",
                    "Generate QA seed data with empty optional fields, boundary values, varied states, and relationship edge cases.",
                    List.of("QA validation", "regression testing", "boundary behavior"),
                    List.of("Use the application's discovered entities")
            ),
            new ScenarioTemplate(
                    "frontend-dashboard-demo",
                    "Frontend Dashboard Demo",
                    "A visually varied dataset for charts, tables, filters, and recent activity.",
                    "Generate dashboard-friendly seed data with varied statuses, dates, totals, and recent activity.",
                    List.of("frontend demos", "charts", "tables and filters"),
                    List.of("Use the application's discovered entities")
            )
    );

    public List<ScenarioTemplate> findAll() {
        return TEMPLATES;
    }

    public Optional<ScenarioTemplate> findById(String id) {
        return TEMPLATES.stream()
                .filter(template -> template.id().equals(id))
                .findFirst();
    }
}

