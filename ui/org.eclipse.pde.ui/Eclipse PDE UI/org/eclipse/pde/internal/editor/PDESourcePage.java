package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.internal.plugins.*;
import org.eclipse.ui.views.properties.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.views.contentoutline.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.dialogs.*;

public abstract class PDESourcePage extends AbstractTextEditor implements IPDEEditorPage {
	public static final String PAGE_TITLE = "SourcePage.title";
	public static final String ERROR_MESSAGE = "SourcePage.errorMessage";
	public static final String ERROR_TITLE = "SourcePage.errorTitle";
	private IContentOutlinePage outlinePage;
	private boolean errorMode;
	private PDEMultiPageEditor editor;
	private boolean modelNeedsUpdating=false;
	private Control control;
	private DocumentListener documentListener = new DocumentListener();

	class DocumentListener implements IDocumentListener {
		public void documentAboutToBeChanged(DocumentEvent e) {
		}
		public void documentChanged(DocumentEvent e) {
			if (isVisible()) {
				modelNeedsUpdating = true;
			}
		}
	}

public PDESourcePage(PDEMultiPageEditor editor) {
	this.editor = editor;
}
public boolean becomesInvisible(IFormPage newPage) {
	if (errorMode || modelNeedsUpdating) {
		boolean cleanModel = getEditor().updateModel();
		if (cleanModel==false) {
			warnErrorsInSource();
			return false;
		}
		modelNeedsUpdating = false;
		errorMode = false;
	}
	getSite().setSelectionProvider(getEditor());
	return true;
}
public void becomesVisible(IFormPage oldPage) {
	modelNeedsUpdating=false;
	getSite().setSelectionProvider(getSelectionProvider());
}
public boolean contextMenuAboutToShow(IMenuManager manager) {
	return false;
}
public abstract IContentOutlinePage createContentOutlinePage();
public void createControl(Composite parent) {
	createPartControl(parent);
}
public void createPartControl(Composite parent) {
	super.createPartControl(parent);
	Control[] children = parent.getChildren();
	control = children[children.length - 1];

	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	document.addDocumentListener(documentListener);
	errorMode = !getEditor().isModelCorrect(getEditor().getModel());
}
public void dispose() {
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	if (document != null)
		document.removeDocumentListener(documentListener);
	super.dispose();
}
protected void firePropertyChange(int type) {
	if (type == PROP_DIRTY) {
		getEditor().fireSaveNeeded();
	} else
		super.firePropertyChange(type);
}
public IContentOutlinePage getContentOutlinePage() {
	if (outlinePage == null) {
		outlinePage = createContentOutlinePage();
	}
	return outlinePage;
}
public Control getControl() {
	return control;
}
public PDEMultiPageEditor getEditor() {
	return editor;
}
public String getLabel() {
	return getTitle();
}
public IPropertySheetPage getPropertySheetPage() {
	return null;
}
public String getTitle() {
	return PDEPlugin.getResourceString(PAGE_TITLE);
}
public void init(IEditorSite site, IEditorInput input)
	throws PartInitException {
	setDocumentProvider(getEditor().getDocumentProvider());
	super.init(site, input);
}
public boolean isEditable() {
	return getEditor().isEditable();
}
public boolean isSource() {
	return true;
}
public boolean isVisible() {
	return editor.getCurrentPage()==this;
}
public void openTo(Object object) {
}
public void performGlobalAction(String id) {}
public String toString() {
	return getTitle();
}
public void update() {}
private void warnErrorsInSource() {
	Display.getCurrent().beep();
	MessageDialog.openError(
		PDEPlugin.getActiveWorkbenchShell(),
		PDEPlugin.getResourceString(ERROR_TITLE),
		PDEPlugin.getResourceString(ERROR_MESSAGE));
}

protected void createActions () {
	PDEEditorContributor contributor = getEditor().getContributor();
	super.createActions();
	setAction(ITextEditorActionConstants.SAVE, contributor.getSaveAction());
}

public void close(boolean save) {
	editor.close(save);
}
}
