package amiin.bazouk.application.com.demo_bytes_android.iota;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import org.json.simple.parser.ParseException;

import java.io.IOException;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.Prices;

public class IOTAPrice {
    private static Prices prices = new Prices();

    public static float getUSD(Context context) throws IOException, ParseException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        float miotUSD = preferences.getFloat(Constants.PREF_MIOTA_USD, 0);
        long loadTime = preferences.getLong(Constants.PREF_MIOTA_USD_LAST_LOAD, 0);
        long timeInterval = SystemClock.elapsedRealtime() - loadTime;
        System.out.println("Found miotUSD: " + miotUSD + " at " + loadTime);

        // get real price if invalid or lastload is > 6 hours ago
        if (miotUSD <= 0 || timeInterval > 6 * 60 * 60 * 1000) {
            return prices.get("IOT");
        }

        System.out.println("Get miotUSD from cache");
        return miotUSD;
    }

    public static void loadPrice(Context context) throws AccountException {
        float miotUSD = 0;
        try {
            miotUSD = prices.get("IOT");
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }

        long loadTime = SystemClock.elapsedRealtime();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit()
                .putLong(Constants.PREF_MIOTA_USD_LAST_LOAD, loadTime)
                .putFloat(Constants.PREF_MIOTA_USD, miotUSD)
                .apply();

        System.out.println("Loaded miotUSD: " + miotUSD + " at " + loadTime);
    }
}
