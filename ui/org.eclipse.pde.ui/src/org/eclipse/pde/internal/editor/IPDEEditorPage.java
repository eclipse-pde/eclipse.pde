package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public interface IPDEEditorPage extends IEditorPart, IFormPage {

boolean contextMenuAboutToShow(IMenuManager manager);
IAction getAction(String id);
	public IContentOutlinePage getContentOutlinePage();
	public IPropertySheetPage getPropertySheetPage();
void openTo(Object object);
void performGlobalAction(String id);
void update();
}
