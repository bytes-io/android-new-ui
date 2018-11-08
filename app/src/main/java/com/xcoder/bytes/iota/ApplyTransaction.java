package com.xcoder.bytes.iota;

import android.content.Context;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.xcoder.bytes.R;

public class ApplyTransaction {
    private static Iota iota = null;
    private static Prices price = new Prices();

    private static String protocol;
    private static String host;
    private static String port;
    private static int minWeightMagnitude;
    private static String explorerHost;
    private static String toAddress;
    private static String senderSeed;

    public static void pay(Context context,long amountIni){

        if (iota == null) {
            iota = createIota(context);
        }

        try {
            System.out.println("before makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );
            List<String> tails = iota.makeTx(toAddress, amountIni);
            System.out.println("after makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );

            System.out.println(tails);
            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + tails.get(0) + " \n\n" );

        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static double getPriceUSD() throws IOException, ParseException {
        double tickerPrice = price.get("IOT");
        System.out.println(tickerPrice);
        return tickerPrice;
    }

    public static double getBalanceUSD() throws IOException, ParseException {

        long currentBalance = 1;
        double tickerPrice = price.get("IOT");
        System.out.println(tickerPrice);
        return currentBalance * tickerPrice;
    }

    private static Iota createIota(Context context) {
        String network = context.getResources().getString(R.string.network);

        if (network.equals("mainnet")) {

            protocol = context.getResources().getString(R.string.mainnet_protocol);
            host = context.getResources().getString(R.string.mainnet_host);
            port = context.getResources().getString(R.string.mainnet_port);
            minWeightMagnitude = context.getResources().getInteger(R.integer.mainnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.mainnet_explorer_host);
            toAddress = context.getResources().getString(R.string.mainnet_to_address);
            senderSeed = context.getResources().getString(R.string.mainnet_sender_seed);
        } else {

            protocol = context.getResources().getString(R.string.testnet_protocol);
            host = context.getResources().getString(R.string.testnet_host);
            port = context.getResources().getString(R.string.testnet_port);
            minWeightMagnitude = context.getResources().getInteger(R.integer.testnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.testnet_explorer_host);
            toAddress = context.getResources().getString(R.string.testnet_to_address);
            senderSeed = context.getResources().getString(R.string.testnet_sender_seed);
        }

        System.out.println("new IOTA [start]: " + DateFormat.getDateTimeInstance()
                .format(new Date()));
        Iota iota = new Iota(protocol, host, port, senderSeed);
        System.out.println("new IOTA [done]: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );

        iota.minWeightMagnitude = minWeightMagnitude;
        return iota;
    }
}
