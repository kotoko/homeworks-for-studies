package jnp2.android.kotoko.czytnikrss;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface DbUrlDao {
	@Query("SELECT * FROM dburl ORDER BY uid ASC")
	List<DbUrl> getAll();

	@Insert
	void insert(DbUrl url);

//	@Query("DELETE FROM dburl")
//	void nukeTable();

	@Query("DELETE FROM dburl WHERE uid LIKE :uid")
	void deleteByUid(long uid);
}
