/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaEditorOpener {

	public static IEditorPart open(Match match, int offset, int length, boolean activate) throws PartInitException, JavaModelException {
		IEditorPart editor = null;
		Object element = match.getElement();
		if (element instanceof IJavaElement) {
			editor = JavaUI.openInEditor((IJavaElement) element);
		}
		if (editor != null && activate)
			editor.getEditorSite().getPage().activate(editor);
		if (editor instanceof ITextEditor) {
			ITextEditor textEditor = (ITextEditor) editor;
			textEditor.selectAndReveal(offset, length);
		} else if (editor != null) {
			if (element instanceof IFile) {
				IFile file = (IFile) element;
				showWithMarker(editor, file, offset, length);
			}
		}
		return editor;
	}

	private static void showWithMarker(IEditorPart editor, IFile file, int offset, int length) {
		try {
			IMarker marker = file.createMarker(NewSearchUI.SEARCH_MARKER);
			HashMap<String, Integer> attributes = new HashMap<>(4);
			attributes.put(IMarker.CHAR_START, Integer.valueOf(offset));
			attributes.put(IMarker.CHAR_END, Integer.valueOf(offset + length));
			marker.setAttributes(attributes);
			IDE.gotoMarker(editor, marker);
			marker.delete();
		} catch (CoreException e) {
		}
	}

}
