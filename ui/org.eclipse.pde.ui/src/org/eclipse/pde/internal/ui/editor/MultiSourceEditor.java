/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;

public abstract class MultiSourceEditor extends PDEFormEditor {
	protected void addSourcePage(String contextId) {
		InputContext context = fInputContextManager.findContext(contextId);
		if (context == null)
			return;
		PDESourcePage sourcePage;
		// Don't duplicate
		if (findPage(contextId) != null)
			return;
		sourcePage = createSourcePage(this, contextId, context.getInput().getName(), context.getId());
		sourcePage.setInputContext(context);
		try {
			addPage(sourcePage, context.getInput());
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void removePage(String pageId) {
		IFormPage page = findPage(pageId);
		if (page == null)
			return;
		if (page.isDirty()) {
			// need to ask the user about this
		} else {
			removePage(page.getIndex());
			if (!page.isEditor())
				page.dispose();
		}
	}

	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new GenericSourcePage(editor, title, name);
	}
}
