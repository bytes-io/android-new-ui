package amiin.bazouk.application.com.demo_bytes_android.iota;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.Prices;
import amiin.bazouk.application.com.demo_bytes_android.R;
import jota.model.Transaction;

public class Wallet {
    private static Iota iota = null;

    private static String[] providers;
    private static int minWeightMagnitude;
    private static String explorerHost;

    public static String paySeller(Context context, float amountIni,String address) throws AccountException {

        if (iota == null) {
            iota = createIota(context);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        /*float maxPrice = Float.parseFloat(
                preferences.getString(
                        ActivityBuyer.PREF_MAX_PRICE,
                        context.getResources().getString(R.string.default_pref_max_price)
                ));
        float miotaUSD = Float.parseFloat(
                preferences.getString(
                        MainActivity.PREF_MIOTA_USD,
                        0
                ));*/

        // assumptions
        double consumptionInBytes = 1000;

        List<String> tails;
        try {
            System.out.println("before makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );
            //ADRIEN : float cast to long
            tails = iota.makeTx(address, (long)amountIni);
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

    public static float getPriceUSD(Context context) throws AccountException {
        float tickerPrice = 0;
        try {
            tickerPrice = IOTAPrice.getUSD(context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        return tickerPrice;
    }

    public static String getCurrentAddress(Context context) throws AccountException {

        if (iota == null) {
            iota = createIota(context);
        }

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

        if (iota == null) {
            iota = createIota(context);
        }

        double balanceInUsd = 0;
        long balanceInI = 0;
        try {
            balanceInUsd = balanceInI * getPriceUSD(context);
            balanceInI = iota.getBalance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        return new ResponseGetBalance(balanceInI, balanceInUsd);
    }

    public static ResponsePayOut payOut(Context context, String payOutAddress, long amountIni) throws AccountException {

        if (iota == null) {
            iota = createIota(context);
        }

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

        if (iota == null) {
            iota = createIota(context);
        }

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


    private static Iota createIota(Context context) {
        providers = context.getResources().getStringArray(R.array.mainnet_providers);
        minWeightMagnitude = context.getResources().getInteger(R.integer.mainnet_min_weight_magnitude);
        explorerHost = context.getResources().getString(R.string.mainnet_explorer_host);

        System.out.println("new IOTA [start]: " + DateFormat.getDateTimeInstance().format(new Date()));
        Iota iota = null;
        try {
            String seed = Seed.getSeed(context);

            for(int i = 0; i < providers.length; i++) {
                iota = new Iota(providers[i], seed);

                if(iota.isNodeUp()) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("new IOTA [done]: " + DateFormat.getDateTimeInstance().format(new Date()));

        iota.minWeightMagnitude = minWeightMagnitude;
        return iota;
    }
}
