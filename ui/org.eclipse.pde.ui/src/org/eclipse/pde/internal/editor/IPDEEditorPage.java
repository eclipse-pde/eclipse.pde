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
