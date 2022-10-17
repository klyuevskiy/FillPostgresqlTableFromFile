import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;

public class ColumnsAnalizator {
    private final String[] columnsNames;

    private ResultSetMetaData columnsMetaData;
    private HashMap<String, Integer> tableColumnsNames = null;

    private HashMap<Integer, String> columnsTypeNames = null;

    private String errors = null;

    public ColumnsAnalizator(ResultSetMetaData columnsMetaData, String[] columnsNames){
        this.columnsMetaData = columnsMetaData;
        this.columnsNames = columnsNames;
    }

    public HashMap<Integer, String> getColumnsTypeNames() throws SQLException {
        if (columnsTypeNames != null)
            return columnsTypeNames;

        determineInsertColumns();
        columnsTypeNames = new HashMap<Integer, String>();

        for(int i = 1; i <=  columnsMetaData.getColumnCount(); i++){
            Integer index = tableColumnsNames.get(columnsMetaData.getColumnName(i).toLowerCase());
            if (index != -1){
                columnsTypeNames.put(index, columnsMetaData.getColumnTypeName(i));
            }
        }

        return columnsTypeNames;
    }

    public String getErrors() throws SQLException {
        return errors;
    }

    public String determineInsertColumns() throws SQLException {
        if (tableColumnsNames != null)
            return errors;

        tableColumnsNames = new HashMap<String, Integer>();

        for(int i = 1; i <= columnsMetaData.getColumnCount(); i++){
            tableColumnsNames.put(columnsMetaData.getColumnName(i).toLowerCase(), -1);
        }

        errors = "";

        for (int i = 0; i < columnsNames.length; i++) {
            String lowerCaseColumnName = columnsNames[i].toLowerCase();
            var value = tableColumnsNames.get(lowerCaseColumnName);
            // такого столбца нет в таблице => ошибка
            // не будем сразу выходить, а накопим все ошибки, чтобы все сразу можно было просмотреть
            if (value == null){
                errors += "ОШИБКА: Столбца " + columnsNames[i] + " нет в таблице\n";
            }
            // такой столбец уже был => ошибка
            else if (value != -1){
                errors += "ОШИБКА: Повторное включение столбца " + columnsNames[i] + "\n";
            }
            else{
                tableColumnsNames.put(lowerCaseColumnName, i);
            }
        }

        if (errors == "")
            errors = null;

        return errors;
    }
}
