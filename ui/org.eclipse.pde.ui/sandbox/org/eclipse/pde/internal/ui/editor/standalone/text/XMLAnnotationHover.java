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
package org.eclipse.pde.internal.ui.editor.standalone.text;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.ui.texteditor.*;

public class XMLAnnotationHover implements IAnnotationHover {

	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		String[] messages = getMessagesForLine(sourceViewer, lineNumber);

		if (messages.length == 0)
			return null;

		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < messages.length; i++) {
			buffer.append(messages[i]);
			if (i < messages.length - 1)
				buffer.append(System.getProperty("line.separator"));
		}
		return buffer.toString();
	}

	private String[] getMessagesForLine(ISourceViewer viewer, int line) {
		IDocument document = viewer.getDocument();
		IAnnotationModel model = viewer.getAnnotationModel();

		if (model == null)
			return new String[0];

		ArrayList messages = new ArrayList();

		Iterator iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof MarkerAnnotation) {
				MarkerAnnotation annotation = (MarkerAnnotation) object;
				if (compareRulerLine(model.getPosition(annotation),
					document,
					line)) {
					IMarker marker = annotation.getMarker();
					String message =
						marker.getAttribute(IMarker.MESSAGE, (String) null);
					if (message != null && message.trim().length() > 0)
						messages.add(message);
				}
			}
		}
		return (String[]) messages.toArray(new String[messages.size()]);
	}

	private boolean compareRulerLine(
		Position position,
		IDocument document,
		int line) {

		try {
			if (position.getOffset() > -1 && position.getLength() > -1) {
				return document.getLineOfOffset(position.getOffset()) == line;
			}
		} catch (BadLocationException e) {
		}
		return false;
	}
}