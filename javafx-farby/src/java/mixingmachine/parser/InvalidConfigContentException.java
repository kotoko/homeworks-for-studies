package mixingmachine.parser;

/**
 * Wyjątek rzucany przez parser, gdy dane w plikach konfiguracyjnych są niepoprawne.
 */
public class InvalidConfigContentException extends InvalidConfigException {
	public InvalidConfigContentException(String msg) {
		super(msg);
	}

	public InvalidConfigContentException(String s, String msg) {
		super(s, msg);
	}

	public InvalidConfigContentException(String s, Throwable throwable, String msg) {
		super(s, throwable, msg);
	}

	public InvalidConfigContentException(Throwable throwable, String msg) {
		super(throwable, msg);
	}

	public InvalidConfigContentException(String s, Throwable throwable, boolean b, boolean b1, String msg) {
		super(s, throwable, b, b1, msg);
	}

	@Override
	public String toString() {
		return "InvalidConfigContentException{" +
				"msg='" + msg + '\'' +
				'}';
	}
}
