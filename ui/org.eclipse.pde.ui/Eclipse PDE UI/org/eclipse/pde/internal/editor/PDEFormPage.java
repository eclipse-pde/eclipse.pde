package org.eclipse.pde.internal.editor;

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.core.runtime.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.*;


public abstract class PDEFormPage extends EditorPart implements IPDEEditorPage {
	private Form form;
	private Control control;
	private PDEMultiPageEditor editor;
	private IContentOutlinePage contentOutlinePage;
	private IPropertySheetPage propertySheetPage;
	private org.eclipse.jface.viewers.ISelection selection;

public PDEFormPage(PDEMultiPageEditor editor, String title) {
	this.editor = editor;
	form = createForm();
	form.setHeadingImage(PDEPluginImages.get(PDEPluginImages.IMG_FORM_WIZ));
	setTitle(title);
}
public boolean becomesInvisible(IFormPage newPage) {
	if (getModel() instanceof IModel &&
		((IModel)getModel()).isEditable())
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
}
public boolean contextMenuAboutToShow(IMenuManager manager) {
	return true;
}
public abstract IContentOutlinePage createContentOutlinePage();
public void createControl(Composite parent) {
	createPartControl(parent);
}
protected abstract Form createForm();
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
	if (contentOutlinePage!=null) contentOutlinePage.dispose();
	if (propertySheetPage!=null) propertySheetPage.dispose();
}
public void doSave(IProgressMonitor monitor) {
}
public void doSaveAs() {}
public IAction getAction(String id) {
	return editor.getAction(id);
}
public IContentOutlinePage getContentOutlinePage() {
	if (contentOutlinePage == null
		|| (contentOutlinePage.getControl() != null && contentOutlinePage.getControl().isDisposed())) {
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
public Form getForm() {
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
	String status="";

	if (input instanceof IFileEditorInput) {
		IFile file = ((IFileEditorInput)input).getFile();
		status = file.getFullPath().toString() + IPath.SEPARATOR;
	}
	status += getTitle();
		
	return status;
}
public void gotoMarker(IMarker marker) {}
public void init(IEditorSite site, IEditorInput input) throws PartInitException {
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
	return getEditor().getCurrentPage()==this;
}
public void openTo(Object object) {
	getForm().expandTo(object);
}
public void performGlobalAction(String id) {
	getForm().doGlobalAction(id);
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
}
