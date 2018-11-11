package jnp2.android.kotoko.czytnikrss;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

public class Settings extends AppCompatActivity {

	private UrlAdapter urlAdapter;

	private void showAddDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Dodaj kanał RSS");  //TODO STRING

		// Set up the input
		final EditText input = new EditText(this);

		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton(getResources().getString(R.string.add),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				urlAdapter.addUrl(input.getText().toString());
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	private void showRemoveDialog(final int position) {
		final UrlModel urlModel = urlAdapter.get(position);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Czy na pewno usunąć" +  " " + urlModel.getUrl() + "?");  //TODO STRING

		// Set up the buttons
		builder.setPositiveButton(getResources().getString(R.string.delete),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
						urlAdapter.deleteUrl(urlModel);
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// Setup toolbar.
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Setup add button.
		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showAddDialog();
			}
		});

		// Setup arrow back.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Refresh list.
		urlAdapter = new UrlAdapter(getApplication());

		// Setup Recycle View
		RecyclerView recyclerView = findViewById(R.id.url_recycler_view);
		recyclerView.setHasFixedSize(true);
		recyclerView.setAdapter(urlAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

		// Refresh list.
		urlAdapter.refreshUrls();

		// Add listener on click action.
		ItemClickSupport.addTo(recyclerView).setOnItemClickListener(
				new ItemClickSupport.OnItemClickListener() {
					@Override
					public void onItemClicked(RecyclerView recyclerView, int position, View v) {
						showRemoveDialog(position);
					}
				}
		);
	}

}
