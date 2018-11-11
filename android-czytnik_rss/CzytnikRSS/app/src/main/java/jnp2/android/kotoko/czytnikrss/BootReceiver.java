package jnp2.android.kotoko.czytnikrss;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import static jnp2.android.kotoko.czytnikrss.AlarmReceiver.autoSyncIntent;

public class BootReceiver extends BroadcastReceiver {
	public static String ACTION_FIRST_RUN = "jnp2.android.kotoko.czytnikrss.action.FIRST_RUN";

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent != null && intent.getAction() != null) {
			if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")
					|| intent.getAction().equals(ACTION_FIRST_RUN)) {
				// Setup auto sync every 1 hour.

				Intent myIntent = autoSyncIntent();
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);

				AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

				alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						AlarmManager.INTERVAL_HOUR, pendingIntent);

			}
		}
	}
}
