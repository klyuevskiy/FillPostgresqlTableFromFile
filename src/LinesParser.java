import java.sql.ResultSetMetaData;
import java.util.List;

public class LinesParser {

    private final List<String[]> lines;
    private final ResultSetMetaData tableMetaData;
    private final DataBaseArgumentsParser dataBaseArgumentsParser;
    private final List<Integer> columnsMatches;
    private final int columnsCount;

    private int parsedLine = 1;

    public LinesParser
            (List<String[]> lines,
             ResultSetMetaData tableMetaData,
             DataBaseArgumentsParser dataBaseArgumentsParser,
             List<Integer> columnsMatches,
             int columnsCount){

        this.lines = lines;
        this.tableMetaData = tableMetaData;
        this.dataBaseArgumentsParser = dataBaseArgumentsParser;
        this.columnsMatches = columnsMatches;
        this.columnsCount = columnsCount;
    }

    public Object[] parseNext() throws Exception {
        if (parsedLine >= lines.size())
            return null;

        String[] line = lines.get(parsedLine++);

        if (columnsCount > line.length){
            throw new Exception(String.format(
                    "ОШИБКА: недостаочно столбцов для вставки. " +
                            "Ожидалось %d, Имеется %d", columnsCount, line.length
            ));
        }

        if (columnsCount < line.length){
            throw new Exception(String.format(
                    "ОШИБКА: слишком много столбцов для вставки. " +
                            "Ожидалось %d, Имеется %d", columnsCount, line.length
            ));
        }

        Object[] data = new Object[columnsCount];

        for (int i = 0; i < columnsCount; i++){
            String part = line[i];
            data[i] = null;
            if (!part.equalsIgnoreCase("null")){
                String columnTypeName = tableMetaData.getColumnTypeName(columnsMatches.get(i));
                data[i] = dataBaseArgumentsParser.parse(part, columnTypeName);
            }
        }

        return data;
    }
}
