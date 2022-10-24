import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MatchesFinder {
    // возвразвращает для каждой колонки в файле индекс колонки в таблице
    // индекс
    public ArrayList<Integer> findMatches(ResultSetMetaData tableMetaData, String[] columnsNames) throws Exception {

        // индексы колонок в таблице по её названию
        HashMap<String, Integer> tableIndexes = new HashMap<>();
        // уже встреченные в файле колонки
        HashSet<String> fileColumnNames = new HashSet<>();
        ArrayList<Integer> result = new ArrayList<>(columnsNames.length);

        for (int i = 1; i <= tableMetaData.getColumnCount(); i++){
            tableIndexes.put(tableMetaData.getColumnName(i), i);
        }

        for (var columnName : columnsNames){
            var columnNameLowerCase = columnName.toLowerCase();
            Integer tableIndex = tableIndexes.get(columnNameLowerCase);

            // нет в таблцице
            if (tableIndex == null)
                throw new Exception(String.format("Столбца %s нет в таблице", columnName));

            // уже встречали такой столбец
            if(fileColumnNames.contains(columnNameLowerCase))
                throw new Exception(String.format("Столбец %s повторяется несколько раз", columnName));

            // всё хорошо: не встречался ранее и есть в таблице
            fileColumnNames.add(columnNameLowerCase);
            result.add(tableIndex);
        }

        return result;
    }
}
