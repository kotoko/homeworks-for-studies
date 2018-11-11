package jnp2.android.kotoko.czytnikrss;

import android.arch.persistence.room.*;

import static android.arch.persistence.room.ForeignKey.CASCADE;


@Entity(
	foreignKeys = {
		@ForeignKey(
				entity = DbUrl.class,
				parentColumns = {"uid"},
				childColumns = {"urlUid"},
				onUpdate = CASCADE,
				onDelete = CASCADE
		)
	},
	indices = {
			@Index("createdDate"),
			@Index("urlUid")
	}
)
public class DbMessage {
	@PrimaryKey(autoGenerate = true)
	private Long uid;

	@ColumnInfo(name = "title")
	private String title;

	@ColumnInfo(name = "content")
	private String content;

	@ColumnInfo(name = "url")
	private String url;

	@ColumnInfo(name = "createdDate")
	private Long createdDate;

	@ColumnInfo(name = "urlUid")
	private long urlUid;


	public DbMessage(String title, String content, String url, Long createdDate, long urlUid) {
		this.title = title;
		this.content = content;
		this.url = url;
		this.createdDate = createdDate;
		this.urlUid = urlUid;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getUrlUid() {
		return urlUid;
	}

	public void setUrlUid(int urlUid) {
		this.urlUid = urlUid;
	}
}
