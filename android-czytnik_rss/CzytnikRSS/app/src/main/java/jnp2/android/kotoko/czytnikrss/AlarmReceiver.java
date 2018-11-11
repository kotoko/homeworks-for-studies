package jnp2.android.kotoko.czytnikrss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static jnp2.android.kotoko.czytnikrss.DownloadService.startActionDownloadMessages;

public class AlarmReceiver extends BroadcastReceiver {
	public static String ACTION_AUTO_SYNC = "jnp2.android.kotoko.czytnikrss.action.AUTO_SYNC";

	public static Intent autoSyncIntent() {
		Intent intent = new Intent();
		intent.setAction(ACTION_AUTO_SYNC);
		return intent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent != null && intent.getAction() != null) {
			if(intent.getAction().equals(ACTION_AUTO_SYNC)) {
				startActionDownloadMessages(context);
			}
		}
	}
}
