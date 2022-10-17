import java.io.*;

public class FileReader {
    private String fileName;

    private String errors = null;

    BufferedReader bufferedReader = null;

    public FileReader(String fileName){
        this.fileName = fileName;
    }

    public String getErrors(){
        return errors;
    }

    public BufferedReader open() {
        if (bufferedReader != null)
            return bufferedReader;

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileName), "UTF8"));
        } catch (Exception e){
            errors = e.toString();
        }

        return bufferedReader;
    }

    public String readLine(){
        if (bufferedReader == null)
            return null;

        String res = null;

        try{
            res = bufferedReader.readLine();
        } catch (IOException e){
            errors = e.toString();
        }

        return res;
    }

    public void close(){
        if (bufferedReader == null)
            return;

        try {
            bufferedReader.close();
        } catch (IOException e) {
            errors = e.toString();
        }
    }
}