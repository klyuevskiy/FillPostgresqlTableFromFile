import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class MetaDataGetter {
    private final Connection connection;

    public MetaDataGetter(Connection connection){

        this.connection = connection;
    }

    public ResultSetMetaData getMetaData(String tableName) throws SQLException {
        return connection
                .createStatement()
                .executeQuery("select * from " + tableName + " where 1 = 0")
                .getMetaData();
    }
}
