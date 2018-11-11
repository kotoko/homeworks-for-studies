package jnp2.android.kotoko.czytnikrss;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

public class MessageList extends AppCompatActivity {
	private static final String BUNDLE_LIST_RECYCLEVIEW = "LIST_RECYCLEVIEW";
	private static final String BUNDLE_LIST_VISIBLE = "LIST_VISIBLE";

	private MessageAdapter messageAdapter;
	private ArrayList<MessageModel> visibleMessages;
	private Parcelable savedPosition;
	private ResponseReceiver receiver;

	private SharedPreferences prefs = null;

	public class ResponseReceiver extends BroadcastReceiver {
		public static final String ACTION_DB_MESSAGES_CHANGED = "jnp2.android.kotoko.czytnikrss.action.DB_MESSAGES_CHANGED";

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent != null) {
				final String action = intent.getAction();
				if(ACTION_DB_MESSAGES_CHANGED.equals(action)) {
					messageAdapter.refreshMessages();
				}
			}
		}
	}

	private void showMessage(int position) {
		MessageModel message = messageAdapter.get(position);

		String title = message.getMessageTitle();
		String content = message.getMessageContent();
		String url = message.getMessageUrl();

		Intent myIntent = new Intent(MessageList.this, ShowMessage.class);

		myIntent.putExtra("title", title);
		myIntent.putExtra("content", content);
		myIntent.putExtra("url", url);

		MessageList.this.startActivity(myIntent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.toolbar_refresh:
				Log.d("Czytnik RSS", "Kliknięto odśwież");
				DownloadService.startActionDownloadMessages(getApplicationContext());
				return true;
			case R.id.toolbar_settings:
				Log.d("Czytnik RSS", "Kliknięto ustawienia");
				Intent myIntent = new Intent(MessageList.this, Settings.class);
				MessageList.this.startActivity(myIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void enableAutoSync() {
		Context context = getApplicationContext();
		PackageManager pm = context.getPackageManager();

		// Enable setup auto sync after reboot.
		ComponentName bootReceiver = new ComponentName(context, BootReceiver.class);
		pm.setComponentEnabledSetting(bootReceiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

		// Enable auto sync using alarm manager.
		ComponentName alarmReceiver = new ComponentName(context, AlarmReceiver.class);
		pm.setComponentEnabledSetting(alarmReceiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

		// Setup auto sync now (no need to reboot device).
		Intent i = new Intent(BootReceiver.ACTION_FIRST_RUN);
		sendBroadcast(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_messages_list);

		prefs = getSharedPreferences("jnp2.android.kotoko.czytnikrss", MODE_PRIVATE);

		RecyclerView recyclerView = findViewById(R.id.message_recycler_view);
		messageAdapter = new MessageAdapter(getApplication(), recyclerView);

		// Setup recycleView.
		recyclerView.setHasFixedSize(true);
		recyclerView.setAdapter(messageAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

		// Refresh list.
		if(savedInstanceState != null) {
			savedPosition = savedInstanceState.getParcelable(BUNDLE_LIST_RECYCLEVIEW);
			visibleMessages = savedInstanceState.getParcelableArrayList(BUNDLE_LIST_VISIBLE);
			// Refreshing in onResume(), because RecycleView won't scroll here.
		} else {
			messageAdapter.refreshMessages();
		}

		// Add listener on click action.
		ItemClickSupport.addTo(recyclerView).setOnItemClickListener(
				new ItemClickSupport.OnItemClickListener() {
					@Override
					public void onItemClicked(RecyclerView recyclerView, int position, View v) {
						Log.d("Czytnik RSS" , "Kliknięto: " + position);
						showMessage(position);
					}
				}
		);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_list_messages, menu);
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Inspired: https://panavtec.me/retain-restore-recycler-view-scroll-position
		RecyclerView recyclerView = findViewById(R.id.message_recycler_view);
		outState.putParcelable(BUNDLE_LIST_RECYCLEVIEW, recyclerView.getLayoutManager().onSaveInstanceState());

		ArrayList<MessageModel> visibleMessagess = messageAdapter.getVisibleMessagess();
		outState.putParcelableArrayList(BUNDLE_LIST_VISIBLE, visibleMessagess);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Check if this is first run of app.
		if(prefs.getBoolean("firstrun", true)) {
			// Do first run stuff here then set 'firstrun' as false
			// using the following line to edit/commit prefs
			enableAutoSync();

			prefs.edit().putBoolean("firstrun", false).apply();
		}

		// Setup broadcast receiver.
		IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_DB_MESSAGES_CHANGED);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ResponseReceiver();
		registerReceiver(receiver, filter);

		// Scroll to position.
		if(visibleMessages != null && savedPosition != null) {
			messageAdapter.refreshMessages(savedPosition, visibleMessages);

			visibleMessages = null;
			savedPosition = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(receiver);
	}
}
