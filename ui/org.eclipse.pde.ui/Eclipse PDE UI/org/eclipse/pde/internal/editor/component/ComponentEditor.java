package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.model.jars.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.model.Plugin;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.dialogs.*;

public class ComponentEditor extends PDEMultiPageXMLEditor {
	public static final String COMPONENT_PAGE = "ComponentPage";
	public static final String REFERENCE_PAGE = "ReferencePage";
	public static final String SOURCE_PAGE = "SourcePage";
	public static final String UNRESOLVED_TITLE = "ComponentEditor.Unresolved.title";
	public static final String VERSION_TITLE = "ComponentEditor.Version.title";
	public static final String VERSION_MESSAGE = "ComponentEditor.Version.message";
	public static final String UNRESOLVED_MESSAGE = "ComponentEditor.Unresolved.message";
	public static final String COMPONENT_PAGE_TITLE = "ComponentEditor.ComponentPage.title";
	public static final String REFERENCE_PAGE_TITLE = "ComponentEditor.ReferencePage.title";


public ComponentEditor() {
	super();
}
private void checkStaleReferences(IComponentModel model) {
	IComponent component = model.getComponent();
	Vector unresolved = new Vector();

	IComponentPlugin[] plugins = component.getPlugins();
	for (int i = 0; i < plugins.length; i++) {
		IComponentReference reference = plugins[i];
		if (component.getReferencedModel(reference) == null) {
			unresolved.add(reference);
		}
	}
	IComponentFragment[] fragments = component.getFragments();
	for (int i = 0; i < fragments.length; i++) {
		IComponentReference reference = fragments[i];
		if (component.getReferencedModel(reference) == null) {
			unresolved.add(reference);
		}
	}
	if (unresolved.size() > 0 && model.isEditable()) {
		try {
			for (int i = 0; i < unresolved.size(); i++) {
				IComponentReference ref = (IComponentReference) unresolved.elementAt(i);
				if (ref instanceof IComponentPlugin) {
					component.removePlugin((IComponentPlugin) ref);
				} else
					component.removeFragment((IComponentFragment) ref);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		MessageDialog.openWarning(
			PDEPlugin.getActiveWorkbenchShell(),
			PDEPlugin.getResourceString(UNRESOLVED_TITLE),
			PDEPlugin.getResourceString(UNRESOLVED_MESSAGE));
	}
}
protected Object createModel(Object input) {
	if (input instanceof IFile) return createResourceModel((IFile)input);
	return null;
}
protected void createPages() {
	firstPageId = COMPONENT_PAGE;
	formWorkbook.setFirstPageSelected(false);
	ComponentFormPage componentPage =
		new ComponentFormPage(this, PDEPlugin.getResourceString(COMPONENT_PAGE_TITLE));
	ComponentReferencePage referencePage =
		new ComponentReferencePage(
			componentPage,
			PDEPlugin.getResourceString(REFERENCE_PAGE_TITLE));
	addPage(COMPONENT_PAGE, componentPage);
	addPage(REFERENCE_PAGE, referencePage);
	addPage(SOURCE_PAGE, new ComponentSourcePage(this));
}
private IComponentModel createResourceModel(IFile file) {
	InputStream stream = null;
	try {
		stream = file.getContents(false);
	} catch (CoreException e) {
		return null;
	}
	IModelProvider provider = PDEPlugin.getDefault().getWorkspaceModelManager();
	provider.connect(file, this);
	IComponentModel model = (IComponentModel) provider.getModel(file, this);
	boolean cleanModel = true;
	try {
		model.load(stream);
		checkStaleReferences(model);
	} catch (CoreException e) {
		cleanModel = false;
	}
	try {
		stream.close();
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
	return cleanModel ? model : null;
}
public void dispose() {
	super.dispose();
	IModelProvider provider = PDEPlugin.getDefault().getWorkspaceModelManager();
	IModel model = (IModel)getModel();
	provider.disconnect(model.getUnderlyingResource(), this);
}
public void doSave(IProgressMonitor monitor) {
	super.doSave(monitor);
	IComponentModel model = (IComponentModel) getModel();
	IComponent component = model.getComponent();
	String version = component.getVersion();
	IFile file = (IFile) model.getUnderlyingResource();
	IContainer parent = file.getParent();
	if (parent instanceof IFolder) {
		String name = parent.getName();
		int uloc = name.lastIndexOf('_');
		if (uloc != -1) {
			String dirVersion = name.substring(uloc + 1);
			if (dirVersion.equals(version) == false) {
				MessageDialog.openError(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(VERSION_TITLE),
					PDEPlugin.getFormattedMessage(VERSION_MESSAGE, name));
				/*
				String newName = name.substring(0, uloc + 1) + version;
				IPath newPath = parent.getFullPath().removeLastSegments(1).append(newName);
				IFolder folder = file.getWorkspace().getRoot().getFolder(newPath);
				if (folder.exists()) {
				
					MessageDialog.openError(
						PDEPlugin.getActiveWorkbenchShell(),
						"Save Error",
						"Folder \""
							+ newName
							+ "\" already exists. Component version and folder name will not be in sync");
							
				} else {
					try {
						folder = (IFolder) parent;
						folder.move(newPath, false, true, monitor);
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
				*/
			}
		}
	}
}
public IPDEEditorPage getHomePage() {
	return getPage(COMPONENT_PAGE);
}
protected String getSourcePageId() {
	return SOURCE_PAGE;
}
public String getTitle() {
	return super.getTitle();
}
protected boolean isModelDirty(Object model) {
	return model != null
		&& model instanceof IEditable
		&& model instanceof IModel
		&& ((IModel) model).isEditable()
		&& ((IEditable) model).isDirty();
}
protected boolean isValidContentType(IEditorInput input) {
	String name = input.getName().toLowerCase();
	if (name.equals("install.xml")) return true;
	return false;
}
protected boolean updateModel() {
	IComponentModel model = (IComponentModel) getModel();
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	String text = document.get();
	InputStream stream = new ByteArrayInputStream(text.getBytes());
	boolean cleanModel = true;
	try {
		model.reload(stream);
	} catch (CoreException e) {
		cleanModel = false;
	}
	try {
		stream.close();
	} catch (IOException e) {
	}
	return cleanModel;
}
public void updateTitle() {
	firePropertyChange(IWorkbenchPart.PROP_TITLE);
}
}
