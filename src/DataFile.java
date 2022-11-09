import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Класс, который читает данные из файла.
 * Можно задать имена колонок, которые можно получить.
 */
public class DataFile {

    private final BufferedReader file;

    /**
     * Колонки файла
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
     * Создаёт экземпляр класса и открывает файл для чтения.
     *
     * @param filePath путь к файлу
     * @throws FileNotFoundException файл не найден
     */
    public DataFile(String filePath) throws FileNotFoundException {
        file = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
    }

    /**
     * Получает колонки, которые есть в файле.
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
     * Считать следующую строку и узнать её наличие.
     *
     * @return true - есть ли следующая строка в файле. false - нет строк
     * @throws IOException ошибка чтения файла
     * @throws Exception   нарушение формата файла (некорректное количество колонок на новой строке)
     */
    public boolean next() throws IOException, Exception {
        // если ещё не читали имена колонок, то считаем
        if (fileColumns == null) {
            getFileColumns();
        }
        try {
            lastLine = file.readLine().split("\t");
            checkLineColumnsCount(lastLine.length);
            if (lastLine == null) {
                file.close();
                return false;
            }
            return true;
        } catch (Exception e) {
            file.close();
            throw e;
        }
    }

    /**
     * Закрывает файл.
     *
     * @throws IOException - ошибка закрытия файла
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
     * @throws ArrayIndexOutOfBoundsException индекс выходит за допустимые пределы
     * @throws Exception                      не был вызван next
     * @see DataFile#next() получить следующую строку
     */
    public String getValue(int index) throws ArrayIndexOutOfBoundsException, Exception {
        if (lastLine == null) {
            throw new Exception("next не был вызван");
        }
        if (columnsIntersection == null) {
            // не строили перечение, значит просто берём значение из файла
            if (index < 0 || index >= fileColumns.length) {
                throw new ArrayIndexOutOfBoundsException("Недопустимый индекс колонки");
            }
            return fileColumns[index];
        } else {
            if (index < 0 || index >= columnsIntersection.getFileColumnsIndexes().size()) {
                throw new ArrayIndexOutOfBoundsException("Недопустимый индекс колонки");
            }
            return fileColumns[columnsIntersection.getFileColumnsIndexes().get(index)];
        }
    }

    /**
     * Возвращает пересечение колонок из файла и требуемых
     *
     * @return Пересечение колонок из файла и требуемых
     * @see DataFile#setDesiredColumns(Set) задать требуемые колонки
     */
    public ColumnsIntersection getColumnsIntersect() {
        return columnsIntersection;
    }

    /**
     * Проверяет, что колонки в файле не повтрояются.
     *
     * @param columns массив имён колонок
     * @throws Exception имена колонок повторяются
     */
    private void checkNotRepeatColumns(String[] columns) throws Exception {
        Set<String> meetColumns = new HashSet<>();

        for (var column : columns) {
            String lowerCaseColumn = column.toLowerCase();
            if (meetColumns.contains(lowerCaseColumn)) {
                throw new Exception(String.format("Колонка %s встречается несколько раз", column));
            }
            meetColumns.add(lowerCaseColumn);
        }
    }

    /**
     * Проверяет что количество колонок на новой строке совпадает с начально заданым количеством колонок
     *
     * @param lineColumnsCount количество колонок
     * @throws Exception количество в строке не соотвествует количеству колонок в файле
     */
    private void checkLineColumnsCount(int lineColumnsCount) throws Exception {
        if (lineColumnsCount < fileColumns.length) {
            throw new Exception(String.format(
                    "Недостаочно столбцов для вставки. " +
                            "Ожидалось %d, Имеется %d", fileColumns.length, lineColumnsCount
            ));
        }
        if (lineColumnsCount > fileColumns.length) {
            throw new Exception(String.format(
                    "Слишком много столбцов для вставки. " +
                            "Ожидалось %d, Имеется %d", fileColumns.length, lineColumnsCount
            ));
        }
    }

    /**
     * Установить желаемые для извлечения из файла колонки.
     * Строится пересечение с колонками из файла.
     * Возвращает пересечение.
     * Если до этого не разу не были получены имена колок файла, то они будут считаны.
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

        // перечечение и пропущенные колонки файла и требуемых
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
