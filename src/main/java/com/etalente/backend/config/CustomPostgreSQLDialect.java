package com.etalente.backend.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;

public class CustomPostgreSQLDialect extends PostgreSQLDialect {

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        var typeConfiguration = functionContributions.getTypeConfiguration();
        var functionRegistry = functionContributions.getFunctionRegistry();

        // Register jsonb_extract_path_text function
        functionRegistry.registerPattern(
                "jsonb_extract_path_text",
                "jsonb_extract_path_text(?1, ?2)",
                typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)
        );

        // Register cast_to_integer function using PostgreSQL's ::integer syntax
        functionRegistry.registerPattern(
                "cast_to_integer",
                "(?1)::integer",
                typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.INTEGER)
        );

        // Register cast_to_text function using PostgreSQL's ::text syntax
        functionRegistry.registerPattern(
                "cast_to_text",
                "(?1)::text",
                typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)
        );
    }
}