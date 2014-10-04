import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileProcessor {
    private Path path;
    private MyStem myStem;

    public FileProcessor(Path path) {
        this.path = path;

        try {
            myStem = new MyStem();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getAllTerms() {
        try {
            return myStem.lemmatize(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
