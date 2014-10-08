package ru.ees.Indexer;

import ru.ees.Indexer.exceptions.IncorrectQueryException;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final String GET_DOCUMENTS = "select documents.id from " +
            "documents join (select document_id from term_document where term_id = " +
            "(select id from terms where term = ?)) t " +
            "on documents.id = t.document_id ";

    static Map<String, String> operators = new HashMap<>();
    static {
        operators.put("OR", " UNION ");
        operators.put("AND", " INTERSECT ");
    }

    private Map<String, Long> terms = new ConcurrentHashMap<>();
    private Map<String, Long> documents = new ConcurrentHashMap<>();

    private long lastTermId = 0;
    private long lastDocumentId = 0;

    private PreparedStatement precompiledAddDocument = null;
    private PreparedStatement precompiledAddTerm = null;
    private PreparedStatement precompiledAddTermDocument = null;
    private PreparedStatement precompiledSelectTerm = null;
    private PreparedStatement precompiledSelectDocument = null;
    private PreparedStatement precompiledGetDocuments = null;

    @Override
    public List<String> processQuery(String query) throws IncorrectQueryException {
        QueryProcessor processor = new QueryProcessor(query);
        List<String> result = processor.process(query);
        return result;
    }

    public static IndexDBAccessor use(Path path) {
        IndexDBAccessor accessor = new IndexDBAccessor(path, true);
        return accessor;
    }

    public IndexDBAccessor() {
    }

    //TODO Убрать костылища
    public IndexDBAccessor(Path file, boolean use) {
        try {
            this.file = file;

            if (!use)
                createFile(file);

            final String connectionString = "jdbc:sqlite:" + file.toString();
            Class.forName(DRIVER_NAME);
            connection = DriverManager.getConnection(connectionString);

            if (!use)
                prepareDB();

            prepareStatements();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void prepareStatements() {
        try {
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

    public synchronized void addTermDocument(String term, String document) throws RuntimeException {
        if  (!documents.containsKey(document)) {
            long documentId = addDocument(document);
            documents.put(document, documentId);
        }

        if (!terms.containsKey(term)) {
            long termId = addTerm(term);
            terms.put(term, termId);
        }

        long termId = terms.get(term);
        long docId = documents.get(document);

        try {
            precompiledAddTermDocument.setLong(1, termId);
            precompiledAddTermDocument.setLong(2, docId);
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

    private class QueryProcessor {
        private String query;
        private StringBuffer buffer = new StringBuffer();

        private QueryProcessor(String query) {
            this.query = query;
        }

        public List<String> process(String query) throws IncorrectQueryException {
            StringReader reader = new StringReader(query);
            List<String> list = new ArrayList<>();
            String sqlQuery = new String();

            String next = nextTerm(reader);
            int count = 0;
            while (!next.isEmpty()) {
                ++count;
                if (count % 2 == 0) {
                    String operator = next.toUpperCase();
                    if (!operators.containsKey(operator)) {
                        throw new IncorrectQueryException();
                    }

                    sqlQuery += operators.get(operator);
                } else {
                    sqlQuery += GET_DOCUMENTS.replace("?", "\"" + next + "\"");
                }
                next = nextTerm(reader);
            }

            list = retrieveDocuments(sqlQuery);
            return list;
        }

        //TODO Убрать костылища
        private List<String> retrieveDocuments(String sql) {
            List<String> result = new ArrayList<>();
            String prepend = "SELECT file FROM documents JOIN ( " + sql + " ) as l on l.id = documents.id;";
            try {
                Statement stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery(prepend);
                while (resultSet.next()) {
                    String file = resultSet.getString(1);
                    result.add(Paths.get(file).getFileName().toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return result;
        }

        public String nextTerm(StringReader reader) throws IncorrectQueryException {
            String STOP_SYMBOLS = ".,;:()!@#$%^&*[]{}\'\"`~";
            try {
                buffer.delete(0, buffer.length());
                int c = 0;

                while ((c = reader.read()) != -1) {
                    if (STOP_SYMBOLS.lastIndexOf(c) != -1) {
                        throw new IncorrectQueryException();
                    }
                    if (Character.isWhitespace(c)) {
                        return buffer.toString();
                    }
                    buffer.append((char)c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buffer.toString();
        }
    }

    public static void main(String[] args) {
        IndexDBAccessor dumper = new IndexDBAccessor(Paths.get("/home/ees/index.ind"), false);
    }
}
