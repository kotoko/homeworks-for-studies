package mixingmachine.parser;

/**
 * Wyjątek rzucany przez parser, gdy nie powiodła się próba przeczytania
 * plików konfiguracyjnych.
 */
public class InvalidConfigIOException extends InvalidConfigException {
	public InvalidConfigIOException(String msg) {
		super(msg);
	}

	public InvalidConfigIOException(String s, String msg) {
		super(s, msg);
	}

	public InvalidConfigIOException(String s, Throwable throwable, String msg) {
		super(s, throwable, msg);
	}

	public InvalidConfigIOException(Throwable throwable, String msg) {
		super(throwable, msg);
	}

	public InvalidConfigIOException(String s, Throwable throwable, boolean b, boolean b1, String msg) {
		super(s, throwable, b, b1, msg);
	}

	@Override
	public String toString() {
		return "InvalidConfigIOException{" +
				"IOException='" + getCause() + '\'' +
				'}';
	}
}
