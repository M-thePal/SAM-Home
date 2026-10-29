package ir.samhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

// Opens the home screen when the TV turns on. The TV starts its own player first, so this waits a
// few seconds and then opens on top of it.
public class BootReceiver extends BroadcastReceiver {

    private static final long DELAY_MS = 6000;

    @Override
    public void onReceive(Context context, Intent intent) {
        openSoon(context);
    }

    static void openSoon(Context context) {
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + DELAY_MS, pi);
    }
}
