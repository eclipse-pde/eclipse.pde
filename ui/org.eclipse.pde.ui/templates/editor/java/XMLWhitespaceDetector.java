package $packageName$;

import org.eclipse.jface.text.rules.IWhitespaceDetector;


public class XMLWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return Character.isWhitespace(c);
	}
}
