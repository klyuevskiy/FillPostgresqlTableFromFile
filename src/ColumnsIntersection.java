import java.util.List;
import java.util.Set;

/**
 * Класс, отвечающий за информацию о пересечении колонок файла с заданными
 */
public class ColumnsIntersection {
    /**
     * Пересечение колонок
     */
    private final Set<String> columnsIntersection;

    /**
     * Индексы колонок файла, которые попали в пересечение
     */
    private final List<Integer> fileColumnsIndexes;

    /**
     * Не попавшие в пересечение колонки из файла
     */
    private final Set<String> missingFileColumns;

    /**
     * Не попавшие в пересечение требуемые колонки
     */
    private final Set<String> missingDesiredColumns;

    /**
     * Создаёт экземпляр пересчения колонок файла с заданными
     *
     * @param columnsIntersection   пересечение имен колонок
     * @param fileColumnsIndexes    индексы колонок из персечения в файле
     * @param missingFileColumns    не вошедшие в пересечение колонки файла
     * @param missingDesiredColumns не вошедшие в пересечение колонки таблицы
     */
    public ColumnsIntersection(Set<String> columnsIntersection,
                               List<Integer> fileColumnsIndexes,
                               Set<String> missingFileColumns,
                               Set<String> missingDesiredColumns) {

        this.columnsIntersection = columnsIntersection;
        this.fileColumnsIndexes = fileColumnsIndexes;
        this.missingFileColumns = missingFileColumns;
        this.missingDesiredColumns = missingDesiredColumns;
    }

    /**
     * Возвращает перечение колонок из файла и требуемых колонок.
     *
     * @return Пересечение колнок
     */
    public Set<String> getColumnsIntersection() {
        return columnsIntersection;
    }

    /**
     * Возвращает колонки из файла, которые не попали в пересечение.
     *
     * @return Колонки из файла, которые не попали в пересечение
     */
    public Set<String> getMissingFileColumns() {
        return missingFileColumns;
    }

    /**
     * Возвращает возвращает требуемые колонки, которые не попали в пересечение.
     *
     * @return Требуемые колонки, которые не попали в пересечение
     */
    public Set<String> getMissingDesiredColumns() {
        return missingDesiredColumns;
    }

    /**
     * Возвращает ндексы колонок файла, которые попали в пересечение.
     *
     * @return индексы колонок файла, которые попали в пересечение
     */
    public List<Integer> getFileColumnsIndexes() {
        return fileColumnsIndexes;
    }
}
