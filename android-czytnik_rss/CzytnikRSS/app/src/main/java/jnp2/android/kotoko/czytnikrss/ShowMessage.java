package jnp2.android.kotoko.czytnikrss;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ShowMessage extends AppCompatActivity {
	private static final String BUNDLE_MESSAGE_TITLE = "MESSAGE_TITLE";
	private static final String BUNDLE_MESSAGE_CONTENT = "MESSAGE_CONTENT";
	private static final String BUNDLE_MESSAGE_URL = "MESSAGE_URL";

	private String title;
	private String content;
	private String url;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.toolbar_url:
				if(url != null) {
					// Add http:// prefix if missing.
					if(!url.startsWith("http")) {
						url = "http://" + url;
					}

					// Run www browser.
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				} else {
					return false;
				}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_show_message);

		// Setup toolbar.
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_show_message);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if(savedInstanceState == null) {
			// Get data to show from intent.
			Intent intent = getIntent();

			title = intent.getStringExtra("title");
			content = intent.getStringExtra("content");
			url = intent.getStringExtra("url");

			// Show title in toolbar.
			getSupportActionBar().setTitle(android.text.Html.fromHtml(title));

			// Show content of message.
			TextView htmlView = findViewById(R.id.show_message_html);
			htmlView.setText(Html.fromHtml(content));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save state to bundle.
		outState.putString(BUNDLE_MESSAGE_TITLE, title);
		outState.putString(BUNDLE_MESSAGE_CONTENT, content);
		outState.putString(BUNDLE_MESSAGE_URL, url);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Recover state from bundle.
		title = savedInstanceState.getString(BUNDLE_MESSAGE_TITLE);
		content = savedInstanceState.getString(BUNDLE_MESSAGE_CONTENT);
		url = savedInstanceState.getString(BUNDLE_MESSAGE_URL);

		// Show title in toolbar.
		getSupportActionBar().setTitle(android.text.Html.fromHtml(title));

		// Show content of message.
		TextView htmlView = findViewById(R.id.show_message_html);
		htmlView.setText(Html.fromHtml(content));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_show_message, menu);
		return true;
	}

}
