package amiin.bazouk.application.com.demo_bytes_android.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.hotspot.MyOreoWifiManager;
import amiin.bazouk.application.com.demo_bytes_android.iota.*;

public class MainActivity extends PermissionsActivity implements NavigationView.OnNavigationItemSelectedListener{

    private static final int PERMISSION_ACCESS_COARSE_LOCATION_CODE = 11;
    //private static final int PERMISSION_ACCESS_READ_PHONE_STATS_CODE = 12;
    private static final int UID_TETHERING = -5;
    public static final String IS_SELLER = "is_seller";
    public static final String IS_BUYER = "is_buyer";
    private static final String PRICE_NOT_FOUND = "Price not found";
    private static final String CONNECTION_OPENED = "connection_opened";
    private static final String CLIENT_CLOSE_THE_CONNECTION = "client_close_the_connection";
    private WebSocketServer server;
    private WebSocketClient webSocketClient;
    private int CLIENT_DISCONNECTED_CODE = 1000;
    private Runnable mRunnableServer;
    private Runnable mRunnableClient;
    private Handler mHandler = new Handler();
    private long mStartTXServer = 0;
    private long mStartRXServer = 0;
    private long mStartTXClient = 0;
    private long mStartRXClient = 0;
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiScanReceiver = null;
    private Toolbar toolbar;
    private AppBarLayout appBar;
    private int counterException = 0;

