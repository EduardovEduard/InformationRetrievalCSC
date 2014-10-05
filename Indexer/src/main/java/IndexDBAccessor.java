import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class IndexDBAccessor implements IndexBackend {
    private Path file;
    private Connection connection;

    private static final String DRIVER_NAME = "org.sqlite.JDBC";

    private static final String ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys = ON;";

    private static final String CREATE_DOCUMENTS = "CREATE TABLE documents(id INTEGER PRIMARY KEY NOT NULL, file TEXT);";
    private static final String CREATE_TERMS = "CREATE TABLE terms(id INTEGER PRIMARY KEY NOT NULL, term TEXT);";
    private static final String CREATE_TERM_DOCUMENT =
            "CREATE TABLE term_document(id INTEGER PRIMARY KEY, term_id, document_id documents, " +
                    "FOREIGN KEY (term_id) REFERENCES terms(id)," +
                    "FOREIGN KEY (document_id) REFERENCES documents(id));";

    private static final String CREATE_INDEX_TERMS = "CREATE INDEX IF NOT EXISTS term_index ON terms(term);";
    private static final String CREATE_INDEX_DOCUMENTS = "CREATE INDEX IF NOT EXISTS documents_index ON documents(file);";

    private static final String ADD_DOCUMENT = "INSERT INTO documents (file) VALUES (?);";
    private static final String ADD_TERM = "INSERT INTO terms (term) VALUES (?);";
    private static final String ADD_TERM_DOCUMENT = "INSERT INTO term_document (term_id, document_id) VALUES (?, ?);";

    private static final String SELECT_TERM = "SELECT id FROM terms WHERE term = ?;";
    private static final String SELECT_DOCUMENT = "SELECT id FROM documents WHERE file = ?;";

    private static final String GET_DOCUMENTS = "select file from " +
            "documents join (select document_id from term_document where term_id = " +
            "(select id from terms where term = ?)) t " +
            "on documents.id = t.document_id;";

    private long lastTermId = 0;
    private long lastDocumentId = 0;

    private PreparedStatement precompiledAddDocument = null;
    private PreparedStatement precompiledAddTerm = null;
    private PreparedStatement precompiledAddTermDocument = null;
    private PreparedStatement precompiledSelectTerm = null;
    private PreparedStatement precompiledSelectDocument = null;
    private PreparedStatement precompiledGetDocuments = null;

    public IndexDBAccessor(Path file) {
        try {
            this.file = file;
            createFile(file);

            final String connectionString = "jdbc:sqlite:" + file.toString();
            Class.forName(DRIVER_NAME);
            connection = DriverManager.getConnection(connectionString);
            prepareDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void createFile(Path file) throws IOException {
        try {
            if (Files.exists(file)) {
                Files.delete(file);
            }

            Files.createFile(file);
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    private void prepareDB() {
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA synchronous = OFF");
            stmt.execute("PRAGMA journal_mode = MEMORY");
            stmt.executeUpdate(ENABLE_FOREIGN_KEYS);
            stmt.executeUpdate(CREATE_DOCUMENTS);
            stmt.executeUpdate(CREATE_TERMS);
            stmt.executeUpdate(CREATE_TERM_DOCUMENT);

            precompiledAddDocument = connection.prepareStatement(ADD_DOCUMENT);
            precompiledAddTerm = connection.prepareStatement(ADD_TERM);
            precompiledAddTermDocument = connection.prepareStatement(ADD_TERM_DOCUMENT);
            precompiledGetDocuments = connection.prepareStatement(GET_DOCUMENTS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized long addDocument(String document) {
        try {
            precompiledAddDocument.setString(1, document);
            precompiledAddDocument.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ++lastDocumentId;
    }

    public synchronized long addTerm(String term) {
        try {
            precompiledAddTerm.setString(1, term);
            precompiledAddTerm.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ++lastTermId;
    }

    public synchronized void addTermDocument(long term, long document) throws RuntimeException {
        try {
            precompiledAddTermDocument.setLong(1, term);
            precompiledAddTermDocument.setLong(2, document);
            precompiledAddTermDocument.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("BEGIN TRANSACTION;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getDocuments(String term) {
        List<String> documents = new ArrayList<String>();

        try {
            precompiledGetDocuments.setString(1, term);
            ResultSet result = precompiledGetDocuments.executeQuery();

            while (result.next()) {
                documents.add(result.getString(1));
            }
        } catch ( SQLException e) {
            e.printStackTrace();
        }

        return documents;
     }

    public void finish() {
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("END TRANSACTION;");

            stmt.executeUpdate(CREATE_INDEX_TERMS);
            stmt.executeUpdate(CREATE_INDEX_DOCUMENTS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        IndexDBAccessor dumper = new IndexDBAccessor(Paths.get("/home/ees/index.ind"));
    }
}
