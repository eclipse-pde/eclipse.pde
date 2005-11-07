package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.rules.IWordDetector;

public class ManifestHeaderDetector implements IWordDetector {
	public boolean isWordStart(char c) {
		return isValidChar(c);
	}
	public boolean isWordPart(char c) {
		return isValidChar(c) || c == '-' || c == '_';
	}
	private boolean isValidChar(char c) {
		return ((c >= 'A' && c <= 'Z') ||
				(c >= 'a' && c <= 'z') ||
				(c >= '0' && c <= '9'));
	}
}