    public static final String PREF_MIOTA_USD = "pref_miota_usd";
    public static final String PREF_MAX_PRICE_BUYER = "pref_max_price_buyer";
    public static final String PREF_MAX_PRICE_SELLER = "pref_max_price_seller";
    private static SharedPreferences preferences;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mWifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction()!= null && intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    List<ScanResult> wifiList = new ArrayList<>();
                    mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    if(mWifiManager!=null) {
                        wifiList = mWifiManager.getScanResults();
                        for (Iterator<ScanResult> iterator = wifiList.iterator(); iterator.hasNext(); ) {
                            ScanResult scanResult = iterator.next();
                            if (scanResult.SSID.length() <= 5 || !scanResult.SSID.substring(0, 6).equals("bytes-")) {
                                iterator.remove();
                            }
                        }
                    }
                    if (connectToHotspot(wifiList)) {
                        try {
                            connectToServer();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mWifiManager.setWifiEnabled(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.sell_button).setEnabled(true);
                                findViewById(R.id.buy_button).setEnabled(true);
                            }
                        });
                    }
                    unregisterReceiver(mWifiScanReceiver);
                }
            }
        };
        appBar = findViewById(R.id.appbar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(IS_SELLER, false);
        editor.putBoolean(IS_BUYER, false);
        editor.apply();

        Thread conversionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    float miotUSD = Account.getPriceUSD();
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putFloat(PREF_MIOTA_USD, miotUSD);
                    editor.apply();

                } catch (AccountException e) {
                    System.out.println("Failed due to " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        conversionThread.start();

        mRunnableServer = new Runnable() {
            @SuppressLint("MissingPermission")
            public void run() {
                long[] res = new long[2];
                NetworkStatsManager networkStatsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    networkStatsManager = getApplicationContext().getSystemService(NetworkStatsManager.class);
                    NetworkStats networkStatsWifi = null;
                    NetworkStats networkStatsMobile = null;
                    String suscriberId = "";
                    /*TelephonyManager tm = (TelephonyManager) MainActivity.this.getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm != null) {
                        suscriberId = tm.getSubscriberId();
                    }*/
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DATE, 1);
                        if (networkStatsManager != null) {
                            networkStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,
                                    suscriberId, 0, calendar.getTimeInMillis(), UID_TETHERING);
                            networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE,
                                    suscriberId, 0, calendar.getTimeInMillis(), UID_TETHERING);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    NetworkStats.Bucket bucket;

                    if (networkStatsWifi != null) {
                        while (networkStatsWifi.hasNextBucket()) {
                            bucket = new NetworkStats.Bucket();
                            networkStatsWifi.getNextBucket(bucket);
                            res[0] += bucket.getTxBytes();
                            res[1] += bucket.getRxBytes();
                        }
                    }
                    if (networkStatsMobile != null) {
                        while (networkStatsMobile.hasNextBucket()) {
                            bucket = new NetworkStats.Bucket();
                            networkStatsMobile.getNextBucket(bucket);
                            res[0] += bucket.getTxBytes();
                            res[1] += bucket.getRxBytes();
                        }
                    }
                    if (networkStatsMobile != null || networkStatsWifi != null) {
                        res[0] -= mStartTXServer;
                        res[1] -= mStartRXServer;
                    }
                } else {
                    res[0] = TrafficStats.getUidTxBytes(UID_TETHERING) - mStartTXServer;
                    res[1] = TrafficStats.getUidRxBytes(UID_TETHERING) - mStartRXServer;
                }

                System.out.println("Value of Rx: " + res[0]);
                System.out.println("Value of Tx: " + res[1]);

                if (server != null) {
                    //((TextView) findViewById(R.id.data_seller)).setText(String.valueOf(((double) (res[0] + res[1])) / 1048576) + "MB");
                    mHandler.postDelayed(mRunnableServer, 10000);
                }
            }
        };

        mRunnableClient = new Runnable() {
            public void run() {
                long[] res = new long[2];
                res[0] = TrafficStats.getTotalTxBytes() - mStartTXClient;
                res[1] = TrafficStats.getTotalRxBytes() - mStartRXClient;

                System.out.println("Value of Rx: " + res[0]);
                System.out.println("Value of Tx: " + res[1]);

                //((TextView) findViewById(R.id.data_buyer)).setText(String.valueOf(((double) (res[0] + res[1])) / 1048576) + "MB");
                mHandler.postDelayed(mRunnableClient, 10000);
            }
        };

        findViewById(R.id.sell_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread serverThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (server == null) {
                            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission( Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_ACCESS_READ_PHONE_STATS_CODE);
                            }
                            else {
                                startServer();
                            }*/
                            startServer();
                        } else {
                            stopServer();
                        }
                    }
                });
                serverThread.start();
            }
        });

        findViewById(R.id.buy_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (webSocketClient == null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION_CODE);
                            }
                            else {
                                startBuying();
                            }
                        } else {
                            webSocketClient.close();
                        }
                    }
                });
                clientThread.start();
            }
        });

        findViewById(R.id.wallet_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Payment.class);
                intent.putExtra("is_crypto_fragment", true);
                startActivity(intent);
            }
        });
    }

    private void startBuying() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button sellButton =  findViewById(R.id.sell_button);
                Button buyButton =  findViewById(R.id.buy_button);
                sellButton.setEnabled(false);
                buyButton.setEnabled(false);
            }
        });
        startClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.payment:
                intent = new Intent(MainActivity.this, Payment.class);
                startActivity(intent);
                break;
            case R.id.history:
                intent = new Intent(MainActivity.this, Payment.class);
                intent.putExtra("is_history_intent", true);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void stopServer() {
        try {
            turnOffHotspot();
            if (server != null) {
                server.stop();
            }
            server = null;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(IS_SELLER, false);
            editor.apply();
            mStartTXServer = 0;
            mStartRXServer = 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mHandler.removeCallbacks(mRunnableServer);
                    enableButtons(findViewById(R.id.buy_button),findViewById(R.id.sell_button),R.string.sell);
                    makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_main), findViewById(R.id.layout_sell));
                    changeMenuColorAndTitle(R.string.Bytes, R.color.colorPrimary,R.color.selector_text_drawer,R.color.selector_icon_drawer);
                    ((TextView) findViewById(R.id.number_of_clients)).setText("0");
                    //((TextView) findViewById(R.id.data_seller)).setText("0MB");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void turnOffHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MyOreoWifiManager myOreoWifiManager = new MyOreoWifiManager(this);
            myOreoWifiManager.stopTethering();
        } else {
            if (mWifiManager == null) {
                mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            }
            if (mWifiManager != null) {
                mWifiManager.setWifiEnabled(true);
                mWifiManager.setWifiEnabled(false);
            }
        }
    }

    private void getNetworkStatsClient() {
        mStartTXClient = TrafficStats.getTotalTxBytes();
        mStartRXClient = TrafficStats.getTotalRxBytes();

        mHandler.postDelayed(mRunnableClient, 1000);
    }

    private void startClient() {
        registerReceiver(mWifiScanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(mWifiManager!=null){
            mWifiManager.setWifiEnabled(true);
            mWifiManager.startScan();
        }
    }

    private void connectToServer() throws URISyntaxException {
        webSocketClient = new WebSocketClient(new URI( "ws://192.168.43.1:8080" )) {

            private float maxPriceSeller = 0;

            @Override
            public void onOpen(ServerHandshake handshakedata) {
            }

            @Override
            public void onMessage(String message) {
                if(message.substring(0,5).equals("price")) {
                    float maxPriceSeller = Float.valueOf(message.substring(5));
                    float maxPriceBuyer = Float.parseFloat(preferences.getString(
                            PREF_MAX_PRICE_BUYER,
                            getResources().getString(R.string.default_pref_max_price)
                    ));
                    if (maxPriceSeller <= maxPriceBuyer) {
                        System.out.println("Price Seller: " + maxPriceSeller);
                        System.out.println("Price Buyer: " + maxPriceBuyer);
                        System.out.println("The transaction will be made");
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(IS_BUYER, true);
                        editor.apply();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setAlertDialogBuilder(getResources().getString(R.string.connected_to_server), getResources().getString(R.string.connected_to_server));
                                getNetworkStatsClient();
                                disableButtons(findViewById(R.id.sell_button), findViewById(R.id.buy_button),getResources().getColor(R.color.red),R.string.disconnect, getResources().getColor(android.R.color.white));
                                makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_buy), findViewById(R.id.layout_main));
                                changeMenuColorAndTitle(R.string.buying, R.color.green,R.color.selector_text_drawer_when_buy_or_sell,R.color.selector_icon_drawer_when_buy_or_sell);
                            }
                        });
                        webSocketClient.send(CONNECTION_OPENED);
                        this.maxPriceSeller = maxPriceSeller;
                    } else {
                        System.out.println("The transaction wont be made");
                        getConnection().close(CLIENT_DISCONNECTED_CODE, PRICE_NOT_FOUND);
                        webSocketClient = null;
                    }
                }
                else if(message.substring(0,7).equals("address")){
                    String address = message.substring(7);
                    System.out.println("address: " + address);
                    Thread paySellerThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(webSocketClient!=null) {
                                long t = System.currentTimeMillis();
                                while(true){
                                    if (webSocketClient==null|| !(System.currentTimeMillis() < t + 60000)) break;
                                }
                                paySeller(maxPriceSeller, address);
                            }
                        }
                    });
                    paySellerThread.start();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote){
                if(!reason.equals(PRICE_NOT_FOUND)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.removeCallbacks(mRunnableClient);
                            enableButtons(findViewById(R.id.sell_button),findViewById(R.id.buy_button),R.string.connect);
                            makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_main), findViewById(R.id.layout_buy));
                            changeMenuColorAndTitle(R.string.Bytes, R.color.colorPrimary,R.color.selector_text_drawer,R.color.selector_icon_drawer);
                            //((TextView) findViewById(R.id.data_buyer)).setText("0MB");
                            setAlertDialogBuilder(getResources().getString(R.string.connection_closed), getResources().getString(R.string.connection_of_client_closed));
                        }
                    });
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(IS_BUYER, false);
                    editor.apply();
                    mStartTXClient = 0;
                    mStartRXClient = 0;
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAlertDialogBuilder("Seller not found","Please reduce the buyer price if you want to match a seller price (go to settings)");
                            findViewById(R.id.sell_button).setEnabled(true);
                            findViewById(R.id.buy_button).setEnabled(true);
                        }
                    });
                }
                mWifiManager.setWifiEnabled(false);
                webSocketClient = null;
            }

            @Override
            public void onError(Exception ex) {
                if (ex.getClass() == ConnectException.class) {
                    /*if(counterException<10){
                        try {
                            connectToServer();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                        counterException++;
                    }
                    else {*/
                        counterException=0;
                        mWifiManager.setWifiEnabled(false);
                        webSocketClient = null;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.sell_button).setEnabled(true);
                                findViewById(R.id.buy_button).setEnabled(true);
                                setAlertDialogBuilder(getResources().getString(R.string.connection_failed), getResources().getString(R.string.connection_of_client_failed));
                            }
                        });
                    //}
                }
                else{
                    getConnection().close(CLIENT_DISCONNECTED_CODE, "");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.sell_button).setEnabled(true);
                            findViewById(R.id.buy_button).setEnabled(true);
                        }
                    });
                }
            }
        };
        webSocketClient.connect();
    }

    private void disableButtons(Button buttonToDisable, Button buttonToChange,int resBackgroundToChange, int resTextToChange, int resTextColorToChange ) {
        buttonToDisable.setEnabled(false);
        buttonToChange.setEnabled(true);
        buttonToChange.setBackgroundColor(resBackgroundToChange);
        buttonToChange.setText(getResources().getString(resTextToChange));
        buttonToChange.setTextColor(resTextColorToChange);
    }

    private void enableButtons(Button buttonToEnable, Button buttonToChange, int resTextToChange ) {
        buttonToEnable.setEnabled(true);
        buttonToChange.setBackgroundResource(R.drawable.selector_background_buttons);
        buttonToChange.setText(getResources().getString(resTextToChange));
        ColorStateList colorStates = getResources().getColorStateList(R.color.selector_text_color_buttons);
        buttonToChange.setTextColor(colorStates);
    }

    private boolean connectToHotspot(List<ScanResult> wifiList) {
        if(wifiList.size()==0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setAlertDialogBuilder("No wifi around","List of wifis around is empty");
                }
            });
            return false;
        }
        boolean isConnected = false;
        for (ScanResult scanResult : wifiList) {
            connect(scanResult.SSID, scanResult.capabilities);
            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() < time + 15000) {
                if (isConnectedToInternet(getApplicationContext())) {
                    isConnected = true;
                    break;
                }
            }
            if (isConnected) {
                break;
            }
        }
        if (!isConnected) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setAlertDialogBuilder(getResources().getString(R.string.connection_not_found),getResources().getString(R.string.connection_cannot_be_found));
                }
            });
        }
        return isConnected;
    }

    private void connect(String ssid, String capabilities) {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        if (mWifiManager != null) {
            mWifiManager.setWifiEnabled(true);
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = String.format("\"%s\"", ssid);
            String password = "12345678";

            conf.status = WifiConfiguration.Status.ENABLED;
            conf.priority = 40;
            if (capabilities.equals("WEP")) {
                Log.v("rht", "Configuring WEP");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                if (password.matches("^[0-9a-fA-F]+$")) {
                    conf.wepKeys[0] = password;
                } else {
                    conf.wepKeys[0] = "\"".concat(password).concat("\"");
                }

                conf.wepTxKeyIndex = 0;

            } else if (capabilities.contains("WPA")) {
                Log.v("rht", "Configuring WPA");

                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                conf.preSharedKey = "\"" + password + "\"";

            } else {
                Log.v("rht", "Configuring OPEN network");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.clear();
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }

            int netId = mWifiManager.addNetwork(conf);
            if (netId == -1) {
                netId = getExistingNetworkId(conf.SSID, mWifiManager);
            }

            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId, true);
            mWifiManager.reconnect();
        }
    }

    private int getExistingNetworkId(String SSID, WifiManager wifiManager) {
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID.equals(SSID)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }

    private void startServer() {
        /*if (!isConnectedToInternet(getApplicationContext())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setAlertDialogBuilder(getResources().getString(R.string.not_connected_to_internet),getResources().getString(R.string.not_connected_to_internet));
                }
            });
            return;
        }*/
        turnOnHotspot();
        long time = System.currentTimeMillis();
        boolean isHotspotTurnOn = false;
        while (System.currentTimeMillis() < time + 15000) {
            if (isHotspotOn()) {
                isHotspotTurnOn = true;
                break;
            }
        }
        if (!isHotspotTurnOn) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setAlertDialogBuilder(getResources().getString(R.string.turn_on_hotspot),getResources().getString(R.string.turn_on_hotspot));
                }
            });
            return;
        }

        boolean isHotspotCorrect = isIpHotspotCorrect();
        time = System.currentTimeMillis();
        while (System.currentTimeMillis() < time + 15000) {
            if (isIpHotspotCorrect()) {
                isHotspotCorrect = true;
                break;
            }
        }
        if (!isHotspotCorrect) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setAlertDialogBuilder(getResources().getString(R.string.change_hotspot_address),getResources().getString(R.string.change_hotspot_address_to_default_address));
                }
            });
            return;
        }

        String ipAddress = "192.168.43.1";
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress, 8080);
        server = new WebSocketServer(inetSockAddress) {
            @Override
            public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String maxPriceSeller = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
                                PREF_MAX_PRICE_SELLER,
                                getResources().getString(R.string.default_pref_max_price)
                        );
                        System.out.print("The price for the seller is: " +maxPriceSeller);
                        conn.send("price"+maxPriceSeller);
                    }
                });
            }

            @Override
            public void onMessage(org.java_websocket.WebSocket conn, String message) {
                if(message.equals(CONNECTION_OPENED)) {
                    try {
                        conn.send("address"+Account.getCurrentAddressTemp(getApplicationContext()));
                    } catch (AccountException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(server!=null) {
                                ((TextView) findViewById(R.id.number_of_clients)).setText(String.valueOf(server.connections().size()));
                            }
                            setAlertDialogBuilder(getResources().getString(R.string.new_client_connected), getResources().getString(R.string.new_client_connected));
                        }
                    });
                }
            }

            @Override
            public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
                if(!reason.equals(PRICE_NOT_FOUND)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(server!=null) {
                                ((TextView) findViewById(R.id.number_of_clients)).setText(String.valueOf(server.connections().size()));
                            }
                            else{
                                ((TextView) findViewById(R.id.number_of_clients)).setText("0");
                            }
                            setAlertDialogBuilder(getResources().getString(R.string.connection_closed), getResources().getString(R.string.connection_of_server_closed));
                        }
                    });
                }
            }

            @Override
            public void onError(org.java_websocket.WebSocket conn, Exception ex) {
            }
        };
        server.start();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(IS_SELLER, true);
        editor.apply();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disableButtons(findViewById(R.id.buy_button),findViewById(R.id.sell_button),getResources().getColor(R.color.red), R.string.stop_selling, getResources().getColor(android.R.color.white));
                makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_sell), findViewById(R.id.layout_main));
                changeMenuColorAndTitle(R.string.selling, R.color.green,R.color.selector_text_drawer_when_buy_or_sell,R.color.selector_icon_drawer_when_buy_or_sell);
                getNetworkStatsServer();
            }
        });
        //paySeller();
        //checkIfConnectedToWifi();
    }

    private void paySeller(float amountIni,String address) {
        System.out.println("Start the transaction");
        try {
            Account.paySeller(this, amountIni,address);
        } catch (AccountException e) {
            System.out.println("Failed due to " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void getBalance() {
        System.out.println("getBalance");
        try {
            System.out.println("getCurrentAddress: " + Account.getCurrentAddress(this));

            ResponseGetBalance responseGetBalance = Account.getBalance(this);
            System.out.println(responseGetBalance.miota);
            System.out.println(responseGetBalance.usd);
        } catch (AccountException e) {
            System.out.println("Failed due to " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkIfConnectedToWifi() {
        Thread checkIfConnectedToWifiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!isConnectedToInternet(getApplicationContext())) {
                        stopServer();
                        return;
                    }
                }
            }
        });
        checkIfConnectedToWifiThread.start();
    }

    public static boolean isConnectedToInternet(Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        }
        return false;
    }

    private void turnOnHotspot() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        WifiConfiguration wifiCon = new WifiConfiguration();
        wifiCon.SSID = "bytes-";
        wifiCon.wepKeys[0] = "12345678";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MyOreoWifiManager myOreoWifiManager = new MyOreoWifiManager(this);
            myOreoWifiManager.startTethering();
        } else {
            try {
                Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
                method.invoke(mWifiManager, wifiCon, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isIpHotspotCorrect() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAdress = intf.getInetAddresses(); enumIpAdress.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAdress.nextElement();
                    if (inetAddress.getHostAddress().equals("192.168.43.1")) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isHotspotOn() {
        return new WifiApManager().isWifiApEnabled();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        if (id == R.id.settings) {
            intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.payment) {
            intent = new Intent(MainActivity.this, Payment.class);
            startActivity(intent);
        } else if (id == R.id.history) {
            intent = new Intent(MainActivity.this, Payment.class);
            intent.putExtra("is_history_intent", true);
            startActivity(intent);
        } else if (id == R.id.terms_privacy_policy) {
            Intent visitUsOnlineIntent = new Intent(Intent.ACTION_VIEW);;
            visitUsOnlineIntent.setData(Uri.parse("https://www.bytes.io/"));
            startActivity(visitUsOnlineIntent);

        } else if (id == R.id.help_support) {
            Intent visitUsOnlineIntent = new Intent(Intent.ACTION_VIEW);;
            visitUsOnlineIntent.setData(Uri.parse("https://www.bytes.io/#team"));
            startActivity(visitUsOnlineIntent);


        } else if (id == R.id.faq) {
            Intent visitUsOnlineIntent = new Intent(Intent.ACTION_VIEW);;
            visitUsOnlineIntent.setData(Uri.parse("https://www.bytes.io/"));
            startActivity(visitUsOnlineIntent);

        } else if (id == R.id.visit_us_online) {
            Intent visitUsOnlineIntent = new Intent(Intent.ACTION_VIEW);;
            visitUsOnlineIntent.setData(Uri.parse("https://www.bytes.io/"));
            startActivity(visitUsOnlineIntent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING,
        WIFI_AP_STATE_DISABLED,
        WIFI_AP_STATE_ENABLING,
        WIFI_AP_STATE_ENABLED,
        WIFI_AP_STATE_FAILED
    }

    public class WifiApManager {
        private final WifiManager mWifiManager;

        WifiApManager() {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }

        /*the following method is for getting the wifi hotspot state*/

        WIFI_AP_STATE getWifiApState() {
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApState");

                int tmp = ((Integer) method.invoke(mWifiManager));

                if (tmp > 10) {
                    tmp = tmp - 10;
                }

                return WIFI_AP_STATE.class.getEnumConstants()[tmp];
            } catch (Exception e) {
                Log.e(this.getClass().toString(), "", e);
                return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
            }
        }

        /**
         * Return whether Wi-Fi Hotspot is enabled or disabled.
         *
         * @return {@code true} if Wi-Fi AP is enabled
         * @see #getWifiApState()
         */
        boolean isWifiApEnabled() {
            return getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
        }
    }

    //@SuppressLint("MissingPermission")
    private void getNetworkStatsServer() {
        NetworkStatsManager networkStatsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkStatsManager = getApplicationContext().getSystemService(NetworkStatsManager.class);
            NetworkStats networkStatsWifi = null;
            NetworkStats networkStatsMobile = null;
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
                String suscriberId = "";
                /*TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    suscriberId = tm.getSubscriberId();
                }*/
                if (networkStatsManager != null) {
                    networkStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,
                            suscriberId, 0, calendar.getTimeInMillis(), UID_TETHERING);
                    networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE,
                            suscriberId, 0, calendar.getTimeInMillis(), UID_TETHERING);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            NetworkStats.Bucket bucket;

            if (networkStatsWifi != null) {
                while (networkStatsWifi.hasNextBucket()) {
                    bucket = new NetworkStats.Bucket();
                    networkStatsWifi.getNextBucket(bucket);
                    mStartTXServer += bucket.getTxBytes();
                    mStartRXServer += bucket.getRxBytes();
                }
            }

            if (networkStatsMobile != null) {
                while (networkStatsMobile.hasNextBucket()) {
                    bucket = new NetworkStats.Bucket();
                    networkStatsMobile.getNextBucket(bucket);
                    mStartTXServer += bucket.getTxBytes();
                    mStartRXServer += bucket.getRxBytes();
                }
            }
        }
        else {
            mStartTXServer = TrafficStats.getUidTxBytes(UID_TETHERING);
            mStartRXServer = TrafficStats.getUidRxBytes(UID_TETHERING);
        }

        mHandler.postDelayed(mRunnableServer, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Thread startBuyingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startBuying();
                        }
                    });
                    startBuyingThread.start();
                }
            /*case PERMISSION_ACCESS_READ_PHONE_STATS_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Thread startSellingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startServer();
                        }
                    });
                    startSellingThread.start();
                }*/
        }
    }

    private void makeLayoutsVisibleAndInvisible(LinearLayout layoutVisible, LinearLayout layoutInvisible){
        layoutVisible.setVisibility(View.VISIBLE);
        layoutInvisible.setVisibility(View.INVISIBLE);
    }

    private void changeMenuColorAndTitle(int resTitle, int resColor, int selectorTextDrawer, int selectorIconDrawer){
        toolbar.setTitle(resTitle);
        appBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(resColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this,resColor));
        }
        findViewById(R.id.nav_header).setBackgroundDrawable(new ColorDrawable(getResources().getColor(resColor)));
        ColorStateList colorStatesListText = getResources().getColorStateList(selectorTextDrawer);
        ColorStateList colorStatesListIcon = getResources().getColorStateList(selectorIconDrawer);
        navigationView.setItemTextColor(colorStatesListText);
        navigationView.setItemIconTintList(colorStatesListIcon);
    }

    private void setAlertDialogBuilder(String title, String message){
        new AlertDialog.Builder(this).setTitle(title)
                .setMessage(message)
                .setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mWifiScanReceiver!=null) {
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        navigationView.getMenu().getItem(0).setChecked(true);
    }

    @Override
    public void finish(){
        super.finish();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
