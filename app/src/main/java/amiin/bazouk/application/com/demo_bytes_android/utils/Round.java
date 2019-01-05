package amiin.bazouk.application.com.demo_bytes_android.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Round {
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
