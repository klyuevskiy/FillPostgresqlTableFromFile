import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;

public class dataBaseInserter {
    private final Connection connection;
    private final DataBaseArgumentsParser parser;

    private String errors = null;

    public dataBaseInserter(Connection connection, DataBaseArgumentsParser parser){
        this.connection = connection;
        this.parser = parser;
    }

    public String getErrors() {
        return errors;
    }

    public boolean insertFromFile
            (PreparedStatement insertStatement,
             FileReader fileReader,
             int columnsCount,
             HashMap<Integer, String> columnsTypeNames) throws SQLException {

        errors = "";

        // точка сохранения, при ошибке будем откатывать один запрос
        Savepoint savepoint = connection.setSavepoint();
        String line;
        int lineNumber = 1;
        boolean isError = false;

        while ((line = fileReader.readLine()) != null && !isError){
            String[] parts = line.split("\t");
            // отлов исключений вставки
            try{
                insertParts(insertStatement, columnsCount, parts, columnsTypeNames);
                savepoint = connection.setSavepoint();
            } catch (SQLException e){
                errors += String.format("ОШИБКА строка %d: " + e + "\n", lineNumber);
                connection.rollback(savepoint);
                isError = true;
            } catch (Exception e){
                errors += String.format("ОШИБКА строка %d: " + e + "\n", lineNumber);
                isError = true;
            }
            lineNumber++;
        }
        if (errors.isEmpty())
            errors = null;

        return isError;
    }

    private void insertParts
            (PreparedStatement insertStatement,
             int columnsCount,
             String[] parts,
             HashMap<Integer, String> columnsTypeNames) throws Exception {

        // нарушение структуры файла
        if (parts.length > columnsCount){
            throw new Exception("Cлишком большое количество столбцов.");
        }
        if (parts.length < columnsCount){
            throw new Exception("Слишком маленькое количество столбцов.");
        }

        for (int i = 0; i < columnsCount; i++){
            String part = parts[i];
            Object obj = null;
            if (!part.equalsIgnoreCase("null"))
                obj = parser.parse(part, columnsTypeNames.get(i));
            insertStatement.setObject(i + 1, obj);
        }

        insertStatement.execute();
    }
}
