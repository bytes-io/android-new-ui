package amiin.bazouk.application.com.demo_bytes_android.iota;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.R;

public class Seed {
    public static void saveSeed(Context context, String seed) throws Exception {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AESCrypt aes = new AESCrypt(context.getResources().getString(R.string.seed_password));
        preferences.edit().putString(Constants.ENC_SEED, aes.encrypt(seed)).apply();
    }

    public static String getSeed(Context context) throws Exception {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AESCrypt aes = new AESCrypt(context.getResources().getString(R.string.seed_password));
        String encSeed = preferences.getString(Constants.ENC_SEED, "");
        return aes.decrypt(encSeed);
    }
}
