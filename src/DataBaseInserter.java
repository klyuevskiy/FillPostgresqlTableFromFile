import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;

public class DataBaseInserter {
    private final Connection connection;

    public DataBaseInserter(Connection connection){
        this.connection = connection;
    }

    public void insert(PreparedStatement insertStatement, LinesParser linesParser, int columnsCount) throws Exception {
        Object[] data;
        Savepoint savepoint = connection.setSavepoint();

        try {
            while ((data = linesParser.parseNext()) != null){
                for (int i = 1; i <= columnsCount; i++){
                    insertStatement.setObject(i, data[i - 1]);
                }
                insertStatement.execute();
                savepoint = connection.setSavepoint();
            }
        }
        catch (SQLException e){
            connection.rollback(savepoint);
            throw e;
        }
    }
}
