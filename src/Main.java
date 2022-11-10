import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    /**
     * Основной метод программы
     *
     * @param args входные аргументы: строка соединения (с пользователем), название таблицы, путь к файлу
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Неверно введены входные аргументы");
            return;
        }

        String connectionUrl = args[0];
        String tableName = args[1];
        String filePath = args[2];

        try (Connection connection = DriverManager.getConnection(connectionUrl)) {
            connection.setAutoCommit(false);

            DataFile dataFile = new DataFile(filePath);
            Table table = new Table(connection, tableName);
            ColumnsIntersection columnsIntersection = dataFile.setDesiredColumns(table.getColumns());

            if (!isContinueProgramAfterColumnsIntersection(columnsIntersection)) {
                return;
            }

            if (!tryInsert(dataFile, table, connection)) {
                connection.rollback();
                System.out.println("Изменения отменены");
            } else {
                connection.commit();
                System.out.println("Успешная вставка");
            }
        } catch (Exception e) {
            printError(e);
        }
    }

    /**
     * Попытка вставить значений в таблицу
     *
     * @param dataFile   класс, отвечающий за работу с файлом
     * @param table      класс, отвечающий за работу с таблицей
     * @param connection соединение, нужно для установки точки сохранения
     * @return true - успешная вставка, false - не успешная
     * @throws Exception ошибка при вставке
     */
    public static boolean tryInsert(DataFile dataFile, Table table, Connection connection) throws Exception {
        table.setInsertColumns(dataFile.getColumnsIntersection().getColumnsIntersection());
        Savepoint savepoint = connection.setSavepoint();

        int lineNumber = 1;
        while (true) {
            try {
                // сразу инкрементируем, чтобы не проверять ей инкремент на исключениях
                lineNumber++;

                // может возникнуть исключение, сообщающее о нарушении структуры файла
                // надо поймать исключение, спросить о нужном действии, если что пропустить строку и продолжить
                // чтобы была возможность пропустить строку ничего лучше не придумал как доставать новую внутри цикла
                if (!dataFile.next()){
                    break;
                }
                setValuesToInsert(table, getValuesFromFile(dataFile));
                table.insert();
                savepoint = connection.setSavepoint();
            } catch (Exception e) {
                printError(e);
                if (getYesNo(String.format("Произошла ошибка при вставке строки %d\n" +
                        "Откатить все вставки и закончить работу (y)\n" +
                        // lineNumber - 1, так как сразу инкрементировали
                        "Пропустить строку (n)", lineNumber - 1))) {
                    return false;
                } else {
                    connection.rollback(savepoint);
                }
            }
        }
        return true;
    }

    /**
     * Получить список значений следующей строки файла
     *
     * @param dataFile класс, отвечающий за работу с файлом
     * @return список значений
     * @throws Exception ошибка получения значений
     */
    private static List<String> getValuesFromFile(DataFile dataFile) throws Exception {
        List<String> values = new ArrayList<>();
        int valuesCount = dataFile.getColumnsIntersection().getColumnsIntersection().size();
        for (int i = 0; i < valuesCount; i++) {
            values.add(dataFile.getValue(i));
        }
        return values;
    }

    /**
     * Установить значения для вставки в таблицу
     * Значения должны соответствовать выбранным для вставки колонкам
     *
     * @param table  объект, отвечающий за вставку в таблицу
     * @param values список значений для вставки
     * @throws Exception ошибка при задании значения
     */
    private static void setValuesToInsert(Table table, List<String> values) throws Exception {
        for (int i = 0; i < values.size(); i++) {
            table.setValue(i, values.get(i));
        }
    }

    /**
     * Вывод всех не вошедших в пересечение колонок таблицы и файла
     *
     * @param columnsIntersection пересечение колонок таблицы и файла
     */
    private static void showIntersectionMissingColumns(ColumnsIntersection columnsIntersection) {
        // пересечение полностью покрывает колонки таблицы и файла, не будет предупреждений
        if (columnsIntersection.getMissingDesiredColumns().size() == 0 &&
                columnsIntersection.getMissingFileColumns().size() == 0) {
            return;
        }

        for (var column : columnsIntersection.getMissingDesiredColumns()) {
            System.out.printf("Колонка %s таблицы не содержится в файле. При вставке будет выставлен NULL\n", column);
        }
        for (var column : columnsIntersection.getMissingFileColumns()) {
            System.out.printf("Колонка %s файла не содержится в таблице При вставке будет игнорироваться\n", column);
        }
    }

    /**
     * Выяснить продолжать ли выполнение программы после пересечения
     *
     * @param columnsIntersection результат пересечения
     * @return true - продолжить выполнение программы, false - закончить выполнение программы
     */
    private static boolean isContinueProgramAfterColumnsIntersection(ColumnsIntersection columnsIntersection) {
        if (columnsIntersection.getColumnsIntersection().size() == 0) {
            System.out.println("Не было найдено соответствий колонок таблицы и файла");
            return false;
        }

        if (columnsIntersection.getMissingDesiredColumns().size() == 0 &&
                columnsIntersection.getMissingFileColumns().size() == 0) {
            return true;
        }

        showIntersectionMissingColumns(columnsIntersection);
        return getYesNo("У колонок таблицы и файла есть несоответствия. Продолжить выполнение?");
    }

    /**
     * печать ошибки
     *
     * @param e ошибка
     */
    private static void printError(Exception e) {
        System.out.printf("%s\n", e.getMessage());
    }

    /**
     * спрашиваем про откат всех данных
     *
     * @param message сообщение, которое будет выводиться
     * @return true - input Yes, false - input No
     */
    private static boolean getYesNo(String message) {
        boolean isCorrectInput = false;
        char answer = 'y';

        System.out.printf("%s\nВведите y/n\n", message);

        while (!isCorrectInput) {
            try {
                answer = Character.toLowerCase((char) System.in.read());
                isCorrectInput = answer == 'y' || answer == 'n';
            } catch (IOException e) {
                System.out.println("Произошла ошибка при вводе");
            }
        }

        return answer == 'y';
    }
}