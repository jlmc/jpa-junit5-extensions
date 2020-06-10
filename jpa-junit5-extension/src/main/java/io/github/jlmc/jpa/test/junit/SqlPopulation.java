package io.github.jlmc.jpa.test.junit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

class SqlPopulation {

    private static final String DELIMITER = "(;(\r)?\n)|((\r)?\n)?(--)?.*(--(\r)?\n)";

    public static void execute(final Connection connection, final List<String> scripts, final List<String> statements) throws SQLException {
        connection.setAutoCommit(false);

        executeScripts(connection, scripts);
        executeStatements(connection, statements);

        connection.commit();
    }

    private static void executeStatements(final Connection connection, final List<String> statements) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            int countStatements = 0;

            for (String s : statements) {
                stmt.addBatch(s);
                countStatements++;
            }

            if (countStatements > 0) {
                stmt.executeBatch();
            }
        }
    }

    private static void executeScripts(final Connection connection, final List<String> scripts) throws SQLException {
        for (final String script : scripts) {
            executeSqlScript(connection, script);
        }
    }

    private static void executeSqlScript(Connection connection, String resourceName) throws SQLException {
        Objects.requireNonNull(resourceName);

        try {

            URL url = SqlPopulation.class.getResource(resourceName);
            Path path = Paths.get(url.toURI());

            executeSqlScript(connection, path.toFile());

        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("Can not execute the resource script +" + resourceName + "'", e);
        }
    }

    private static void executeSqlScript(Connection connection, File inputFile) throws IOException, SQLException {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(inputFile);

        Scanner scanner = new Scanner(inputFile, StandardCharsets.UTF_8);
        scanner.useDelimiter(DELIMITER);

        try (final Statement statement = connection.createStatement()) {
            int countBatches = 0;
            while (scanner.hasNext()) {

                final String line = scanner.next();

                String sql = toSqlStatement(line);

                if (sql != null) {
                    statement.addBatch(sql);
                    countBatches++;
                }
            }
            if (countBatches > 0) {
                statement.executeBatch();
            }
        }
    }

    private static String toSqlStatement(final String line) {
        String sql = line;

        if (line.startsWith("/*!") && line.endsWith("*/")) {
            int i = line.indexOf(' ');
            sql = line.substring(i + 1, line.length() - " */".length());
        }

        if (!sql.trim().isEmpty()) {
            return sql;
        } else {
            return null;
        }
    }

}
