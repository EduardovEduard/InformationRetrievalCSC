
public interface IndexBackend {
    public void start();
    public void finish();

    public long addDocument(String document);
    public long addTerm(String document);
    public void addTermDocument(long term, long document);
}
