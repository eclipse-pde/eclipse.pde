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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.update.ui.forms.internal.AbstractSectionForm;
import org.eclipse.update.ui.forms.internal.IFormPage;

public abstract class PDEFormPage
	extends EditorPart
	implements IPDEEditorPage {
	private AbstractSectionForm form;
	private Control control;
	private PDEMultiPageEditor editor;
	private IContentOutlinePage contentOutlinePage;
	private IPropertySheetPage propertySheetPage;
	private org.eclipse.jface.viewers.ISelection selection;

	public PDEFormPage(PDEMultiPageEditor editor, String title) {
		this(editor, title, null);
	}

	public PDEFormPage(
		PDEMultiPageEditor editor,
		String title,
		AbstractSectionForm form) {
		this.editor = editor;
		if (form == null)
			form = createForm();
		this.form = form;
		if (isWhiteBackground())
			form.setHeadingImage(PDEPluginImages.get(PDEPluginImages.IMG_FORM_BANNER));
		setTitle(title);
	}

	private boolean isWhiteBackground() {
		Color bg = form.getFactory().getBackgroundColor();
		return (bg.getRed() == 255 && bg.getGreen() == 255 && bg.getBlue() == 255);
	}
	public boolean becomesInvisible(IFormPage newPage) {
		if (getModel() instanceof IModel && ((IModel) getModel()).isEditable())
			form.commitChanges(false);
		getEditor().setSelection(new StructuredSelection());
		if (newPage instanceof PDESourcePage) {
			getEditor().updateDocument();
		}
		return true;
	}
	public void becomesVisible(IFormPage oldPage) {
		update();
		setFocus();
		getEditor().getContributor().updateSelectableActions(null);
	}
	public boolean contextMenuAboutToShow(IMenuManager manager) {
		return true;
	}
	public abstract IContentOutlinePage createContentOutlinePage();
	public void createControl(Composite parent) {
		createPartControl(parent);
	}
	protected abstract AbstractSectionForm createForm();

	public void createPartControl(Composite parent) {
		control = form.createControl(parent);
		control.setMenu(editor.getContextMenu());
		form.initialize(getModel());
	}
	public IPropertySheetPage createPropertySheetPage() {
		return null;
	}
	public void dispose() {
		form.dispose();
		if (contentOutlinePage != null)
			contentOutlinePage.dispose();
		if (propertySheetPage != null)
			propertySheetPage.dispose();
	}
	public void doSave(IProgressMonitor monitor) {
	}
	public void doSaveAs() {
	}

	public IAction getAction(String id) {
		return editor.getAction(id);
	}
	public IContentOutlinePage getContentOutlinePage() {
		if (contentOutlinePage == null
			|| (contentOutlinePage.getControl() != null
				&& contentOutlinePage.getControl().isDisposed())) {
			contentOutlinePage = createContentOutlinePage();
		}
		return contentOutlinePage;
	}
	public Control getControl() {
		return control;
	}
	public PDEMultiPageEditor getEditor() {
		return editor;
	}
	public AbstractSectionForm getForm() {
		return form;
	}
	public String getLabel() {
		return getTitle();
	}
	public Object getModel() {
		return getEditor().getModel();
	}
	public IPropertySheetPage getPropertySheetPage() {
		if (propertySheetPage == null
			|| (propertySheetPage.getControl() != null
				&& propertySheetPage.getControl().isDisposed())) {
			propertySheetPage = createPropertySheetPage();
		}
		return propertySheetPage;
	}
	public org.eclipse.jface.viewers.ISelection getSelection() {
		return selection;
	}
	public String getStatusText() {
		IEditorInput input = getEditor().getEditorInput();
		String status = "";

		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			status = file.getFullPath().toString() + IPath.SEPARATOR;
		}
		status += getTitle();

		return status;
	}
	public void gotoMarker(IMarker marker) {
	}
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
	}
	public boolean isDirty() {
		return false;
	}
	public boolean isSaveAsAllowed() {
		return false;
	}
	public boolean isSource() {
		return false;
	}
	public boolean isVisible() {
		return getEditor().getCurrentPage() == this;
	}
	public void openTo(Object object) {
		getForm().expandTo(object);
	}
	public boolean performGlobalAction(String id) {
		return getForm().doGlobalAction(id);
	}
	public void setFocus() {
		getForm().setFocus();
	}
	public void setSelection(ISelection newSelection) {
		selection = newSelection;
		getEditor().setSelection(selection);
	}
	public String toString() {
		return getTitle();
	}
	public void update() {
		form.update();
	}

	public boolean canPaste(Clipboard clipboard) {
		return form.canPaste(clipboard);
	}
}
