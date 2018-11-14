package amiin.bazouk.application.com.demo_bytes_android.iota;

public class AccountException extends Exception {
    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
