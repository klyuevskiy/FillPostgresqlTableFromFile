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
        var columnsMetaData = tableAnalizator.getColumnsMetaData();

        // не смогли получить метаданные, значит произошла ошибка
        if (columnsMetaData == null){
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

        ColumnsAnalizator columnsAnalizator = new ColumnsAnalizator(columnsMetaData, columnsNames);

        // не смогли найти соответствия
        if (!columnsAnalizator.determineInsertColumns()){
            System.out.println(columnsAnalizator.getErrors());
            fileReader.close();
            connection.close();
            return;
        }

        String insertQuery = tableAnalizator.getInsertQuery(columnsNames);
        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);

        // вставка из файла
        dataBaseInserter dataBaseInserter = new dataBaseInserter(connection, new PostgresqlParser());
        boolean hasErrors = dataBaseInserter.insertFromFile(
                insertStatement,
                fileReader,
                columnsNames.length,
                columnsAnalizator.getColumnsTypeNames());

        // если были ошибки, то спросим откатить все вставки или нет
        if (hasErrors) {
            System.out.println(dataBaseInserter.getErrors());
            if (askRollbackData()){
                connection.rollback();
                System.out.println("Изменения отменены");
            } else{
                hasErrors = false;
            }
        }

        fileReader.close();
        connection.commit();
        if (!hasErrors){
            System.out.println("Успешная вставка");
        }
        connection.close();
    }

    // спрашиваем про откат всех данных
    private static boolean askRollbackData(){
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