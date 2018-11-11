package jnp2.android.kotoko.czytnikrss;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class DownloadService extends IntentService {
	private static final String ACTION_DOWNLOAD_MESSAGES = "jnp2.android.kotoko.czytnikrss.action.DOWNLOAD_MESSAGES";
	// source: https://stackoverflow.com/a/7927233
	private static DateFormat dateFormatterRssPubDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	private static DateFormat dateFormatterAtomReleaseDate1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
	private static DateFormat dateFormatterAtomReleaseDate2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);

	public DownloadService() {
		super("DownloadService");
	}

	/**
	 * Starts this service to perform action DownloadMessages with the given
	 * parameters. If the service is already performing a task this action
	 * will be queued.
	 *
	 * @see IntentService
	 */
	public static void startActionDownloadMessages(Context context) {
		Intent intent = new Intent(context, DownloadService.class);
		intent.setAction(ACTION_DOWNLOAD_MESSAGES);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if(intent != null) {
			final String action = intent.getAction();
			if(ACTION_DOWNLOAD_MESSAGES.equals(action)) {
				handleActionDownloadMessages();
			}
		}
	}

	/**
	 * Handle action DownloadMessages in the provided background thread with t
	 * he provided parameters.
	 */
	private void handleActionDownloadMessages() {
		Log.d("Czytnik RSS", "handleActionDownloadMessages()");

		AppDatabase db = AppDatabase.getDatabase(getApplication());
		DbMessageDao myDbMessageDao = db.dbMessageDao();
		DbUrlDao myDbUrlDao = db.dbUrlDao();

		// If there is no internet abort early.
		if(!checkInternet()) {
			return;
		}

		// Get list of links
		List<DbUrl> dbUrlList = myDbUrlDao.getAll();
		List<Long> parsedUrls = new ArrayList<>();
		List<DbMessage> parsedMessages = new ArrayList<>();

		for(DbUrl dbUrl : dbUrlList) {
			try {
				String link = dbUrl.getUrl();

				// Add http:// prefix.
				if(!link.startsWith("http://") && !link.startsWith("https://")) {
					link = "http://" + link;
				}

				URL url = new URL(dbUrl.getUrl());
				InputStream inputStream;

				// Parse xml.
				List<DbMessage> list;

				// 1. Atom
				inputStream = url.openConnection().getInputStream();
				list = parseAtomFeed(inputStream, dbUrl.getUid());

				// 2. RSS
				if(list.isEmpty()) {
					inputStream = url.openConnection().getInputStream();
					list = parseRSSFeed(inputStream, dbUrl.getUid());
				}

				parsedUrls.add(dbUrl.getUid());
				parsedMessages.addAll(list);

			} catch (IOException | XmlPullParserException e) {
				Log.e("Czytnik RSS", "DownloadService, failed to update: " + dbUrl.getUrl(), e);
			}
		}

		// Save to database.
		myDbMessageDao.deleteByUrlUid(convertLongs(parsedUrls));
		myDbMessageDao.insertAll(parsedMessages);

		// Update GUI.
		updateMessagesGUI();
	}

	private static long[] convertLongs(List<Long> list)	{
		long[] ret = new long[list.size()];
		for(int i = 0; i < ret.length; ++i) {
			ret[i] = list.get(i).intValue();
		}
		return ret;
	}

	private boolean checkInternet() {
		// source: https://stackoverflow.com/a/4239019
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	private static List<DbMessage> parseRSSFeed(InputStream inputStream, long urlUid) throws XmlPullParserException, IOException {
		// source: https://github.com/obaro/SimpleRSSReader/blob/master/app/src/main/java/com/sample/foo/simplerssreader/MainActivity.java#L74
		List<DbMessage> list = new ArrayList<>();

		String title = null;
		String url = null;
		String content = null;
		String date = null;

		boolean isItem = false;

		try {
			XmlPullParser xmlPullParser = Xml.newPullParser();
			xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			xmlPullParser.setInput(inputStream, null);

			xmlPullParser.nextTag();
			while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
				int eventType = xmlPullParser.getEventType();

				String name = xmlPullParser.getName();
				if(name == null)
					continue;

				if(eventType == XmlPullParser.END_TAG) {
					if(name.equalsIgnoreCase("item")) {
						isItem = false;
					}
					continue;
				}

				if(eventType == XmlPullParser.START_TAG) {
					if(name.equalsIgnoreCase("item")) {
						isItem = true;
						continue;
					}
				}

				String result = "";
				if(xmlPullParser.next() == XmlPullParser.TEXT) {
					result = xmlPullParser.getText();
					xmlPullParser.nextTag();
				}

				if(name.equalsIgnoreCase("title")) {
					title = result;
				} else if(name.equalsIgnoreCase("link")) {
					url = result;
				} else if(name.equalsIgnoreCase("description")) {
					content = result;
				} else if(name.equalsIgnoreCase("pubdate")) {
					date = result;
				}

				if(title != null && url != null && content != null && date != null) {
					if(isItem) {
						Date date1;

						try {
							date1 = dateFormatterRssPubDate.parse(date);
						} catch(ParseException e) {
							date1 = new Date(0);
						}

						DbMessage message = new DbMessage(title, content, url, date1.getTime(), urlUid);
						list.add(message);
					}

					title = null;
					url = null;
					content = null;
					isItem = false;
				}
			}
		} finally {
			inputStream.close();
		}

		return list;
	}

	private static List<DbMessage> parseAtomFeed(InputStream inputStream, long urlUid) throws XmlPullParserException, IOException {
		// source: https://github.com/obaro/SimpleRSSReader/blob/master/app/src/main/java/com/sample/foo/simplerssreader/MainActivity.java#L74
		List<DbMessage> list = new ArrayList<>();

		String title = null;
		String url = null;
		String content = null;
		String date = null;

		boolean isItem = false;

		try {
			XmlPullParser xmlPullParser = Xml.newPullParser();
			xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			xmlPullParser.setInput(inputStream, null);

			xmlPullParser.nextTag();
			while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
				int eventType = xmlPullParser.getEventType();

				String name = xmlPullParser.getName();
				if(name == null)
					continue;

				if(eventType == XmlPullParser.END_TAG) {
					if(name.equalsIgnoreCase("entry")) {
						isItem = false;
					}
					continue;
				}

				if(eventType == XmlPullParser.START_TAG) {
					if(name.equalsIgnoreCase("entry")) {
						isItem = true;
						continue;
					}
				}

				String result = "";
				if(xmlPullParser.next() == XmlPullParser.TEXT) {
					result = xmlPullParser.getText();
					xmlPullParser.nextTag();
				}

				if(name.equalsIgnoreCase("title")) {
					title = result;
				} else if(name.equalsIgnoreCase("link")) {
					if(url == null) {
						url = xmlPullParser.getAttributeValue(null, "href") ;
					}
				} else if(name.equalsIgnoreCase("content")) {
					content = result;
				} else if(name.equalsIgnoreCase("updated")) {
					date = result;
				}

				if(title != null && url != null && content != null && date != null) {
					if(isItem) {
						Date date1;

						try {
							try {
								date1 = dateFormatterAtomReleaseDate1.parse(date);
							} catch(ParseException e) {
								date1 = dateFormatterAtomReleaseDate2.parse(date);
							}
						} catch(ParseException e) {
							date1 = new Date(0);
						}

						DbMessage message = new DbMessage(title, content, url, date1.getTime(), urlUid);
						list.add(message);
					}

					title = null;
					url = null;
					content = null;
					isItem = false;
				}
			}
		} finally {
			inputStream.close();
		}

		return list;
	}

	private void updateMessagesGUI() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MessageList.ResponseReceiver.ACTION_DB_MESSAGES_CHANGED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);
	}
}
