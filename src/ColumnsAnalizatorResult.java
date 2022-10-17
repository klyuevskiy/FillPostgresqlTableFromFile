import java.sql.ResultSetMetaData;
import java.util.HashMap;

public class ColumnsAnalizatorResult {
    private final ResultSetMetaData columnsMetaData;
    private final HashMap<Integer, String> columnsTypes;
    private final String errors;

    ColumnsAnalizatorResult(ResultSetMetaData columnsMetaData,
                            HashMap<Integer, String> columnsTypes,
                            String errors){

        this.columnsMetaData = columnsMetaData;
        this.columnsTypes = columnsTypes;
        this.errors = errors;
    }

    public ResultSetMetaData getColumnsMetaData(){
        return columnsMetaData;
    }

    public HashMap<Integer, String> getColumnsTypes(){
        return columnsTypes;
    }

    public String getErrors(){
        return errors;
    }
}
