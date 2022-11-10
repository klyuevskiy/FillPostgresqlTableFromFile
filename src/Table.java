import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Класс таблицы базы данных.
 * Позволяет вставлять значения в таблицу.
 */
public class Table {

    private final Connection connection;
    private final String tableName;

    /**
     * Имена колонок таблицы
     */
    private final Set<String> columns = new HashSet<>();

    /**
     * запрос для вставки значений
     */
    private PreparedStatement insertStatement = null;

    private int insertColumnsCount = -1;

    /**
     * Создаёт экземпляр таблицы.
     * Собирает информацию о колонках таблицы.
     *
     * @param connection соединение с БД
     * @param tableName  название таблицы
     * @throws SQLException невозможно получить доступ к таблице
     */
    public Table(Connection connection, String tableName) throws SQLException {

        this.connection = connection;
        this.tableName = tableName;

        ResultSetMetaData metaData = getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columns.add(metaData.getColumnName(i).toLowerCase());
        }
    }

    /**
     * получить колонки таблицы
     *
     * @return Уникальное множество колонок таблицы
     */
    public Set<String> getColumns() {
        return columns;
    }

    /**
     * Установить колонки для вставки.
     *
     * @param insertColumns имена колонок для вставки
     * @throws SQLException невозможно подготовить запрос на вставку для данных колонок
     * @throws Exception    таблица не содержит заданных колонок
     */
    public void setInsertColumns(Set<String> insertColumns) throws SQLException, Exception {
        StringBuilder insertQueryStringBuilder = new StringBuilder(String.format("insert into %s (", tableName));

        for (var column : insertColumns) {
            if (!columns.contains(column.toLowerCase())) {
                throw new Exception(String.format("Таблица %s не содержит колонки %s", tableName, column));
            }
            insertQueryStringBuilder.append(String.format("%s,", column));
        }
        // delete last ,
        insertQueryStringBuilder.deleteCharAt(insertQueryStringBuilder.length() - 1);

        insertQueryStringBuilder.append(") values(");
        insertQueryStringBuilder
                .append("?,".repeat(insertColumns.size() - 1))
                .append("?)");

        insertStatement = connection.prepareStatement(insertQueryStringBuilder.toString());
        insertColumnsCount = insertColumns.size();
    }

    /**
     * Установить значение для вставки по индексу колонки
     * Значения устанавливаются для заранее заданных колонок
     *
     * @param index индекс колонки
     * @param value вставляемое значение
     * @throws IndexOutOfBoundsException индекс выходит за допустимые границы
     * @throws Exception                 не были предварительно указаны колонки для вставки
     * @see Table#setInsertColumns(Set) задать колонки для вставки
     */
    public void setValue(int index, String value) throws IndexOutOfBoundsException, Exception {
        if (insertStatement == null) {
            throw new Exception("Не указаны колонки для вставки");
        }
        if (index < 0 || index >= insertColumnsCount) {
            throw new IndexOutOfBoundsException("Индекс выходит за допустимые границы");
        }
        insertStatement.setString(index + 1, value);
    }

    /**
     * Осуществляет вставку предварительно заданных значений для заданных колонок
     *
     * @throws SQLException ошибка вставки
     * @throws Exception    не были предварительно указаны колонки для вставки
     * @see Table#setInsertColumns(Set) задать колонки для вставки
     * @see Table#setValue(int, String) задать вставляемое значение для колонки
     */
    public void insert() throws SQLException, Exception {
        if (insertStatement == null) {
            throw new Exception("Не указаны колонки для вставки");
        }
        insertStatement.execute();
    }

    /**
     * Получить метаданные таблицы
     *
     * @return метаданные
     * @throws SQLException невозможно получить метаданные таблицы
     */
    private ResultSetMetaData getMetaData() throws SQLException {
        return connection
                .createStatement()
                .executeQuery("select * from " + tableName + " where 1 = 0")
                .getMetaData();
    }
}
