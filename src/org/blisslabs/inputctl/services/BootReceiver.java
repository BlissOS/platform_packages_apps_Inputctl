package org.blisslabs.inputctl.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("Inputctl_Boot", "Boot completed, starting service...");
            Intent serviceIntent = new Intent(context, TabletModeService.class);
            context.startService(serviceIntent);
        }
    }
}
