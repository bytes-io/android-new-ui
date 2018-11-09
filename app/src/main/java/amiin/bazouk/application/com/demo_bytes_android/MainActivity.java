package amiin.bazouk.application.com.demo_bytes_android;

import android.Manifest;
import android.app.AlertDialog;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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

import amiin.bazouk.application.com.demo_bytes_android.hotspot.MyOreoWifiManager;
import amiin.bazouk.application.com.demo_bytes_android.iota.ApplyTransaction;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends PermissionsActivity {

    private static final int PERMISSION_ACCESS_COARSE_LOCATION_CODE = 11 ;
    private static final int UID_TETHERING = -5;
    private static final int AMOUNT_CHANGE_CODE = 20;
    public static final String AMOUNT_INTENT = "amount_intent";
    private static final String AMOUNT_STRING = "amount_string";
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
    private long amount;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        amount = preferences.getLong(AMOUNT_STRING,1);

        //to move
        findViewById(R.id.ready_to_connect).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text to copy", ((TextView)findViewById(R.id.ready_to_connect)).getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(),"Text Copied",Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        //

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
                    //Remove it
                    ((TextView)findViewById(R.id.data)).setText(String.valueOf(res[0]+res[1]));

                    mHandler.postDelayed(mRunnableServer, 10000);
                }
            }
        };

        mRunnableClient = new Runnable() {
            public void run() {
                long [] res = new long[2];
                res[0] = TrafficStats.getTotalTxBytes()- mStartTXClient;
                res[1] = TrafficStats.getTotalRxBytes()- mStartRXClient;

                ((TextView)findViewById(R.id.data)).setText(String.valueOf(res[0]+res[1]));

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.buying_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings :
                Intent intent = new Intent(MainActivity.this,ActivityBuyer.class);
                startActivityForResult(intent,AMOUNT_CHANGE_CODE);
        }
        return true;
    }

    private void stopServer() {
        try {
            turnOffHotspot();
            server.stop();
            server=null;
            mHandler.removeCallbacks(mRunnableServer);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button buyButton = findViewById(R.id.buy_button);
                    buyButton.setEnabled(true);
                    Button sellButton = findViewById(R.id.sell_button);
                    sellButton.setText(getResources().getString(R.string.sell));
                    sellButton.setBackgroundResource(android.R.drawable.btn_default);
                    sellButton.setTextColor(buyButton.getTextColors().getDefaultColor());
                    findViewById(R.id.layout_main).setVisibility(View.VISIBLE);
                    findViewById(R.id.layout_buy).setVisibility(View.INVISIBLE);
                    ((TextView)findViewById(R.id.number_of_clients)).setText("0");
                    ((TextView)findViewById(R.id.data)).setText("0");
                    mStartTXServer = 0;
                    mStartRXServer = 0;
                    getSupportActionBar().setTitle(R.string.Bytes);
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
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
                        findViewById(R.id.sell_button).setEnabled(false);;
                        findViewById(R.id.buy_button).setEnabled(true);
                        Button buyButton = findViewById(R.id.buy_button);
                        buyButton.setText(getResources().getString(R.string.disconnect));
                        buyButton.setBackgroundColor(getResources().getColor(R.color.red));
                        buyButton.setTextColor(getResources().getColor(android.R.color.white));
                        findViewById(R.id.layout_main).setVisibility(View.INVISIBLE);
                        findViewById(R.id.layout_buy).setVisibility(View.VISIBLE);
                        getSupportActionBar().setTitle(R.string.buying);
                        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));
                    }
                });
                Thread startPaymentThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isConnectedToInternet()) {
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
                        buyButton.setText(getResources().getString(R.string.connect));
                        buyButton.setTextColor(sellButton.getTextColors().getDefaultColor());
                        buyButton.setBackgroundResource(android.R.drawable.btn_default);
                        findViewById(R.id.layout_main).setVisibility(View.VISIBLE);
                        findViewById(R.id.layout_buy).setVisibility(View.INVISIBLE);
                        getSupportActionBar().setTitle(R.string.Bytes);
                        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
                        builder.setTitle(getResources().getString(R.string.connection_closed))
                                .setMessage(getResources().getString(R.string.connection_of_client_closed))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
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
                    if(isConnectedToInternet()){
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
        if(!isConnectedToInternet()){
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
                sellButton.setText(getResources().getString(R.string.stop_selling));
                sellButton.setTextColor(getResources().getColor(android.R.color.white));
                sellButton.setBackgroundColor(getResources().getColor(R.color.red));
                findViewById(R.id.layout_main).setVisibility(View.INVISIBLE);
                findViewById(R.id.layout_buy).setVisibility(View.VISIBLE);
                getSupportActionBar().setTitle(R.string.selling);
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));
            }
        });
        paySeller();
        checkIfConnectedToWifi();
    }

    private void paySeller() {
        System.out.println("Start the transaction");
        ApplyTransaction.paySeller(this,amount);
    }

    private void checkIfConnectedToWifi() {
        Thread checkIfConnectedToWifiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(!isConnectedToInternet()){
                        stopServer();
                        return;
                    }
                }
            }
        });
        checkIfConnectedToWifiThread.start();
    }

    private boolean isConnectedToInternet() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AMOUNT_CHANGE_CODE) {
            if (resultCode == RESULT_OK) {
                amount = data.getLongExtra(AMOUNT_INTENT,amount);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(AMOUNT_STRING,amount);
                editor.apply();
            }
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
