import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class MatchesFinder {

    private final String[] columnsNames;
    private final ResultSetMetaData tableMetaData;
    private final List<Integer> indexesOfInsertFileColumns;

    public MatchesFinder(String[] columnsNames, ResultSetMetaData tableMetaData) throws SQLException {

        this.columnsNames = columnsNames;
        this.tableMetaData = tableMetaData;
        indexesOfInsertFileColumns = getIndexesOfInsertFileColumnsAndPrintInconsistencies();
    }

    public List<String> getInsertColumnsNames() {
        List<String> insertColumnsNames = new ArrayList<>(indexesOfInsertFileColumns.size());
        for (var i : indexesOfInsertFileColumns) {
            insertColumnsNames.add(columnsNames[i]);
        }
        return insertColumnsNames;
    }

    public List<String> buildInsertParts(String[] lineParts) {
        List<String> insertParts = new ArrayList<>(indexesOfInsertFileColumns.size());
        for (var i : indexesOfInsertFileColumns) {
            insertParts.add(lineParts[i]);
        }
        return insertParts;
    }

    private List<Integer> getIndexesOfInsertFileColumnsAndPrintInconsistencies() throws SQLException {
        List<Integer> indexesOfInsertFileColumns = new ArrayList<>();

        // колонки, которые есть в таблице, также будем отмечать встретили мы её или нет
        // для того, чтобы вывести предупреждение, что они будут NULL
        Map<String, Boolean> tableColumnsNames = new HashMap<>();
        // уже встреченные в файле колонки
        Set<String> fileColumnsNames = new HashSet<>();

        for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
            tableColumnsNames.put(tableMetaData.getColumnName(i).toLowerCase(), false);
        }

        for (int i = 0; i < columnsNames.length; i++) {
            var columnNameLowerCase = columnsNames[i].toLowerCase();

            if (tableColumnsNames.get(columnNameLowerCase) == null) {
                System.out.printf("Столбца %s нет в таблице. Он вставляться не будет\n", columnsNames[i]);
            } else if (fileColumnsNames.contains(columnNameLowerCase)) {
                System.out.printf("Столбец %s повторяется несколько раз. Повторные включения будут игнорироваться\n", columnsNames[i]);
            } else {
                // всё хорошо: не встречался ранее, есть в таблице, вставляем
                fileColumnsNames.add(columnNameLowerCase);
                indexesOfInsertFileColumns.add(i);
                tableColumnsNames.put(columnNameLowerCase, true);
            }
        }

        for(var tableColumnName : tableColumnsNames.entrySet()){
            if (!tableColumnName.getValue()){
                System.out.printf("Столбца %s нет в файле, но есть в таблице. Будет вставлен NULL\n", tableColumnName.getKey());
            }
        }

        return indexesOfInsertFileColumns;
    }
}
