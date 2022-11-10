import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Класс, который читает данные из файла.
 * Можно задать имена колонок, которые можно получить.
 * Будет строиться пересечение колонок файла и тех, которые желаете получить
 */
public class DataFile {

    private final BufferedReader file;

    /**
     * Колонки файла.
     * Первая строка файла
     */
    private String[] fileColumns = null;

    /**
     * Прошлая считанная строка
     */
    private String[] lastLine = null;

    /**
     * Пересечение колонок из файла и требуемых колонок
     */
    private ColumnsIntersection columnsIntersection = null;


    /**
     * Создаёт экземпляр класса, открывает файл для чтения.
     *
     * @param filePath путь к файлу
     * @throws FileNotFoundException файл не найден
     */
    public DataFile(String filePath) throws FileNotFoundException {
        file = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
    }

    /**
     * Даёт колонки, которые есть в файле.
     *
     * @return массив с именами колонок файла
     * @throws IOException ошибка чтения файла
     * @throws Exception   имена колонок в файле повторяются
     */
    public String[] getFileColumns() throws IOException, Exception {
        if (fileColumns != null) {
            return fileColumns;
        }
        try {
            String line = file.readLine();
            fileColumns = line.split("\t");
            checkNotRepeatColumns(fileColumns);
            return fileColumns;
        } catch (Exception e) {
            file.close();
            throw e;
        }
    }

    /**
     * Считать следующую строку и узнать о её наличие.
     * Если до этого ни разу не были получены имена колок файла, то они будут считаны.
     *
     * @return true - есть ли следующая строка в файле, false - нет строк
     * @throws IOException ошибка чтения файла
     * @throws Exception   нарушение формата файла (некорректное количество колонок на новой строке)
     */
    public boolean next() throws IOException, Exception {
        // если ещё не читали имена колонок, то считаем
        if (fileColumns == null) {
            getFileColumns();
        }
        try {
            String line = file.readLine();
            if (line == null) {
                file.close();
                return false;
            }
            lastLine = line.split("\t");
            checkLineColumnsCount(lastLine.length);
            return true;
        } catch (IOException e) {
            // ошибка чтения файла, сразу закрываем
            file.close();
            throw e;
        }
    }

    /**
     * Закрывает файл.
     *
     * @throws IOException ошибка закрытия файла
     */
    public void close() throws IOException {
        file.close();
    }

    /**
     * Получить значение колонки index в текущей строке.
     * Если было построено пересечение, то требуется индекс колонки пересечения, иначе индекс колонки в файле.
     *
     * @param index индекс нужной колонки
     * @return Значение колонки в текущей строке index
     * @throws IndexOutOfBoundsException индекс выходит за допустимые пределы
     * @throws Exception                 не был вызван next
     * @see DataFile#next() получить следующую строку
     */
    public String getValue(int index) throws IndexOutOfBoundsException, Exception {
        if (lastLine == null) {
            throw new Exception("next не был вызван");
        }
        if (columnsIntersection == null) {
            // не строили пересечение, значит просто берём значение из файла
            if (index < 0 || index >= fileColumns.length) {
                throw new IndexOutOfBoundsException("Недопустимый индекс колонки");
            }
        } else {
            if (index < 0 || index >= columnsIntersection.getFileColumnsIndexes().size()) {
                throw new IndexOutOfBoundsException("Недопустимый индекс колонки");
            }
            index = columnsIntersection.getFileColumnsIndexes().get(index);
        }

        if (lastLine[index].equalsIgnoreCase("null")) {
            return null;
        }
        return lastLine[index];
    }

    /**
     * Возвращает пересечение колонок из файла и требуемых
     *
     * @return Пересечение колонок из файла и требуемых
     * @see DataFile#setDesiredColumns(Set) задать требуемые колонки
     */
    public ColumnsIntersection getColumnsIntersection() {
        return columnsIntersection;
    }

    /**
     * Проверяет, что колонки не повторяются.
     *
     * @param columns массив имён колонок
     * @throws Exception имена колонок повторяются
     */
    private void checkNotRepeatColumns(String[] columns) throws Exception {
        Set<String> meetColumns = new HashSet<>();

        for (var column : columns) {
            String lowerCaseColumn = column.toLowerCase();
            if (meetColumns.contains(lowerCaseColumn)) {
                throw new Exception(String.format("Колонка %s встречается несколько раз в файле", column));
            }
            meetColumns.add(lowerCaseColumn);
        }
    }

    /**
     * Проверяет что количество колонок на новой строке совпадает с начально заданным количеством колонок
     *
     * @param lineColumnsCount количество колонок
     * @throws Exception количество в строке не соответствует количеству колонок в файле
     */
    private void checkLineColumnsCount(int lineColumnsCount) throws Exception {
        if (lineColumnsCount < fileColumns.length) {
            throw new Exception(String.format(
                    "Слишком мало столбцов в строке. " +
                            "Должно быть %d, Имеется %d", fileColumns.length, lineColumnsCount
            ));
        }
        if (lineColumnsCount > fileColumns.length) {
            throw new Exception(String.format(
                    "Слишком много столбцов в строке. " +
                            "Должно быть %d, Имеется %d", fileColumns.length, lineColumnsCount
            ));
        }
    }

    /**
     * Установить желаемые для извлечения из файла колонки.
     * Строится пересечение с колонками из файла.
     * Возвращает пересечение.
     * Если до этого ни разу не были получены имена колок файла, то они будут считаны.
     *
     * @param desiredColumns требуемые для получения из файла колонки
     * @return Пересечение колонок
     * @throws IOException ошибка чтения имён колонок файла
     * @throws Exception   повторение имён колонок в файле
     */
    public ColumnsIntersection setDesiredColumns(Set<String> desiredColumns) throws IOException, Exception {
        // не брали ещё колонки из файла, возьмём
        if (fileColumns == null) {
            getFileColumns();
        }

        // пересечение и пропущенные колонки файла и требуемых
        List<Integer> fileColumnsIndexes = new ArrayList<>();
        Set<String> columnsIntersect = new HashSet<>();
        Set<String> missingFileColumns = new HashSet<>();
        Set<String> missingDesiredColumns = new HashSet<>();

        // проходим по колонкам файла и строим пересечение
        for (int i = 0; i < fileColumns.length; i++) {
            var fileColumnLowerCase = fileColumns[i].toLowerCase();

            // нет такой колонки в нужных для выбора, отметим
            if (!desiredColumns.contains(fileColumnLowerCase)) {
                missingFileColumns.add(fileColumns[i]);
            } else {
                // добавим в пересечение
                fileColumnsIndexes.add(i);
                columnsIntersect.add(fileColumnLowerCase);
            }
        }

        // возьмём все нужные колонки, но которых нет в файле
        for (var column : desiredColumns) {
            if (!columnsIntersect.contains(column.toLowerCase())) {
                missingDesiredColumns.add(column);
            }
        }

        this.columnsIntersection = new ColumnsIntersection(columnsIntersect, fileColumnsIndexes, missingFileColumns, missingDesiredColumns);
        return this.columnsIntersection;
    }
}
