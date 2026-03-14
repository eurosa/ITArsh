package com.googleapi.bluetoothweight;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    private static List<Activity> activityList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityList.add(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityList.remove(this);
    }

    public static void exitApplication(Context context) {
        // Show exit confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Are you sure you want to exit application?")
                .setPositiveButton("Exit", (dialog, which) -> {
                    // Close all activities
                    for (Activity activity : activityList) {
                        if (!activity.isFinishing()) {
                            activity.finish();
                        }
                    }

                    // Kill process
                    android.os.Process.killProcess(android.os.Process.myPid());
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Rate", (dialog, which) -> {
                    rateApp(context);
                })
                .show();
    }

    private static void rateApp(Context context) {
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + appPackageName)));
        } catch (ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}