import java.io.*;
import java.sql.*;

public class Main {
    public static void main(String[] args) throws SQLException{
        String connectionUrl = args[0];
        String tableName = args[1];
        String fileName = args[2];

        Connection connection = DriverManager.getConnection(connectionUrl);
        connection.setAutoCommit(false);

        TableAnalizator tableAnalizator = new TableAnalizator(connection, tableName);

        if (tableAnalizator.getColumnsMetaData() == null){
            System.out.println(tableAnalizator.getErrors());
            connection.close();
            return;
        }

        String[] columnsNames;
        String line;

        FileReader fileReader = new FileReader(fileName);

        // откырваем файл и пытаемся прочесть 1 строку, если где-то ошибка, то выход
        if (fileReader.open() == null ||
                (line = fileReader.readLine()) == null) {
            System.out.println(fileReader.getErrors());
            connection.close();
            return;
        } else {
            columnsNames = line.split("\t");
        }

        ColumnsAnalizator columnsAnalizator = new ColumnsAnalizator(tableAnalizator.getColumnsMetaData(), columnsNames);

        if (columnsAnalizator.determineInsertColumns() != null){
            System.out.println(columnsAnalizator.getErrors());
            fileReader.close();
            connection.close();
            return;
        }

        String insertQuery = tableAnalizator.getInsertQuery(columnsNames);
        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);

        StringInserter stringInserter = new StringInserter(connection, new PostgresqlParser());
        boolean isRollback = stringInserter.insertFromFile(
                insertStatement,
                fileReader,
                columnsNames.length,
                columnsAnalizator.getColumnsTypeNames());

        // если были ошибки, то спросим откатить все вставки или нет
        if (isRollback) {
            System.out.println(stringInserter.getErrors());
            if (isRollback()){
                connection.rollback();
                System.out.println("Изменения отменены");
            } else{
                isRollback = false;
            }
        }

        fileReader.close();
        connection.commit();
        if (!isRollback){
            System.out.println("Успешная вставка");
        }
        connection.close();
    }

    // спрашиваем про откат всех данных
    private static boolean isRollback(){
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
}