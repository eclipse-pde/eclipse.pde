package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.rules.IWordDetector;

public class ManifestAssignmentDetector implements IWordDetector {
	public boolean isWordStart(char c) {
		return ':' == c;
	}
	public boolean isWordPart(char c) {
		return false;
	}
}