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

import org.eclipse.jface.action.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.ui.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.dnd.Clipboard;

public interface IPDEEditorPage extends IEditorPart, IFormPage {

boolean contextMenuAboutToShow(IMenuManager manager);
IAction getAction(String id);
	public IContentOutlinePage getContentOutlinePage();
	public IPropertySheetPage getPropertySheetPage();
void openTo(Object object);
boolean performGlobalAction(String id);
void update();
boolean canPaste(Clipboard clipboard);
}
