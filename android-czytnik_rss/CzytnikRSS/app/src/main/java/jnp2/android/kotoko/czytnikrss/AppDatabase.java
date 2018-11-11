package jnp2.android.kotoko.czytnikrss;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {DbUrl.class, DbMessage.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
	private volatile static AppDatabase INSTANCE;

	public abstract DbUrlDao dbUrlDao();
	public abstract DbMessageDao dbMessageDao();

	public static AppDatabase getDatabase(final Context context) {
		if(INSTANCE == null) {
			synchronized (AppDatabase.class) {
				if(INSTANCE == null) {
					INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
						AppDatabase.class, "app_database")
						.build();
				}
			}
		}

		return INSTANCE;
	}
}
