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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.*;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SearchGoToAction extends Action {
	
	public SearchGoToAction() {
		super();
	}

	public void run() {
		try {
			ISearchResultView view = SearchUI.getSearchResultView();
			ISelection selection = view.getSelection();
			Object element = null;
			if (selection instanceof IStructuredSelection)
				element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof ISearchResultViewEntry) {
				ISearchResultViewEntry entry = (ISearchResultViewEntry) element;
				element = entry.getGroupByKey();
				if (element instanceof IJavaElement) {
					IEditorPart editor =
						JavaUI.openInEditor((IJavaElement) element);
					IDE.gotoMarker(editor, entry.getSelectedMarker());
				} else if (element instanceof IPluginObject) {
					IPluginObject object = (IPluginObject) element;
					if (object instanceof IPluginBase) {
						ManifestEditor.openPluginEditor((IPluginBase) object);
					} else {
						ManifestEditor.openPluginEditor(
							object.getPluginBase(),
							object,
							entry.getSelectedMarker());
					}
				}
			}
		} catch (PartInitException e) {
		} catch (JavaModelException e) {
		}
	}


}
