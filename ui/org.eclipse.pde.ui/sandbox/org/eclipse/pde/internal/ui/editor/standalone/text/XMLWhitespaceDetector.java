package org.eclipse.pde.internal.ui.editor.standalone.text;

import org.eclipse.jface.text.rules.*;

public class XMLWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
