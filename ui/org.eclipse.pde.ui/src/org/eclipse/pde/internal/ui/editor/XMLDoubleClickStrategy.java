/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.*;

public class XMLDoubleClickStrategy implements ITextDoubleClickStrategy {
	protected ITextViewer fText;

	public void doubleClicked(ITextViewer part) {
		int pos = part.getSelectedRange().x;
		if (pos > 0) {
			fText = part;
			selectWord(pos);
		}
	}
	
	protected boolean selectWord(int caretPos) {

		IDocument doc = fText.getDocument();
		int startPos, endPos;

		try {

			int pos = caretPos;
			char c;

			while (pos >= 0) {
				c = doc.getChar(pos);
				if (!Character.isJavaIdentifierPart(c) && c != '.')
					break;
				--pos;
			}

			startPos = pos;

			pos = caretPos;
			int length = doc.getLength();

			while (pos < length) {
				c = doc.getChar(pos);
				if (!Character.isJavaIdentifierPart(c) && c != '.')
					break;
				++pos;
			}

			endPos = pos;
			selectRange(startPos, endPos);
			return true;

		} catch (BadLocationException x) {
		}

		return false;
	}
	
	private void selectRange(int startPos, int stopPos) {
		int offset = startPos+1;
		int length = stopPos - offset;
		fText.setSelectedRange(offset, length);
	}
}
