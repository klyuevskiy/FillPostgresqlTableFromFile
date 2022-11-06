import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DataBaseInserter {

    private DataBaseInserter() {

    }

    private static ResultSetMetaData getTableMetaData(Connection connection, String tableName) throws SQLException {
        return connection
                .createStatement()
                .executeQuery("select * from " + tableName + " where 1 = 0")
                .getMetaData();
    }

    private static void findColumnsMatchesErrors
            (String[] columnsNames, ResultSetMetaData tableMetaData) throws Exception {

        // колонки, которые есть в таблице
        Set<String> tableColumnsNames = new HashSet<>();
        // уже встреченные в файле колонки
        Set<String> fileColumnsNames = new HashSet<>();

        for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
            tableColumnsNames.add(tableMetaData.getColumnName(i).toLowerCase());
        }

        for (var columnName : columnsNames) {
            var columnNameLowerCase = columnName.toLowerCase();

            // нет в таблцице
            if (!tableColumnsNames.contains(columnNameLowerCase))
                throw new Exception(String.format("Столбца %s нет в таблице", columnName));

            // уже встречали такой столбец
            if (fileColumnsNames.contains(columnNameLowerCase))
                throw new Exception(String.format("Столбец %s повторяется несколько раз", columnName));

            // всё хорошо: не встречался ранее и есть в таблице
            fileColumnsNames.add(columnNameLowerCase);
        }
    }

    private static PreparedStatement getInsertStatement(Connection connection, String tableName, String[] columnsNames) throws SQLException {
        StringBuilder insertQuery = new StringBuilder(String.format("insert into %s (%s", tableName, columnsNames[0]));

        for (int i = 1; i < columnsNames.length; i++) {
            insertQuery.append(String.format(", %s", columnsNames[i]));
        }

        insertQuery.append(") values(");

        insertQuery
                .append("?, ".repeat(columnsNames.length - 1))
                .append("?)");

        return connection.prepareStatement(insertQuery.toString());
    }

    public static void insert
            (Connection connection, FileReader fileReader, String tableName) throws Exception {

        String[] columnsNames = fileReader.getColumnsNames();
        findColumnsMatchesErrors(columnsNames, getTableMetaData(connection, tableName));
        PreparedStatement insertStatement = getInsertStatement(connection, tableName, columnsNames);

        Savepoint savepoint = connection.setSavepoint();

        try {
            String[] parts;
            while ((parts = fileReader.getNextStringParts()) != null) {
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equalsIgnoreCase("null")) {
                        insertStatement.setString(i + 1, null);
                    } else {
                        insertStatement.setString(i + 1, parts[i]);
                    }
                }
                insertStatement.execute();
                savepoint = connection.setSavepoint();
            }
        } catch (SQLException e) {
            connection.rollback(savepoint);
            throw e;
        }
    }
}
