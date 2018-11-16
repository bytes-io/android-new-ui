package amiin.bazouk.application.com.demo_bytes_android.activities;

import android.Manifest;
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
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.hotspot.MyOreoWifiManager;
import amiin.bazouk.application.com.demo_bytes_android.iota.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends PermissionsActivity {

    private static final int PERMISSION_ACCESS_COARSE_LOCATION_CODE = 11 ;
    private static final int UID_TETHERING = -5;
    public static final String IS_SELLER = "is_seller";
    public static final String IS_BUYER = "is_buyer";
    private WebSocketServer server;
    private OkHttpClient client;
    private WebSocket webSocketClient;
    private int CLIENT_DISCONNECTED_CODE = 1000;
    private Runnable mRunnableServer;
    private Runnable mRunnableClient;
    private Handler mHandler = new Handler();
    private long mStartTXServer = 0;
    private long mStartRXServer = 0;
    private long mStartTXClient = 0;
    private long mStartRXClient = 0;
    private List<ScanResult> wifiList = new ArrayList<>();
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiScanReceiver = null;
    private Toolbar toolbar;
    private AppBarLayout appBar;

    public static final String PREF_MIOTA_USD = "pref_miota_usd";
    private static SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        appBar = findViewById(R.id.appbar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
                    editor.putFloat(PREF_MIOTA_USD,  miotUSD);
                    editor.apply();

                } catch (AccountException e) {
                    System.out.println("Failed due to " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        conversionThread.start();

        mRunnableServer = new Runnable() {
            public void run() {
                long [] res = new long[2];
                NetworkStatsManager networkStatsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    networkStatsManager = getApplicationContext().getSystemService(NetworkStatsManager.class);
                    NetworkStats networkStatsWifi = null;
                    NetworkStats networkStatsMobile = null;
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DATE, 1);
                        if (networkStatsManager != null) {
                            networkStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,
                                    "", 0, calendar.getTimeInMillis(), UID_TETHERING);
                            networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE,
                                    "", 0, calendar.getTimeInMillis(), UID_TETHERING);
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
                    if(networkStatsMobile != null || networkStatsWifi != null) {
                        res[0] -= mStartTXServer;
                        res[1] -= mStartRXServer;
                    }
                }
                else {
                    res[0] = TrafficStats.getUidTxBytes(UID_TETHERING)- mStartTXServer;
                    res[1] = TrafficStats.getUidRxBytes(UID_TETHERING)- mStartRXServer;
                }

                if(server!=null) {
                    ((TextView)findViewById(R.id.data_seller)).setText(String.valueOf(((double)(res[0]+res[1]))/1000000)+"MB");
                    mHandler.postDelayed(mRunnableServer, 10000);
                }
            }
        };

        mRunnableClient = new Runnable() {
            public void run() {
                long [] res = new long[2];
                res[0] = TrafficStats.getTotalTxBytes()- mStartTXClient;
                res[1] = TrafficStats.getTotalRxBytes()- mStartRXClient;
                ((TextView)findViewById(R.id.data_buyer)).setText(String.valueOf(((double)(res[0]+res[1]))/1000000)+"MB");
                mHandler.postDelayed(mRunnableClient, 10000);
            }
        };

        findViewById(R.id.sell_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread serverThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(server==null) {
                            startServer();
                            if(server!=null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getNetworkStatsServer();
                                    }
                                });
                            }
                        }
                        else{
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
                Thread clientThread;
                if(client == null) {
                    findViewById(R.id.sell_button).setEnabled(false);
                    findViewById(R.id.buy_button).setEnabled(false);
                    clientThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startClient();
                        }
                    });
                }
                else{
                    clientThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            webSocketClient.close(CLIENT_DISCONNECTED_CODE,"");
                        }
                    });
                }
                clientThread.start();
            }
        });

        findViewById(R.id.wallet_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("wallet btn click");

                getBalance();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.buying_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.settings :
                intent = new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.payment :
                intent = new Intent(MainActivity.this,Payment.class);
                startActivity(intent);
                break;
            case R.id.history :
                intent = new Intent(MainActivity.this,Payment.class);
                intent.putExtra("is_history_intent",true);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void stopServer() {
        try {
            turnOffHotspot();
            if(server!=null) {
                server.stop();
            }
            server=null;
            mHandler.removeCallbacks(mRunnableServer);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(IS_SELLER,false);
            editor.apply();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button buyButton = findViewById(R.id.buy_button);
                    buyButton.setEnabled(true);
                    Button sellButton = findViewById(R.id.sell_button);
                    sellButton.setBackgroundDrawable(buyButton.getBackground());
                    changeButtonCharacteristics(sellButton, R.string.sell, buyButton.getTextColors().getDefaultColor());
                    makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_main),findViewById(R.id.layout_sell));
                    changeMenuColorAndTitle(R.string.Bytes,R.color.colorPrimary);
                    ((TextView)findViewById(R.id.number_of_clients)).setText("0");
                    ((TextView)findViewById(R.id.data_seller)).setText("0MB");
                    mStartTXServer = 0;
                    mStartRXServer = 0;
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
        }
        else{
            if(mWifiManager == null){
                mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            }
            if(mWifiManager != null) {
                mWifiManager.setWifiEnabled(true);
            }
        }
    }

    private void getNetworkStatsClient() {
        mStartTXClient = TrafficStats.getTotalTxBytes();
        mStartRXClient = TrafficStats.getTotalRxBytes();

        mHandler.postDelayed(mRunnableClient, 1000);
    }

    private void startClient() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION_CODE);
            }
            else{
                getWifiList();
            }
        }
        else{
            getWifiList();
        }
        if(connectToHotspot()) {
            connectToServer();
        }
        else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.sell_button).setEnabled(true);
                    findViewById(R.id.buy_button).setEnabled(true);
                }
            });
        }
    }

    private void connectToServer() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        client = new OkHttpClient();
        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(IS_BUYER,true);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        builder.setTitle(getResources().getString(R.string.connected_to_server))
                                .setMessage(getResources().getString(R.string.connected_to_server))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                        getNetworkStatsClient();
                        findViewById(R.id.sell_button).setEnabled(false);
                        Button buyButton = findViewById(R.id.buy_button);
                        buyButton.setEnabled(true);
                        buyButton.setBackgroundColor(getResources().getColor(R.color.red));
                        changeButtonCharacteristics(buyButton,R.string.disconnect,getResources().getColor(android.R.color.white));
                        makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_buy),findViewById(R.id.layout_main));
                        changeMenuColorAndTitle(R.string.buying,R.color.green);
                    }
                });
                Thread startPaymentThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isConnectedToInternet(getApplicationContext())) {
                            paySeller();
                        }
                        /*while(webSocketClient!=null){
                            long t = System.currentTimeMillis();
                            while(true){
                                if (!(System.currentTimeMillis() < t + 60000)) break;
                            }
                            //pay();
                        }*/
                    }
                });
                startPaymentThread.start();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                mWifiManager.setWifiEnabled(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.removeCallbacks(mRunnableClient);
                        Button sellButton = findViewById(R.id.sell_button);
                        sellButton.setEnabled(true);
                        Button buyButton = findViewById(R.id.buy_button);
                        buyButton.setBackgroundResource(android.R.drawable.btn_default);
                        changeButtonCharacteristics(buyButton, R.string.connect, sellButton.getTextColors().getDefaultColor());
                        makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_main),findViewById(R.id.layout_buy));
                        changeMenuColorAndTitle(R.string.Bytes,R.color.colorPrimary);
                        ((TextView)findViewById(R.id.data_buyer)).setText("0MB");
                        builder.setTitle(getResources().getString(R.string.connection_closed))
                                .setMessage(getResources().getString(R.string.connection_of_client_closed))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(IS_BUYER,false);
                editor.apply();
                mStartTXClient = 0;
                mStartRXClient = 0;
                client = null;
                webSocketClient = null;
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, final Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.sell_button).setEnabled(true);
                        findViewById(R.id.buy_button).setEnabled(true);
                    }
                });
                if (t.getClass() == ConnectException.class) {
                    mWifiManager.setWifiEnabled(false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.setTitle(getResources().getString(R.string.connection_failed))
                                    .setMessage(getResources().getString(R.string.connection_of_client_failed))
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }).setIcon(android.R.drawable.ic_dialog_alert).show();
                        }
                    });
                }
                if (t.getClass() == SocketException.class) {
                    onClosed(webSocket, CLIENT_DISCONNECTED_CODE, "");
                }
            }
        };
        Request request = new Request.Builder().url("ws://192.168.43.1:38301").build();
        webSocketClient = client.newWebSocket(request, webSocketListener);
        client.dispatcher().executorService().shutdown();
    }

    private boolean connectToHotspot() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        long time = System.currentTimeMillis();
        while(System.currentTimeMillis()<time+15000){
            if(wifiList.size()>0){
                break;
            }
        }
        boolean isConnected = false;
        for(ScanResult scanResult: wifiList){
            String ssid = scanResult.SSID;
            if(ssid.length()>=6 && ssid.substring(0,6).equals("bytes-")){
                connect(ssid,scanResult.capabilities);
                time = System.currentTimeMillis();
                while(System.currentTimeMillis()<time+15000){
                    if(isConnectedToInternet(getApplicationContext())){
                        isConnected = true;
                        break;
                    }
                }
            }
            if(isConnected){
                break;
            }
        }
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
        mWifiScanReceiver = null;
        wifiList = new ArrayList<>();
        if(!isConnected){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.connection_not_found))
                            .setMessage(getResources().getString(R.string.connection_cannot_be_found))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
        }
        return isConnected;
    }

    private void connect(String ssid,String capabilities) {
        if(mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        if(mWifiManager!=null) {
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
                netId = getExistingNetworkId(conf.SSID,mWifiManager);
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
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        if(!isConnectedToInternet(getApplicationContext())){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.not_connected_to_internet))
                            .setMessage(getResources().getString(R.string.you_are_not_connected_to_internet))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return;
        }
        turnOnHotspot();
        long time = System.currentTimeMillis();
        boolean isHotspotTurnOn = false;
        while(System.currentTimeMillis()<time+15000){
            if(isHotspotOn()){
               isHotspotTurnOn = true;
               break;
            }
        }
        if(!isHotspotTurnOn){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.turn_on_hotspot))
                            .setMessage(getResources().getString(R.string.turn_on_hotspot))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return;
        }

        boolean isHotspotCorrect = isIpHotspotCorrect();
        time = System.currentTimeMillis();
        while(System.currentTimeMillis()<time+15000){
            if(isIpHotspotCorrect()){
                isHotspotCorrect = true;
                break;
            }
        }
        if(!isHotspotCorrect){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.change_hotspot_address))
                            .setMessage(getResources().getString(R.string.change_hotspot_address_to_default_address))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return;
        }

        String ipAddress = "192.168.43.1";
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress,38301);
        server = new WebSocketServer(inetSockAddress){
            @Override
            public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.number_of_clients)).setText(String.valueOf(server.connections().size()));
                        builder.setTitle(getResources().getString(R.string.new_client_connected))
                                .setMessage(getResources().getString(R.string.new_client_connected))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
            }
            @Override
            public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.number_of_clients)).setText(String.valueOf(server.connections().size()));
                        builder.setTitle(getResources().getString(R.string.connection_closed))
                                .setMessage(getResources().getString(R.string.connection_of_server_closed))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
            }

            @Override
            public void onMessage(org.java_websocket.WebSocket conn, String message) {
            }

            @Override
            public void onError(org.java_websocket.WebSocket conn, Exception ex) {
            }
        };
        server.start();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.buy_button).setEnabled(false);
                Button sellButton = findViewById(R.id.sell_button);
                sellButton.setBackgroundColor(getResources().getColor(R.color.red));
                changeButtonCharacteristics(sellButton, R.string.stop_selling, getResources().getColor(android.R.color.white));
                makeLayoutsVisibleAndInvisible(findViewById(R.id.layout_sell),findViewById(R.id.layout_main));
                changeMenuColorAndTitle(R.string.selling,R.color.green);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(IS_SELLER,true);
                editor.apply();
            }
        });
        paySeller();
        checkIfConnectedToWifi();
    }

    private void paySeller() {
        System.out.println("Start the transaction");
        try {
            Account.paySeller(this);
        } catch (AccountException e) {
            System.out.println("Failed due to " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void getBalance() {
        System.out.println("getBalance");
        try {
            System.out.println("getCurrentAddress: "+Account.getCurrentAddress(this));

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
                while(true){
                    if(!isConnectedToInternet(getApplicationContext())){
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
        if(connectivityManager!=null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        }
        return false;
    }

    private void turnOnHotspot() {
        if(mWifiManager == null){
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        WifiConfiguration wifiCon = new WifiConfiguration();
        wifiCon.SSID = "bytes-";
        wifiCon.wepKeys[0] = "12345678";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MyOreoWifiManager myOreoWifiManager = new MyOreoWifiManager(this);
            myOreoWifiManager.startTethering();
        }
        else{
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
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAdress = intf.getInetAddresses(); enumIpAdress.hasMoreElements();){
                    InetAddress inetAddress = enumIpAdress.nextElement();
                    if(inetAddress.getHostAddress().equals("192.168.43.1")){
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isHotspotOn(){
        return new WifiApManager().isWifiApEnabled();
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
            mWifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

    private void getNetworkStatsServer() {
        NetworkStatsManager networkStatsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkStatsManager = getApplicationContext().getSystemService(NetworkStatsManager.class);
            NetworkStats networkStatsWifi = null;
            NetworkStats networkStatsMobile = null;
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
                if (networkStatsManager != null) {
                    networkStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,
                            "", 0, calendar.getTimeInMillis(), UID_TETHERING);
                    networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE,
                            "", 0, calendar.getTimeInMillis(), UID_TETHERING);
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

            if (networkStatsWifi != null) {
                if (networkStatsMobile != null) {
                    while (networkStatsMobile.hasNextBucket()) {
                        bucket = new NetworkStats.Bucket();
                        networkStatsMobile.getNextBucket(bucket);
                        mStartTXServer += bucket.getTxBytes();
                        mStartRXServer += bucket.getRxBytes();
                    }
                }
            }
        }
        else {
            mStartTXServer = TrafficStats.getUidTxBytes(UID_TETHERING);
            mStartRXServer = TrafficStats.getUidRxBytes(UID_TETHERING);
        }

        mHandler.postDelayed(mRunnableServer, 1000);
    }

    private void getWifiList() {
        if(mWifiManager!=null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        if(mWifiManager!=null){
            mWifiManager.setWifiEnabled(true);
            mWifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    if (intent.getAction()!= null && intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                        wifiList = mWifiManager.getScanResults();
                    }
                }
            };
            registerReceiver(mWifiScanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getWifiList();
                    if(connectToHotspot()) {
                        connectToServer();
                    }
                }
        }
    }

    private void changeButtonCharacteristics(Button buttonCharacteristicsToChange, int resTextToChange, int resTextColorToChange){
        buttonCharacteristicsToChange.setText(getResources().getString(resTextToChange));
        buttonCharacteristicsToChange.setTextColor(resTextColorToChange);
    }

    private void makeLayoutsVisibleAndInvisible(LinearLayout layoutVisible, LinearLayout layoutInvisible){
        layoutVisible.setVisibility(View.VISIBLE);
        layoutInvisible.setVisibility(View.INVISIBLE);
    }

    private void changeMenuColorAndTitle(int resTitle, int resColor){
        toolbar.setTitle(resTitle);
        appBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(resColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this,resColor));
        }
    }


    @Override
    public void onPause(){
        super.onPause();
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }

    @Override
    public void finish(){
        super.finish();
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }
}
