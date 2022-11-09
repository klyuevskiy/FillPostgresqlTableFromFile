import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
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
     * @throws SQLException невозоможно получить доступ к таблице
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
     * Вставка в таблицу
     *
     * @param values список значений для вставки, соотвествующий заранее указанным колонкам
     * @throws SQLException              ошибка вставки
     * @throws InvalidParameterException размер списка параметров не соответствует количеству вствляемых колонок
     * @throws Exception                 не были предварительно указаны колонки для вставки
     * @see Table#setInsertColumns(Set) задать колонки для вставки
     */
    public void insert(List<String> values) throws SQLException, InvalidParameterException, Exception {
        if (insertStatement == null) {
            throw new Exception("Не указаны колонки для вставки");
        }
        if (insertColumnsCount != values.size()) {
            throw new InvalidParameterException("Размер списка данных не соотвествует количеству вставляемых колонок");
        }
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase("null")){
                insertStatement.setString(i + 1, null);
            } else{
                insertStatement.setString(i + 1, values.get(i));
            }
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
