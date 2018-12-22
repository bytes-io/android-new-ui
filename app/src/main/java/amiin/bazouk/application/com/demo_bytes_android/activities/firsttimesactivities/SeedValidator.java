package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import amiin.bazouk.application.com.demo_bytes_android.R;

public class SeedValidator {

    private static final int SEED_LENGTH_MIN = 41;
    private static final int SEED_LENGTH_MAX = 81;

    public static String isSeedValid(Context context, String seed) {
        if (!seed.matches("^[A-Z9a-z]+$")) {
            if (seed.length() > SEED_LENGTH_MAX)
                return context.getString(R.string.messages_invalid_characters_seed) + " " + context.getString(R.string.messages_seed_to_long);
            else if (seed.length() < SEED_LENGTH_MIN)
                return context.getString(R.string.messages_invalid_characters_seed) + " " + context.getString(R.string.messages_seed_to_short);
            else
                return context.getString(R.string.messages_invalid_characters_seed);

        } else if (seed.matches(".*[A-Z].*") && seed.matches(".*[a-z].*")) {
            if (seed.length() > SEED_LENGTH_MAX)
                return context.getString(R.string.messages_mixed_seed) + " " + context.getString(R.string.messages_seed_to_long);
            else if (seed.length() < SEED_LENGTH_MIN)
                return context.getString(R.string.messages_mixed_seed) + " " + context.getString(R.string.messages_seed_to_short);
            else
                return context.getString(R.string.messages_mixed_seed);

        } else if (seed.length() > SEED_LENGTH_MAX) {
            return context.getString(R.string.messages_to_long_seed);

        } else if (seed.length() < SEED_LENGTH_MIN) {
            return context.getString(R.string.messages_to_short_seed);
        }

        return null;
    }

    public static String getSeed(String seed) {

        seed = seed.toUpperCase();

        if (seed.length() > SEED_LENGTH_MAX)
            seed = seed.substring(0, SEED_LENGTH_MAX);

        seed = seed.replaceAll("[^A-Z9]", "9");

        seed = StringUtils.rightPad(seed, SEED_LENGTH_MAX, '9');

        return seed;
    }
}

