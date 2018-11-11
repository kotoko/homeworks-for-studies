package jnp2.android.kotoko.czytnikrss;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class MessageModel implements Parcelable {
	private String messageTitle;
	private Date messageCreated;
	private String messageUrl;
	private String messageContent;


	public MessageModel(DbMessage dbMessage) {
		this.messageTitle = dbMessage.getTitle();
		this.messageCreated = new Date(dbMessage.getCreatedDate());
		this.messageUrl = dbMessage.getUrl();
		this.messageContent = dbMessage.getContent();
	}

	public MessageModel(String messageTitle, Date messageCreated, String messageUrl, String messageContent) {
		this.messageTitle = messageTitle;
		this.messageCreated = messageCreated;
		this.messageUrl = messageUrl;
		this.messageContent = messageContent;
	}

	public String getMessageTitle() {
		return messageTitle;
	}

	public Date getMessageCreated() {
		return messageCreated;
	}

	public String getMessageUrl() {
		return messageUrl;
	}

	public String getMessageContent() {
		return messageContent;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.messageTitle);
		dest.writeLong(this.messageCreated != null ? this.messageCreated.getTime() : -1);
		dest.writeString(this.messageUrl);
		dest.writeString(this.messageContent);
	}

	protected MessageModel(Parcel in) {
		this.messageTitle = in.readString();
		long tmpMessageCreated = in.readLong();
		this.messageCreated = tmpMessageCreated == -1 ? null : new Date(tmpMessageCreated);
		this.messageUrl = in.readString();
		this.messageContent = in.readString();
	}

	public static final Parcelable.Creator<MessageModel> CREATOR = new Parcelable.Creator<MessageModel>() {
		@Override
		public MessageModel createFromParcel(Parcel source) {
			return new MessageModel(source);
		}

		@Override
		public MessageModel[] newArray(int size) {
			return new MessageModel[size];
		}
	};
}
