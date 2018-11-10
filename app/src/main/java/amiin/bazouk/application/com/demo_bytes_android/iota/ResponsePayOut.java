package amiin.bazouk.application.com.demo_bytes_android.iota;

public class ResponsePayOut {
    public String hash;
    public String link;
    public String status;

    public ResponsePayOut(String hash, String link, String status) {
        this.hash = hash;
        this.link = link;
        this.status = status;
    }
}
