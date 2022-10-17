import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class TableAnalizator {
    private final Connection connection;
    private final String tableName;

    ResultSetMetaData columnsMetaData = null;

    private String errors = null;

    public TableAnalizator(Connection connection, String tableName){

        this.connection = connection;
        this.tableName = tableName;
    }

    public String getErrors(){
        return errors;
    }

    public ResultSetMetaData getColumnsMetaData(){
        if (columnsMetaData != null)
            return columnsMetaData;

        try{
            ResultSet columns = connection.createStatement().executeQuery(
                    "select * from " + tableName + " where 1 = 0"
            );
            columnsMetaData = columns.getMetaData();
        } catch (SQLException e){
            errors = e.toString();
        }
        return columnsMetaData;
    }

    public String getInsertQuery(String[] columnsNames){
        String insertQuery = "insert into " + tableName + "(" + columnsNames[0];

        for (int i = 1; i < columnsNames.length; i++){
            insertQuery += ", ";
            insertQuery += columnsNames[i];
        }

        insertQuery += ") values(";

        for (int i = 1; i < columnsNames.length; i++){
            insertQuery += "?, ";
        }
        return insertQuery + "?)";
    }
}
