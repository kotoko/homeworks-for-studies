package jnp2.android.kotoko.czytnikrss;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface DbMessageDao {
	@Query("SELECT * FROM dbmessage ORDER BY createdDate DESC")
	List<DbMessage> getAll();

	@Query("SELECT * FROM dbmessage ORDER BY createdDate DESC LIMIT :n")
	List<DbMessage> getTop(int n);

	@Insert
	void insertAll(List<DbMessage> msgs);

//	@Query("DELETE FROM dbmessage")
//	void nukeTable();

	@Query("DELETE FROM dbmessage WHERE urlUid IN (:urlUids)")
	void deleteByUrlUid(long[] urlUids);

//	@Delete
//	void delete(DbMessage msg);
}
