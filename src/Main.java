import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Main {
    private static Connection connection;
    private static String tableName;
    private static String fileName;
    private static ResultSetMetaData columnsMetaData;
    private static HashMap<String, Integer> tableColumnNames;
    private static String[] columnNames;
    public static void main(String[] args) throws SQLException, IOException {
        String connectionUrl = args[0];
        tableName = args[1];
        fileName = args[2];

        connection = DriverManager.getConnection(connectionUrl);
        connection.setAutoCommit(false);

        columnsMetaData = getTableMetaData();

        BufferedReader br = getFileReader();
        if (br == null){
            connection.close();
            return;
        }
        try {
            columnNames = br.readLine().split("\t");
        } catch (Exception e){
            System.out.println(e);
            connection.close();
            br.close();
            return;
        }

        String insertQuery = insertPrepare();
        if (insertQuery.isEmpty()){
            connection.close();
            br.close();
            return;
        }

        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);

        boolean isRollback = insertDataFromFile(insertStatement, br, columnNames.length);

        // если были ошибки, то спросим откатить все вставки или нет
        if (isRollback) {
            if (isRollbackWasErrors()){
                connection.rollback();
                System.out.println("Изменения отменены");
            } else{
                isRollback = false;
            }
        }

        br.close();
        connection.commit();
        if (!isRollback){
            System.out.println("Успешная вставка");
        }
        connection.close();
    }

    // спрашиваем про откат всех данных
    private static boolean isRollbackWasErrors() throws IOException{
        boolean isCorrectInput = false;
        char answer = 'y';

        while (!isCorrectInput){
            try{
                System.out.println("Произошли ошибки при вставке данных. Откатить ВСЕ вставки? y/n");
                answer = Character.toLowerCase((char) System.in.read());
                isCorrectInput = answer == 'y' || answer == 'n';
            } catch (IOException e){
                System.out.println("Произошла ошибка при вводе");
            }
        }

        return answer == 'y';
    }

    // подготовка к вставке, проверка, получение некторых переменных
    private static String insertPrepare() throws SQLException {
        if (columnNames.length == 0){
            System.out.println("ОШИБКА: не найдены столбцы для вставки");
            return "";
        }
        tableColumnNames = determineInsertColumns();
        if (tableColumnNames == null){
            return "";
        }
        return getInsertQuery(tableName, columnNames);
    }

    private static BufferedReader getFileReader(){
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
        } catch (Exception e) {
            System.out.println(e);
        }
        return br;
    }

    private static ResultSetMetaData getTableMetaData() throws SQLException {
        ResultSet columns = connection.createStatement().executeQuery(
                "select * from " + tableName + " where 1 = 0"
        );
        return columns.getMetaData();
    }

    private static boolean insertDataFromFile
            (PreparedStatement insertStatement,
             BufferedReader br,
             int columnsCount) throws SQLException, IOException {

        // точка сохранения, при ошибке будем откатывать один запрос
        Savepoint savepoint = connection.setSavepoint();
        String line;
        int lineNumber = 1;
        HashMap<Integer, String> columnsTypeNames = getColumnsTypeNames();
        boolean isError = false;

        while ((line = br.readLine()) != null){
            String[] parts = line.split("\t");
            // отлов исключений вставки
            try{
                insertParts(insertStatement, columnsCount, parts, lineNumber, columnsTypeNames);
                savepoint = connection.setSavepoint();
            } catch (SQLException e){
                System.out.printf("!ОШИБКА строка %d: " + e + "\n", lineNumber);
                connection.rollback(savepoint);
                isError = true;
            } catch (Exception e){
                System.out.printf("!ОШИБКА строка %d: " + e + "\n", lineNumber);
                isError = true;
            }
            lineNumber++;
        }
        return isError;
    }

    private static void insertParts
            (PreparedStatement insertStatement,
             int columnsCount,
             String[] parts,
             int lineNumber,
             HashMap<Integer, String> columnsTypeNames) throws SQLException {

        if (parts.length > columnsCount){
            System.out.printf("ПРЕДУПРЕЖДЕНИЕ строка %d: слишком большое количество столбцов. " +
                    "Лишние будут игнорироваться \n", lineNumber);
        }
        if (parts.length < columnsCount){
            System.out.printf("ПРЕДУПРЕЖДЕНИЕ строка %d: слишком маленькое количество столбцов. " +
                    "Строка будет игнорироваться \n", lineNumber);
            return;
        }

        for (int i = 0; i < columnsCount; i++){
            String part = parts[i];
            Object obj = null;
            if (!part.equalsIgnoreCase("null"))
                obj = getParsedPart(part, columnsTypeNames.get(i));
            insertStatement.setObject(i + 1, obj);
        }

        insertStatement.execute();
    }

    // генерирует Map (колонка таблицы, индекс колонки в файле). Проверяет ошибки соответсвия
    private static HashMap<String, Integer> determineInsertColumns
            () throws SQLException {
        // Map для определения соответсвий столбцов таблицы и файла
        // Map так как ключ = строка и быстродействие
        HashMap<String, Integer> tableColumnsNames = new HashMap<String, Integer>();

        for(int i = 1; i <= columnsMetaData.getColumnCount(); i++){
            tableColumnsNames.put(columnsMetaData.getColumnName(i).toLowerCase(), -1);
        }

        String errors = "";

        for (int i = 0; i < columnNames.length; i++) {
            String lowerCaseColumnName = columnNames[i].toLowerCase();
            var value = tableColumnsNames.get(lowerCaseColumnName);
            // такого столбца нет в таблице => ошибка
            // не будем сразу выходить, а накопим все ошибки, чтобы все сразу можно было просмотреть
            if (value == null){
                errors += "ОШИБКА: Столбца " + columnNames[i] + " нет в таблице\n";
            }
            // такой столбец уже был => ошибка
            else if (value != -1){
                errors += "ОШИБКА: Повторное включение столбца " + columnNames[i] + "\n";
            }
            else{
                tableColumnsNames.put(lowerCaseColumnName, i);
            }
        }

        // были ошибки, вставка невозможна
        if (!errors.isEmpty()){
            System.out.print(errors);
            return null;
        }

        return tableColumnsNames;
    }

    private static HashMap<Integer, String> getColumnsTypeNames() throws SQLException {
        HashMap<Integer, String> columnsTypeNames = new HashMap<Integer, String>();
        for(int i = 1; i <=  columnsMetaData.getColumnCount(); i++){
            Integer index = tableColumnNames.get(columnsMetaData.getColumnName(i).toLowerCase());
            if (index != -1){
                columnsTypeNames.put(index, columnsMetaData.getColumnTypeName(i));
            }
        }

        return columnsTypeNames;
    }

    private static String getInsertQuery
            (String tableName, String[] columnsNames) throws SQLException {

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

    private static Object getParsedPart(String part, String typeName){
        if (typeName.contains("int") || typeName.contains("serial"))
            return Integer.parseInt(part);
        if (typeName.contains("numeric") ||
                typeName.contains("real") || typeName.contains("decimal") ||
                typeName.contains("money"))
            return Double.parseDouble(part);
        if (typeName.contains("time"))
            return LocalDateTime.parse(part, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return part;
    }
}