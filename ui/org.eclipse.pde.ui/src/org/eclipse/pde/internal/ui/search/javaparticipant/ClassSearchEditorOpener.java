/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.javaparticipant;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;


public class ClassSearchEditorOpener {

	public static IEditorPart open(Match match) throws PartInitException {
		IEditorPart editorPart = null;
		Object element = match.getElement();
		if (element instanceof SearchHit)
			element = ((SearchHit)element).getHitElement();
		editorPart = ManifestEditor.open(element, true);
		if (editorPart instanceof ManifestEditor) {
			ManifestEditor editor = (ManifestEditor)editorPart;
			IDocument doc = editor.getDocument(match);
			if (doc != null) {
				editor.openToSourcePage(match.getElement(), match.getOffset(), match.getLength());
			}
		}
		return editorPart;
	}
}