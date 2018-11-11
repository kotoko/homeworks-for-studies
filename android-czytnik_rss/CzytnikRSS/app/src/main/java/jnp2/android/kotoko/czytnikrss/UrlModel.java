package jnp2.android.kotoko.czytnikrss;

import android.os.Parcel;
import android.os.Parcelable;

public class UrlModel implements Parcelable {
	private String url;
	private long uid;

	public UrlModel(String url, int uid) {
		this.url = url;
		this.uid = uid;
	}

	public UrlModel(DbUrl dbUrl) {
		this.url = dbUrl.getUrl();
		this.uid = dbUrl.getUid();
	}

	public UrlModel(String url) {
		this.url = url;
		this.uid = -1;
	}

	public String getUrl() {
		return url;
	}

	public long getUid() {
		return uid;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.url);
		dest.writeLong(this.uid);
	}

	protected UrlModel(Parcel in) {
		this.url = in.readString();
		this.uid = in.readInt();
	}

	public static final Parcelable.Creator<UrlModel> CREATOR = new Parcelable.Creator<UrlModel>() {
		@Override
		public UrlModel createFromParcel(Parcel source) {
			return new UrlModel(source);
		}

		@Override
		public UrlModel[] newArray(int size) {
			return new UrlModel[size];
		}
	};
}
