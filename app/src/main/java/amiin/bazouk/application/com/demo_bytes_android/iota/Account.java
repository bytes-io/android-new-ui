package amiin.bazouk.application.com.demo_bytes_android.iota;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;
import amiin.bazouk.application.com.demo_bytes_android.activities.SettingsActivity;
import jota.model.Transaction;

public class Account {
    private static Iota sellerIota = null;
    private static Iota buyerIota = null;
    private static Prices price = new Prices();

    private static String[] providers;
    private static int minWeightMagnitude;
    private static String explorerHost;
    private static String sellerSeed;
    private static String buyerSeed;
    private static SharedPreferences preferences;

    public static String paySeller(Context context, float maxPriceSeller, String address) throws AccountException {

        Iota iota = getIota(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        float maxPriceBuyerInGB = Float.parseFloat(
                preferences.getString(
                        SettingsActivity.PREF_MAX_PRICE_BUYER,
                        context.getResources().getString(R.string.default_pref_max_price)
                ));
        float miotaUSD = Float.parseFloat(
                preferences.getString(
                        MainActivity.PREF_MIOTA_USD,
                        context.getResources().getString(R.string.default_pref_miota_usd)
                ));

        int consumptionInMB = 1; // assumption

        double costPerMB = maxPriceBuyerInGB / 1024;
        double toPayIn$ = costPerMB * consumptionInMB;

        long amountIni = Math.round((toPayIn$ / (miotaUSD / 1000))) ;
        System.out.println("amountIni:" + amountIni);

        List<String> tails;
        try {
            System.out.println("before makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );
            tails = iota.makeTx(address, 0);
            System.out.println("after makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );

            System.out.println(tails);
            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + tails.get(0) + " \n\n" );

        } catch(Exception e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        return tails.get(0);
    }

    public static float getPriceUSD() throws AccountException {
        float tickerPrice = 0;
        try {
            tickerPrice = price.get("IOT");
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        System.out.println(tickerPrice);
        return tickerPrice;
    }

    public static String getCurrentAddress(Context context) throws AccountException {

        Iota iota = getIota(context);

        String address;
        try {
            address = iota.getCurrentAddress();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        return address;
    }

    public static ResponseGetBalance getBalance(Context context) throws AccountException {

        Iota iota = getIota(context);

        double balanceInUsd = 0;
        long balanceInI = 0;
        try {
            balanceInUsd = balanceInI * getPriceUSD();
            balanceInI = iota.getBalance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        return new ResponseGetBalance(balanceInI, balanceInUsd);
    }

    public static ResponsePayOut payOut(Context context, String payOutAddress, long amountIni) throws AccountException {

        Iota iota = getIota(context);

        List<String> tails = new ArrayList<String>();
        try {
            tails = iota.makeTx(payOutAddress, amountIni);
        } catch(Exception e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }

        String hash = tails.get(0);
        String link = explorerHost + "/transaction/" + tails.get(0);
        return new ResponsePayOut(hash, link, "Pending");
    }

    public static List<TxData> getTransactionHistory(Context context) throws AccountException {

        Iota iota = getIota(context);

        List<Transaction> transactions = null;
        try {
            transactions = iota.getTransactions();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        List<TxData> txs = new ArrayList<>();

        for(Transaction tx: transactions) {
            txs.add(new TxData(tx, explorerHost));
        }
        return txs;
    }

    private static Iota getIota(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if(preferences.getBoolean(MainActivity.IS_BUYER,false)) {
            if (buyerIota == null) {
                buyerIota = createIota(context, "buyer");
            }
            return buyerIota;
        } else {
            if (sellerIota == null) {
                sellerIota = createIota(context, "seller");
            }
            return sellerIota;
        }
    }

    private static Iota createIota(Context context, String accountType) {
        String network = context.getResources().getString(R.string.network);

        if (network.equals("mainnet")) {

            providers = context.getResources().getStringArray(R.array.mainnet_providers);
            minWeightMagnitude = context.getResources().getInteger(R.integer.mainnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.mainnet_explorer_host);
            sellerSeed = context.getResources().getString(R.string.mainnet_seller_seed);
            buyerSeed = context.getResources().getString(R.string.mainnet_buyer_seed);
        } else {

            providers = context.getResources().getStringArray(R.array.testnet_providers);
            minWeightMagnitude = context.getResources().getInteger(R.integer.testnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.testnet_explorer_host);
            sellerSeed = context.getResources().getString(R.string.mainnet_seller_seed);
            buyerSeed = context.getResources().getString(R.string.mainnet_buyer_seed);
        }
        System.out.println("new IOTA [start]: " + DateFormat.getDateTimeInstance().format(new Date()));

        Iota iota = null;
        String seed;
        if(accountType.equals("buyer")) {
            seed = buyerSeed;
        } else {
            seed = sellerSeed;
        }
        for(int i = 0; i < providers.length; i++) {
            try {
                iota = new Iota(providers[i], seed);
                iota.minWeightMagnitude = minWeightMagnitude;

            } catch (Exception e) {
                System.err.println("\nERROR: Something went wrong: " + e.getMessage());
                e.printStackTrace();
            }

            if(iota.isNodeUp()) {
                break;
            }
        }
        System.out.println("new IOTA [done]: " + DateFormat.getDateTimeInstance().format(new Date()));

        iota.minWeightMagnitude = minWeightMagnitude;
        return iota;
    }
}

class IotaData {
    public void IotaData(Iota iota) {

    }

    public void getAddresses(Iota iota) {
        new GetAddresses(iota).execute();
    }
}


class GetAddresses extends AsyncTask<String, Integer, String> {
    private Iota iota;

    // a constructor so that you can pass the object and use
    GetAddresses(Iota iota){
        this.iota = iota;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        // get the string from params, which is an array
        String myString = params[0];

        // Do something that takes a long time, for example:
        for (int i = 0; i <= 100; i++) {

            // Do things

            // Call this to update your progress
            publishProgress(i);
        }

        return "this string is passed to onPostExecute";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }
}
