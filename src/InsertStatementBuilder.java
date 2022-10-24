import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertStatementBuilder {
    private final Connection connection;

    public InsertStatementBuilder(Connection connection){

        this.connection = connection;
    }

    public PreparedStatement getInsertStatement(String tableName, String[] columnsNames) throws SQLException {
        StringBuilder insertQuery = new StringBuilder(String.format("insert into %s (%s", tableName, columnsNames[0]));

        for (int i = 1; i < columnsNames.length; i++){
            insertQuery.append(String.format(", %s", columnsNames[i]));
        }

        insertQuery.append(") values(");

        insertQuery.append("?, ".repeat(columnsNames.length - 1));
        insertQuery.append("?)");

        return connection.prepareStatement(insertQuery.toString());
    }
}
