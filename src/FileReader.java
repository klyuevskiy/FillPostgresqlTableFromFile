import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileReader {
    private final BufferedReader bufferedReader;
    private int columnsCount = 0;

    public FileReader(String fileName) throws FileNotFoundException {
        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));
    }

    public String[] getColumnsNames() throws IOException {
        try {
            String line = bufferedReader.readLine();
            if (line == null) {
                throw new EOFException("Файл пустой");
            }
            String[] columnsNames = line.split("\t");
            columnsCount = columnsNames.length;
            return columnsNames;
        } catch (IOException e) {
            bufferedReader.close();
            throw e;
        }
    }

    private void checkPartsLengthWithColumnsCount(int partsLength) throws Exception {
        if (partsLength < columnsCount) {
            throw new Exception(String.format(
                    "Недостаочно столбцов для вставки. " +
                            "Ожидалось %d, Имеется %d", columnsCount, partsLength
            ));
        }
        if (partsLength > columnsCount) {
            throw new Exception(String.format(
                    "Слишком много столбцов для вставки. " +
                            "Ожидалось %d, Имеется %d", columnsCount, partsLength
            ));
        }
    }

    public String[] getNextStringParts() throws Exception {
        try {
            String line = bufferedReader.readLine();
            if (line == null) {
                return null;
            }
            String[] parts = line.split("\t");
            checkPartsLengthWithColumnsCount(parts.length);
            return parts;
        } catch (Exception e) {
            bufferedReader.close();
            throw e;
        }
    }
}
