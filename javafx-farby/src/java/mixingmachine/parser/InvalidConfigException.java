package mixingmachine.parser;

/**
 * Klasa wyjątków rzucanych przez parser.
 */
public abstract class InvalidConfigException extends Throwable {
	public final String msg;

	public InvalidConfigException(String msg) {
		this.msg = msg;
	}

	public InvalidConfigException(String s, String msg) {
		super(s);
		this.msg = msg;
	}

	public InvalidConfigException(String s, Throwable throwable, String msg) {
		super(s, throwable);
		this.msg = msg;
	}

	public InvalidConfigException(Throwable throwable, String msg) {
		super(throwable);
		this.msg = msg;
	}

	public InvalidConfigException(String s, Throwable throwable, boolean b, boolean b1, String msg) {
		super(s, throwable, b, b1);
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "InvalidConfigException{" +
				"msg='" + msg + '\'' +
				'}';
	}
}
