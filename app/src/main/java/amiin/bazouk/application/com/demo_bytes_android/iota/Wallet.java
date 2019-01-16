package amiin.bazouk.application.com.demo_bytes_android.iota;

import android.content.Context;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.utils.InternetConn;

public class Wallet {
    private static Iota iota = null;

    private static String[] providers;
    private static int minWeightMagnitude;
    private static String explorerHost;

    public static String paySeller(Context context, float maxGBPriceSeller, String address, long dataUsageBytesPerMinute) throws AccountException {

        if (iota == null) {
            iota = createIota(context);
        }

        List<String> tails;
        try {

            float miotaUSD = IOTAPrice.getMIOTAUSD(context);
            double iotaUSD = (double)miotaUSD / 1000 / 1000;
            System.out.println(miotaUSD + " iotaUSD: " + iotaUSD);

            double costPerByte = (double)maxGBPriceSeller / (1024 * 1024);
            System.out.println("costPerByte: " + costPerByte);

            double amountToPayInUSD = costPerByte * dataUsageBytesPerMinute;
            System.out.println("amountToPayInUSD: " + amountToPayInUSD);

            long amountToPayIni = (long) (amountToPayInUSD / iotaUSD);

            System.out.println("before makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );
            tails = iota.makeTx(address, amountToPayIni);
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

        double balanceInUsd;
        long balanceIni;
        try {
            balanceIni = iota.getBalance();
            double balanceInMi = (double)balanceIni / 1000 / 1000;
//            double balanceInMi = jota.utils.IotaUnitConverter.convertUnits(balanceIni, IotaUnits.IOTA, IotaUnits.MEGA_IOTA);

            System.out.println("balanceIni: " +balanceIni);
            System.out.println("balanceInMi: " +balanceInMi);
            balanceInUsd = balanceInMi * IOTAPrice.getMIOTAUSD(context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        return new ResponseGetBalance(balanceIni, balanceInUsd);
    }

    public static ResponsePayOut payOut(Context context, String payOutAddress, long amountIni) throws AccountException {

        if (iota == null) {
            iota = createIota(context);
        }
        System.out.println("Payout:" + amountIni + " to: " + payOutAddress);

        List<String> tails = new ArrayList<String>();
        try {
            tails = iota.makeTx(payOutAddress, amountIni);
        } catch(Exception e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }

        String hash = tails.get(0);
        String link = getTxLink(tails.get(0));
        return new ResponsePayOut(hash, link, "Pending");
    }

    public static List<TxData> getTransactionHistory(Context context) throws AccountException {

        if (iota == null) {
            iota = createIota(context);
        }

        List<TxData> txs = new ArrayList<>();
        try {
            txs = iota.getTransactions();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AccountException("ACCOUNT_ERROR", e);
        }
        Collections.reverse(txs);
        return txs;
    }

    public static boolean isAddressValid(String address) {
        return Iota.isAddress(address);
    }

    public static String getTxLink(String hash) {
        return explorerHost + "/transaction/" + hash;
    }


    private static Iota createIota(Context context) {
        if(!InternetConn.isConnected(context)) {
            return null;
        }
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
