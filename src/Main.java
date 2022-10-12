import java.io.*;
import java.sql.*;

public class Main {
    public static void main(String[] args) throws SQLException {
        String connectionUrl = args[0];
        String tableName = args[1];
        String inputDataFileName = args[2];

        Connection connection = DriverManager.getConnection(connectionUrl);

        ResultSetMetaData columnsMetaData = getTableMetaData(connection, tableName);

        String insertQuery = getInsertQuery(tableName, columnsMetaData);
        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);

        insertDataFromFile(insertStatement, columnsMetaData, inputDataFileName);

        connection.close();
    }

    private static ResultSetMetaData getTableMetaData(Connection connection, String tableName) throws SQLException {
        ResultSet columns = connection.createStatement().executeQuery(
                "select * from " + tableName + " where 1 = 0"
        );
        return columns.getMetaData();
    }

    private static void insertDataFromFile
            (PreparedStatement insertStatement, ResultSetMetaData columnsMetaData, String fileName){
        // внешний try-catch для отлова исключений файла
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"))){
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null){
                String[] parts = line.split("\t");
                // отлов исключений вставки
                try{
                    insertParts(insertStatement, columnsMetaData, parts, lineNumber);
                } catch (SQLException e){
                    System.out.printf("!ОШИБКА строка %d: " + e + "\n", lineNumber);
                }
                lineNumber++;
            }
        } catch (Exception e){
            System.out.println(e);
        }
    }

    private static void insertParts
            (PreparedStatement insertStatement, ResultSetMetaData columnsMetaData, String[] parts, int lineNumber) throws SQLException {
        if (parts.length > columnsMetaData.getColumnCount())
            System.out.printf("ПРЕДУПРЕЖДЕНИЕ строка %d: в строке больше значений, чем колонок в таблице. " +
                    "Избыточные значения будут проигнорированы\n", lineNumber);
        if (parts.length < columnsMetaData.getColumnCount())
            System.out.printf("ПРЕДУПРЕЖДЕНИЕ строка %d: в строке меньше значений, чем колонок в таблице. " +
                    "Недостающие значения будут заполнены NULL\n", lineNumber);

        for (int i = 1; i <= columnsMetaData.getColumnCount() && i <= parts.length; i++){
            String part = parts[i - 1];
            Object obj = null;
            if (!part.equalsIgnoreCase("null"))
                obj = getParsedPart(part, columnsMetaData.getColumnTypeName(i));
            insertStatement.setObject(i, obj);
        }

        for (int i = parts.length + 1; i <= columnsMetaData.getColumnCount(); i++){
            insertStatement.setObject(i, null);
        }

        insertStatement.execute();
    }

    private static String getInsertQuery(String tableName, ResultSetMetaData columnsMetaData) throws SQLException {
        String insertQuery = "insert into " + tableName + " values(";
        for (int i = 1; i < columnsMetaData.getColumnCount(); i++){
            insertQuery += "?, ";
        }
        return insertQuery + "?)";
    }

    private static Object getParsedPart(String part, String typeName){
        if (typeName.contains("int") || typeName.contains("serial"))
            return Integer.parseInt(part);
        if (typeName.contains("numeric") || typeName.contains("real") || typeName.contains("decimal"))
            return Double.parseDouble(part);
        return part;
    }
}