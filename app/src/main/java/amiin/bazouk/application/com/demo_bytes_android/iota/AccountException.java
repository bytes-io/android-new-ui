package amiin.bazouk.application.com.demo_bytes_android.iota;

import com.crashlytics.android.Crashlytics;

public class AccountException extends Exception {
    public AccountException(String message, Throwable cause) {
        super(message, cause);
        Crashlytics.logException(cause);
    }
}
