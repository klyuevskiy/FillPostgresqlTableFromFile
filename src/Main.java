import java.io.*;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        String connectionUrl = args[0];
        String tableName = args[1];
        String fileName = args[2];

        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            connection.setAutoCommit(false);

            try {
                new DataBaseInserter(connection, tableName).insert(new FileReader(fileName));
            } catch (Exception e) {
                printError(e);
                if (askRollbackData(connection)) {
                    System.out.println("Изменения отменены");
                    return;
                }
            }

            connection.commit();
            System.out.println("Успешная вставка");

        } catch (Exception e) {
            printError(e);
        }
    }

    private static void printError(Exception e) {
        System.out.printf("%s\n", e.getMessage());
    }

    private static boolean askRollbackData(Connection connection) throws SQLException {
        boolean isRollback = getUserAnswerRollback();
        if (isRollback)
            connection.rollback();
        return isRollback;
    }

    // спрашиваем про откат всех данных
    private static boolean getUserAnswerRollback() {
        boolean isCorrectInput = false;
        char answer = 'y';

        while (!isCorrectInput) {
            try {
                System.out.println("Произошли ошибки при вставке данных. Откатить ВСЕ вставки? y/n");
                answer = Character.toLowerCase((char) System.in.read());
                isCorrectInput = answer == 'y' || answer == 'n';
            } catch (IOException e) {
                System.out.println("Произошла ошибка при вводе");
            }
        }

        return answer == 'y';
    }
}