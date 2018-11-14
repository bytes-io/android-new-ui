package amiin.bazouk.application.com.demo_bytes_android.iota;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.ActivityBuyer;
import amiin.bazouk.application.com.demo_bytes_android.MainActivity;
import amiin.bazouk.application.com.demo_bytes_android.R;
import jota.error.ArgumentException;
import jota.model.Transaction;

public class Account {
    private static Iota iota = null;
    private static Prices price = new Prices();

    private static String[] providers;
    private static int minWeightMagnitude;
    private static String explorerHost;
    private static String toAddress;
    private static String senderSeed;

    public static String paySeller(Context context){

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
                        context.getResources().getString(R.string.default_pref_miota_usd)
                ));*/

        // assumptions
        double consumptionInBytes = 1000;
        long amountIni = 0;

        List<String> tails = new ArrayList<String>();
        try {
            System.out.println("before makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );
            tails = iota.makeTx(toAddress, amountIni);
            System.out.println("after makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );

            System.out.println(tails);
            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + tails.get(0) + " \n\n" );

        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
        return tails.get(0);
    }

    public static float getPriceUSD() throws IOException, ParseException {
        float tickerPrice = price.get("IOT");
        System.out.println(tickerPrice);
        return tickerPrice;
    }

    public static String getCurrentAddress(Context context) throws ArgumentException {

        if (iota == null) {
            iota = createIota(context);
        }

        return iota.getCurrentAddress();
    }

    public static ResponseGetBalance getBalance(Context context) throws ArgumentException, IOException, ParseException {

        if (iota == null) {
            iota = createIota(context);
        }

        long balanceInI = iota.getBalance();
        double balanceInUsd = balanceInI * getPriceUSD();
        return new ResponseGetBalance(balanceInI, balanceInUsd);
    }

    public static ResponsePayOut payOut(Context context, String payOutAddress, long amountIni) {

        if (iota == null) {
            iota = createIota(context);
        }

        List<String> tails = new ArrayList<String>();
        try {
            tails = iota.makeTx(payOutAddress, amountIni);
        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }

        String hash = tails.get(0);
        String link = explorerHost + "/transaction/" + tails.get(0);
        return new ResponsePayOut(hash, link, "Pending");
    }

    public static List<TxData> getTransactionHistory(Context context) throws ArgumentException {

        if (iota == null) {
            iota = createIota(context);
        }

        List<Transaction> transactions = iota.getTransactions();
        List<TxData> txs = new ArrayList<>();

        for(Transaction tx: transactions) {

            txs.add(new TxData(tx, explorerHost));
        }
        return txs;
    }

    private static Iota createIota(Context context) {
        String network = context.getResources().getString(R.string.network);

        if (network.equals("mainnet")) {

            providers = context.getResources().getStringArray(R.array.mainnet_providers);
            minWeightMagnitude = context.getResources().getInteger(R.integer.mainnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.mainnet_explorer_host);
            toAddress = context.getResources().getString(R.string.mainnet_to_address);
            senderSeed = context.getResources().getString(R.string.mainnet_sender_seed);
        } else {

            providers = context.getResources().getStringArray(R.array.testnet_providers);
            minWeightMagnitude = context.getResources().getInteger(R.integer.testnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.testnet_explorer_host);
            toAddress = context.getResources().getString(R.string.testnet_to_address);
            senderSeed = context.getResources().getString(R.string.testnet_sender_seed);
        }

        System.out.println("new IOTA [start]: " + DateFormat.getDateTimeInstance().format(new Date()));
        boolean nodeUp = false;
        Iota iota = null;

        for(int i = 0; i < providers.length; i++) {
            try {
                iota = new Iota(providers[i], senderSeed);
                iota.minWeightMagnitude = minWeightMagnitude;

                nodeUp = iota.isNodeUp();

            } catch (Throwable e) {
                System.err.println("\nERROR: Something went wrong: " + e.getMessage());
                e.printStackTrace();
            }

            if(nodeUp) {
                break;
            }
        }
        System.out.println("new IOTA [done]: " + DateFormat.getDateTimeInstance().format(new Date()));

        iota.minWeightMagnitude = minWeightMagnitude;
        return iota;
    }
}
