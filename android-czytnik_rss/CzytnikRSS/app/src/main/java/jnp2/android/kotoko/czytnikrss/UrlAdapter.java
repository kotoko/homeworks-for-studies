package jnp2.android.kotoko.czytnikrss;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UrlAdapter extends RecyclerView.Adapter<UrlAdapter.ViewHolder> {
	public static class ViewHolder extends RecyclerView.ViewHolder {
		public TextView urlTextView;
		public ViewHolder(View urlView) {
			super(urlView);
			urlTextView = urlView.findViewById(R.id.list_item_url);
		}
	}

	private List<UrlModel> urls;
	private DbUrlDao myDbUrlDao;

	public UrlAdapter(Application application) {
		this.urls = new ArrayList<>();

		AppDatabase db = AppDatabase.getDatabase(application);
		myDbUrlDao = db.dbUrlDao();
	}

	private static class refreshAllAsyncTask extends AsyncTask<Void, Void, Void> {
		private DbUrlDao dao;
		private WeakReference<UrlAdapter> weakUrlAdapter;
		private List<UrlModel> destination;

		public refreshAllAsyncTask(UrlAdapter urlAdapter, DbUrlDao dao, List<UrlModel> destination) {
			this.dao = dao;
			this.weakUrlAdapter = new WeakReference<>(urlAdapter);
			this.destination = destination;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			List<UrlModel> list = new ArrayList<>();
			List<DbUrl> dbList;

			dbList = dao.getAll();

			for(DbUrl url : dbList) {
				list.add(new UrlModel(url));
			}

			destination.clear();
			destination.addAll(list);

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			UrlAdapter urlAdapter = weakUrlAdapter.get();
			if(urlAdapter != null) {
				urlAdapter.notifyDataSetChanged();
			}
		}
	}

	private static class addUrlAsyncTask extends AsyncTask<Void, Void, Void> {
		private DbUrlDao dao;
		private WeakReference<UrlAdapter> weakUrlAdapter;
		private UrlModel newUrl;
		private List<UrlModel> destination;

		public addUrlAsyncTask(UrlAdapter urlAdapter, DbUrlDao dao, List<UrlModel> destination, UrlModel newUrl) {
			this.dao = dao;
			this.weakUrlAdapter = new WeakReference<>(urlAdapter);
			this.newUrl = newUrl;
			this.destination = destination;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			DbUrl dbUrl = new DbUrl(newUrl.getUrl());
			dao.insert(dbUrl);

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			UrlAdapter urlAdapter = weakUrlAdapter.get();
			if(urlAdapter != null) {
				// Refresh GUI.
				new refreshAllAsyncTask(urlAdapter, dao, destination).execute();
			}
		}
	}

	private static class deleteUrlAsyncTask extends AsyncTask<Void, Void, Void> {
		private DbUrlDao dao;
		private WeakReference<UrlAdapter> weakUrlAdapter;
		private UrlModel oldUrl;
		private List<UrlModel> destination;

		public deleteUrlAsyncTask(UrlAdapter urlAdapter, DbUrlDao dao, List<UrlModel> destination, UrlModel oldUrl) {
			this.dao = dao;
			this.weakUrlAdapter = new WeakReference<>(urlAdapter);
			this.oldUrl = oldUrl;
			this.destination = destination;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			dao.deleteByUid(oldUrl.getUid());
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			UrlAdapter urlAdapter = weakUrlAdapter.get();
			if(urlAdapter != null) {
				// Refresh GUI.
				new refreshAllAsyncTask(urlAdapter, dao, destination).execute();
			}
		}
	}

	// Download from database.
	public void refreshUrls() {
		new UrlAdapter.refreshAllAsyncTask(this, myDbUrlDao, urls).execute();
	}

	// Add to database and refresh GUI.
	public void addUrl(String url) {
		UrlModel newUrl = new UrlModel(url);
		new UrlAdapter.addUrlAsyncTask(this, myDbUrlDao, urls, newUrl).execute();
	}

	// Delete from database and refresh GUI.
	public void deleteUrl(UrlModel oldUrl) {
		new UrlAdapter.deleteUrlAsyncTask(this, myDbUrlDao, urls, oldUrl).execute();
	}

	@Override
	public UrlAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		Context context = viewGroup.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);

		// Inflate the custom layout
		View messageView = inflater.inflate(R.layout.url_list_item, viewGroup, false);

		// Return a new holder instance
		return new UrlAdapter.ViewHolder(messageView);
	}

	@Override
	public void onBindViewHolder(UrlAdapter.ViewHolder viewHolder, int i) {
		// Get the data model based on position.
		 UrlModel u = urls.get(i);

		// Set item views based on your views and data model.
		viewHolder.urlTextView.setText(u.getUrl());
	}

	@Override
	public int getItemCount() {
		return urls.size();
	}

	public UrlModel get(int position) {
		return urls.get(position);
	}
}
