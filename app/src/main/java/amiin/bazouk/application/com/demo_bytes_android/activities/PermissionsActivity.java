package amiin.bazouk.application.com.demo_bytes_android.activities;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;

public abstract class PermissionsActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_MANAGE_WRITE_SETTINGS = 100 ;
    private static final int MY_PERMISSIONS_USAGE_ACCESS_SETTINGS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingPermissionTurnOnHotspot();
        settingPermissionCheckHotspot();
        //settingPermissionReadDataPhone();
    }

    private void settingPermissionCheckHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(isNotAccessGranted()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, MY_PERMISSIONS_USAGE_ACCESS_SETTINGS);
            }
        }
    }

    private void settingPermissionTurnOnHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MY_PERMISSIONS_MANAGE_WRITE_SETTINGS);
            }
        }
    }

    private void settingPermissionReadDataPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(isNotAccessGranted()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, MY_PERMISSIONS_USAGE_ACCESS_SETTINGS);
            }
        }
    }

    private boolean isNotAccessGranted() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                PackageManager packageManager = getPackageManager();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
                AppOpsManager appOpsManager;
                appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int mode = 0;
                if (appOpsManager != null) {
                    mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                            applicationInfo.uid, applicationInfo.packageName);
                }
                return (mode != AppOpsManager.MODE_ALLOWED);

            } catch (PackageManager.NameNotFoundException e) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_PERMISSIONS_MANAGE_WRITE_SETTINGS) {
            if (resultCode != RESULT_OK) {
                settingPermissionTurnOnHotspot();
            }
        }
        if(requestCode == MY_PERMISSIONS_USAGE_ACCESS_SETTINGS){
            if (isNotAccessGranted()) {
                settingPermissionCheckHotspot();
            }
        }
    }
}
