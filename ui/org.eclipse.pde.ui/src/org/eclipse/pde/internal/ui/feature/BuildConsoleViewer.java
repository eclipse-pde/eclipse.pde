package org.eclipse.pde.internal.ui.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.custom.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.resource.*;

public class BuildConsoleViewer extends TextViewer {
	private int currentInsertLocation = 0;

public BuildConsoleViewer(Composite parent) {
	super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
	//setEditable(false);
	setDocument(new Document());
	StyledText styledText= getTextWidget();
	styledText.setFont(JFaceResources.getTextFont());
	styledText.setForeground(styledText.getDisplay().getSystemColor(SWT.COLOR_BLUE));
}
public void append(String text) {
	text = text + "\n";
	int len = text.length();
	try {
		getDocument().replace(currentInsertLocation, 0, text);
		currentInsertLocation += len;
		revealEndOfDocument();
	} catch (BadLocationException e) {
	}
}
public void clearDocument() {
	IDocument doc = getDocument();
	if (doc != null) {
		doc.set("");
	}
	currentInsertLocation = 0;
}
public void revealEndOfDocument() {
	IDocument doc = getDocument();
	int docLength = doc.getLength();
	if (docLength > 0) {
		revealRange(docLength - 1, 1);
		StyledText widget = getTextWidget();
		widget.setCaretOffset(docLength);

	}
}
}
