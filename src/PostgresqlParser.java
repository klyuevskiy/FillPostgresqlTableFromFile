import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PostgresqlParser implements DataBaseArgumentsParser{
    @Override
    public Object parse(String argument, String argumentType) {
        if (argumentType.contains("int") || argumentType.contains("serial"))
            return Integer.parseInt(argument);
        if (argumentType.contains("numeric") ||
                argumentType.contains("real") || argumentType.contains("decimal") ||
                argumentType.contains("money"))
            return Double.parseDouble(argument);
        if (argumentType.contains("time"))
            return LocalDateTime.parse(argument, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return argument;
    }
}
