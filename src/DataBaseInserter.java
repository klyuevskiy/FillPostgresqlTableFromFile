import java.sql.*;
import java.util.List;

public class DataBaseInserter {

    private final Connection connection;
    private final String tableName;

    public DataBaseInserter(Connection connection, String tableName) {

        this.connection = connection;
        this.tableName = tableName;
    }

    private ResultSetMetaData getTableMetaData() throws SQLException {
        return connection
                .createStatement()
                .executeQuery("select * from " + tableName + " where 1 = 0")
                .getMetaData();
    }

    private PreparedStatement getInsertStatement(List<String> columnsNames) throws SQLException {
        StringBuilder insertQueryStringBuilder = new StringBuilder(String.format("insert into %s (%s", tableName, columnsNames.get(0)));

        for (int i = 1; i < columnsNames.size(); i++) {
            insertQueryStringBuilder.append(String.format(", %s", columnsNames.get(i)));
        }

        insertQueryStringBuilder.append(") values(");

        insertQueryStringBuilder
                .append("?, ".repeat(columnsNames.size() - 1))
                .append("?)");

        return connection.prepareStatement(insertQueryStringBuilder.toString());
    }

    public void insert(FileReader fileReader) throws Exception {

        MatchesFinder matchesFinder = new MatchesFinder(fileReader.getColumnsNames(), getTableMetaData());
        PreparedStatement insertStatement = getInsertStatement(matchesFinder.getInsertColumnsNames());

        Savepoint savepoint = connection.setSavepoint();

        try {
            String[] parts;
            while ((parts = fileReader.getNextStringParts()) != null) {
                List<String> insertParts = matchesFinder.buildInsertParts(parts);
                for (int i = 0; i < insertParts.size(); i++) {
                    if (insertParts.get(i).equalsIgnoreCase("null")) {
                        insertStatement.setString(i + 1, null);
                    } else {
                        insertStatement.setString(i + 1, insertParts.get(i));
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
