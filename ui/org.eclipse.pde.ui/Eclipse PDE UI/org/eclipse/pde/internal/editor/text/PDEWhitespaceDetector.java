package org.eclipse.pde.internal.editor.text;

import org.eclipse.jface.text.rules.IWhitespaceDetector;


public class PDEWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return Character.isWhitespace(c);
	}
}
