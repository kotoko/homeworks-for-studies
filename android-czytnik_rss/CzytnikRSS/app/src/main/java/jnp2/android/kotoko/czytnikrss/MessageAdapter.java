package jnp2.android.kotoko.czytnikrss;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
	public static class ViewHolder extends RecyclerView.ViewHolder {
		public TextView titleTextView;
		public TextView dateTextView;
		public TextView descTextView;
		public ViewHolder(View messageView) {
			super(messageView);
			titleTextView = messageView.findViewById(R.id.list_item_title);
			dateTextView = messageView.findViewById(R.id.list_item_date);
			descTextView = messageView.findViewById(R.id.list_item_desc);
		}
	}

	private DbMessageDao myDbMessageDao;
	private List<MessageModel> messages;
	private RecyclerView recyclerView;

	private static final SimpleDateFormat myFormatterToday = new SimpleDateFormat("H:mm", java.util.Locale.getDefault());
	private static final SimpleDateFormat myFormatterWeek = new SimpleDateFormat("EEEE", java.util.Locale.getDefault());
	private static final SimpleDateFormat myFormatterYear = new SimpleDateFormat("d MMMM", java.util.Locale.getDefault());
	private static final SimpleDateFormat myFormatterLong = new SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault());

	public MessageAdapter(Application application, RecyclerView recyclerView) {
		messages = new ArrayList<>();
		this.recyclerView = recyclerView;

		AppDatabase db = AppDatabase.getDatabase(application);
		myDbMessageDao = db.dbMessageDao();
	}

	private static class refreshAllAsyncTask extends AsyncTask<Void, Void, Void> {
		private DbMessageDao dao;
		private WeakReference<MessageAdapter> weakMessageAdapter;
		private WeakReference<RecyclerView> weakRecyclerView;
		Parcelable savedPosition;
		private List<MessageModel> destination;
		Boolean preload;  // true = preload; false = load all

		public refreshAllAsyncTask(MessageAdapter messageAdapter, RecyclerView recyclerView, DbMessageDao dao, List<MessageModel> destination, Boolean preload, Parcelable savedPosition) {
			this.dao = dao;
			this.weakMessageAdapter = new WeakReference<>(messageAdapter);
			this.weakRecyclerView = new WeakReference<>(recyclerView);
			this.destination = destination;
			this.preload = preload;
			this.savedPosition = savedPosition;
		}

		public refreshAllAsyncTask(MessageAdapter messageAdapter, RecyclerView recyclerView, DbMessageDao dao, List<MessageModel> destination, Boolean preload) {
			this.dao = dao;
			this.weakMessageAdapter = new WeakReference<>(messageAdapter);
			this.weakRecyclerView = new WeakReference<>(recyclerView);
			this.destination = destination;
			this.preload = preload;
			this.savedPosition = null;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			List<MessageModel> list = new ArrayList<>();
			List<DbMessage> dbList;

			if(preload) {
				dbList = dao.getTop(HardcodedAppSettings.preloadElements);
			} else {
				dbList = dao.getAll();
			}

			for(DbMessage msg : dbList) {
				list.add(new MessageModel(msg));
			}

			destination.clear();
			destination.addAll(list);

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			MessageAdapter messageAdapter = weakMessageAdapter.get();
			if(messageAdapter != null) {
				messageAdapter.notifyDataSetChanged();

				// Load all.
				if(preload) {
					new refreshAllAsyncTask(messageAdapter, weakRecyclerView.get(), dao, destination, false, savedPosition).execute();
				} else {
					RecyclerView recyclerView = weakRecyclerView.get();

					// Restore position.
					if(savedPosition != null && recyclerView != null) {
						recyclerView.stopScroll();
						recyclerView.getLayoutManager().onRestoreInstanceState(savedPosition);
					}
				}
			}

		}
	}

	// Download from database.
	public void refreshMessages() {
		if(messages.isEmpty()) {
			new refreshAllAsyncTask(this, recyclerView, myDbMessageDao, messages, true).execute();
		} else {
			new refreshAllAsyncTask(this, recyclerView, myDbMessageDao, messages, false).execute();
		}
	}

	public void refreshMessages(Parcelable savedPosition, List<MessageModel> savedList) {
		messages.clear();
		messages.addAll(savedList);
		this.notifyDataSetChanged();

		recyclerView.scrollToPosition(1);
		// For some reason scrollBy(...) did not work for me here :(
		recyclerView.smoothScrollBy(0, -50);

		new refreshAllAsyncTask(this, recyclerView, myDbMessageDao, messages, false, savedPosition).execute();
	}

	public MessageModel get(int position) {
		return messages.get(position);
	}

	@Override
	public MessageAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		Context context = viewGroup.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);

		// Inflate the custom layout
		View messageView = inflater.inflate(R.layout.messages_list_item, viewGroup, false);

		// Return a new holder instance
		return new ViewHolder(messageView);
	}

	public ArrayList<MessageModel> getVisibleMessagess() {
		final RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
		ArrayList<View> listViews = new ArrayList<>();

		for(int i = 0; i < manager.getChildCount(); ++i) {
			View child = manager.getChildAt(i);
			listViews.add(child);
		}

		Collections.sort(listViews, new Comparator<View>() {
			@Override
			public int compare(View view, View t1) {
				return Integer.compare(manager.getPosition(view), manager.getPosition(t1));
			}
		});

		ArrayList<MessageModel> listMessagess = new ArrayList<>();

		// Save visible messages.
		int last = 0;
		int position;
		for(View view : listViews) {
			position = manager.getPosition(view);
			listMessagess.add(messages.get(position));
			last = Math.max(last, position);
		}

		// Save a couple of extra messages.
		for(int i = 1; i <= HardcodedAppSettings.bundleElementsExtra && last + i < messages.size(); ++i) {
			listMessagess.add(messages.get(i + last));
		}

		return listMessagess;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int i) {
		// Get the data model based on position.
		MessageModel m = messages.get(i);

		// Set item views based on your views and data model.
		viewHolder.titleTextView.setText(android.text.Html.fromHtml(m.getMessageTitle()));
		viewHolder.descTextView.setText(android.text.Html.fromHtml(m.getMessageContent()).toString());

		// Decide which format of date.
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);

		Calendar lastSevenDays = Calendar.getInstance();
		lastSevenDays.set(Calendar.HOUR_OF_DAY, 0);
		lastSevenDays.add(Calendar.DATE, -6);

		Calendar currentYear = Calendar.getInstance();
		currentYear.set(Calendar.DAY_OF_YEAR, 1);


		SimpleDateFormat dateFormat;

		// Format for today.
		if(m.getMessageCreated().getTime() >= today.getTime().getTime()) {
			dateFormat = myFormatterToday;
		}
		// Format for last 7 days.
		else if(m.getMessageCreated().getTime() >= lastSevenDays.getTime().getTime()) {
			dateFormat = myFormatterWeek;
		}
		// Format for current year.
		else if(m.getMessageCreated().getTime() >= currentYear.getTime().getTime()) {
			dateFormat = myFormatterYear;
		}
		// Long format.
		else {
			dateFormat = myFormatterLong;
		}

		viewHolder.dateTextView.setText(dateFormat.format(m.getMessageCreated()));
	}

	@Override
	public int getItemCount() {
		return messages.size();
	}
}
