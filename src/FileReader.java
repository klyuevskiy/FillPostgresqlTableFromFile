import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileReader {
    private final String fileName;

    public FileReader(String fileName){
        this.fileName = fileName;
    }

    public ArrayList<String[]> read() throws IOException {
        ArrayList<String[]> lines = new ArrayList<>();

        try(var bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line.split("\t"));
            }
        }
        return lines;
    }
}
