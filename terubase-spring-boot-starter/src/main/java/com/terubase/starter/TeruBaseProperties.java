package com.terubase.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "terubase")
public class TeruBaseProperties {

    private boolean enabled = true;
    private String jdbcUrl = "jdbc:h2:mem:terubase_isolated_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    private String username = "sa";
    private String password = "";
    private String entityBasePackage;
    private String openAiChatCompletionsUrl = "https://api.openai.com/v1/chat/completions";
    private String openAiModel = "gpt-4o";
    private int maxMockRows = 100;
    private boolean sqlExecutionEnabled = false;
    private boolean forceEnableInProduction = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEntityBasePackage() {
        return entityBasePackage;
    }

    public void setEntityBasePackage(String entityBasePackage) {
        this.entityBasePackage = entityBasePackage;
    }

    public String getOpenAiChatCompletionsUrl() {
        return openAiChatCompletionsUrl;
    }

    public void setOpenAiChatCompletionsUrl(String openAiChatCompletionsUrl) {
        this.openAiChatCompletionsUrl = openAiChatCompletionsUrl;
    }

    public String getOpenAiModel() {
        return openAiModel;
    }

    public void setOpenAiModel(String openAiModel) {
        this.openAiModel = openAiModel;
    }

    public int getMaxMockRows() {
        return maxMockRows;
    }

    public void setMaxMockRows(int maxMockRows) {
        this.maxMockRows = maxMockRows;
    }

    public boolean isSqlExecutionEnabled() {
        return sqlExecutionEnabled;
    }

    public void setSqlExecutionEnabled(boolean sqlExecutionEnabled) {
        this.sqlExecutionEnabled = sqlExecutionEnabled;
    }

    public boolean isForceEnableInProduction() {
        return forceEnableInProduction;
    }

    public void setForceEnableInProduction(boolean forceEnableInProduction) {
        this.forceEnableInProduction = forceEnableInProduction;
    }
}
