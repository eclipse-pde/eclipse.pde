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

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.update.ui.forms.internal.IFormPage;

public abstract class PDESourcePage
	extends AbstractTextEditor
	implements IPDEEditorPage {
	public static final String PAGE_TITLE = "SourcePage.title";
	public static final String ERROR_MESSAGE = "SourcePage.errorMessage";
	public static final String ERROR_TITLE = "SourcePage.errorTitle";
	private IContentOutlinePage outlinePage;
	private boolean errorMode;
	private PDEMultiPageEditor editor;
	private boolean modelNeedsUpdating = false;
	private Control control;
	private IDocumentListener documentListener;

	class DocumentListener implements IDocumentListener {
		public void documentAboutToBeChanged(DocumentEvent e) {
		}
		public void documentChanged(DocumentEvent e) {
			if (isVisible()) {
				setModelNeedsUpdating(true);
			}
		}
	}

	public PDESourcePage(PDEMultiPageEditor editor) {
		this.editor = editor;
		initializeDocumentListener();
		setEditorContextMenuId("#PDESourcePageEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#PDESourcePageRulerContext"); //$NON-NLS-1$
	}
	public boolean becomesInvisible(IFormPage newPage) {
	if (errorMode || isModelNeedsUpdating()) {
			boolean cleanModel = getEditor().updateModel();
			if (cleanModel)
				setModelNeedsUpdating(false);
			boolean valid = getEditor().validateModelSemantics();
			if (cleanModel == false || valid==false) {
				warnErrorsInSource();
				errorMode = true;
				return false;
			}
			errorMode = false;
		}
		//getSite().setSelectionProvider(getEditor());
		return true;
	}
	
	public void becomesVisible(IFormPage oldPage) {
		setModelNeedsUpdating(false);
		if (oldPage instanceof PDEFormPage) {
			selectObjectRange(((PDEFormPage) oldPage).getSelection());
		}
		//getSite().setSelectionProvider(getSelectionProvider());
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
		
		WorkbenchHelp.setHelp(control, IHelpContextIds.MANIFEST_SOURCE_PAGE);

		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		document.addDocumentListener(documentListener);
		errorMode = !getEditor().isModelCorrect(getEditor().getModel());
		unregisterGlobalActions();
		// Important - must reset the provider to the multi-page
		// editor.
		// See 32622
		getSite().setSelectionProvider(getEditor());
	}
	
	private void unregisterGlobalActions() {
		// A workaround for bug 27539
		// Unregistering important actions from
		// the key binding service allows
		// global actions to handle accelerators 
		// properly
		IKeyBindingService service = getEditor().getSite().getKeyBindingService();
		service.unregisterAction(getAction(ActionFactory.DELETE.getId()));
		service.unregisterAction(getAction(ActionFactory.UNDO.getId()));
		service.unregisterAction(getAction(ActionFactory.REDO.getId()));
		service.unregisterAction(getAction(ActionFactory.CUT.getId()));
		service.unregisterAction(getAction(ActionFactory.COPY.getId()));
		service.unregisterAction(getAction(ActionFactory.PASTE.getId()));
		service.unregisterAction(getAction(ActionFactory.SELECT_ALL.getId()));
		service.unregisterAction(getAction(ActionFactory.FIND.getId()));
	}

	public void dispose() {
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		if (document != null)
			document.removeDocumentListener(documentListener);
		if (outlinePage != null) {
			outlinePage.dispose();
			outlinePage = null;
		}
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
		if (editor.getEditorInput() != getEditorInput())
			editor.setInput(getEditorInput());
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
		return editor.getCurrentPage() == this;
	}

	public void openTo(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
		}
	}

protected void selectObjectRange(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			int start = 0;
			int stop = 0;
			// Compute the entire range
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				ISourceObject sobj = null;

				if (obj instanceof ISourceObject)
					sobj = (ISourceObject) obj;
				if (obj instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) obj;
					sobj =
						(ISourceObject) adaptable.getAdapter(
							ISourceObject.class);
				}
				if (sobj != null) {
					if (start == 0) {
						start = sobj.getStartLine() - 1;
					} else {
						start = Math.min(start, sobj.getStartLine() - 1);
					}
					stop = Math.max(stop, sobj.getStopLine() - 1);
				}
			}
			if (start > 0) {
				IDocument document =
					getDocumentProvider().getDocument(getEditorInput());
				if (document == null)
					return;
				try {
					//int offset = editor.getRealStartOffset(document.getLineOffset(start-1));
					int startOffset = document.getLineOffset(start);
					int stopOffset =
						document.getLineOffset(stop)
							+ document.getLineLength(stop);
					selectAndReveal(startOffset, stopOffset - startOffset);
				} catch (BadLocationException e) {
				}
			}
		}
	}

	public boolean performGlobalAction(String id) {
		return true;
	}
	public String toString() {
		return getTitle();
	}
	public void update() {
	}
	protected void warnErrorsInSource() {
		Display.getCurrent().beep();
		String title = editor.getSite().getRegisteredName();
		MessageDialog.openError(
			PDEPlugin.getActiveWorkbenchShell(),
			title,
			PDEPlugin.getResourceString(ERROR_MESSAGE));
	}

	protected void createActions() {
		PDEEditorContributor contributor = getEditor().getContributor();
		super.createActions();
		setAction(ActionFactory.SAVE.getId(), contributor.getSaveAction());
	}

	public void close(boolean save) {
		editor.close(save);
	}

	public boolean canPaste(Clipboard clipboard) {
		return true;
	}
	public void setFocus() {
		getControl().setFocus();
	}

	public boolean containsError() {
		return errorMode;
	}

	protected boolean isModelNeedsUpdating() {
		return modelNeedsUpdating;
	}

	protected void setModelNeedsUpdating(boolean modelNeedsUpdating) {
		this.modelNeedsUpdating= modelNeedsUpdating;
	}

	protected void setDocumentListener(IDocumentListener documentListener) {
		this.documentListener= documentListener;
	}

	protected void initializeDocumentListener() {
		setDocumentListener(new DocumentListener());
	}

}
