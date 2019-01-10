package amiin.bazouk.application.com.demo_bytes_android.utils;

import android.text.TextUtils;

public class EmailValidator {
    public final static boolean isValid(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
