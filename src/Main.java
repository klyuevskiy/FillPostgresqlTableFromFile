import java.io.*;
import java.sql.*;

public class Main {
    public static void main(String[] args){
        String connectionUrl = args[0];
        String tableName = args[1];
        String fileName = args[2];

        try(Connection connection = DriverManager.getConnection(connectionUrl)){
            connection.setAutoCommit(false);

            var lines = new FileReader(fileName).read();
            if (lines.isEmpty())
                throw new Exception("Файл с данными пустой");

            String[] columnsNames = lines.get(0);
            int columnsCount = columnsNames.length;

            var metaData = new MetaDataGetter(connection).getMetaData(tableName);
            var columnsMatches = new MatchesFinder().findMatches(metaData, columnsNames);
            PreparedStatement insertStatement = new InsertStatementBuilder(connection).getInsertStatement(tableName, columnsNames);

            LinesParser linesParser = new LinesParser(lines, metaData, new PostgresqlParser(), columnsMatches, columnsCount);

            try{
                new DataBaseInserter(connection).insert(insertStatement, linesParser, columnsCount);
            } catch (Exception e){
                System.out.printf("ОШИБКА: %s\n", e.getMessage());
                if (askRollbackData(connection)){
                    System.out.println("Изменения отменены");
                    return;
                }
            }

            connection.commit();
            System.out.println("Успешная вставка");

        } catch (Exception e) {
            System.out.printf("ОШИБКА: %s\n", e.getMessage());
        }
    }

    private static boolean askRollbackData(Connection connection) throws SQLException {
        boolean isRollback = getUserAnswerRollback();
        if (isRollback)
            connection.rollback();
        return isRollback;
    }

    // спрашиваем про откат всех данных
    private static boolean getUserAnswerRollback(){
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