package essential.bridge;

public class Config {
    public String address;
    public Integer port;
    public boolean count;
    public SharingConfig sharing;

    static class SharingConfig {
        public boolean ban;
        public boolean broadcast;
    }
}
