package jnp2.android.kotoko.czytnikrss;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class DbUrl {
	@PrimaryKey(autoGenerate = true)
	private Long uid;

	@ColumnInfo(name = "url")
	private String url;

	public DbUrl(String url) {
		this.url = url;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Long getUid() {
		return uid;
	}

	public String getUrl() {
		return url;
	}
}
