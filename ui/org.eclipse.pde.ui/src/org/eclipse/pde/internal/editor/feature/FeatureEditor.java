package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
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
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.model.Plugin;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.dialogs.*;

public class FeatureEditor extends PDEMultiPageXMLEditor {
	public static final String FEATURE_PAGE = "ComponentPage";
	public static final String INFO_PAGE = "InfoPage";
	public static final String REFERENCE_PAGE = "ReferencePage";
	public static final String SOURCE_PAGE = "SourcePage";
	public static final String UNRESOLVED_TITLE = "FeatureEditor.Unresolved.title";
	public static final String VERSION_TITLE = "FeatureEditor.Version.title";
	public static final String VERSION_MESSAGE = "FeatureEditor.Version.message";
	public static final String VERSION_EXISTS = "FeatureEditor.Version.exists";
	public static final String UNRESOLVED_MESSAGE = "FeatureEditor.Unresolved.message";
	public static final String FEATURE_PAGE_TITLE = "FeatureEditor.FeaturePage.title";
	public static final String REFERENCE_PAGE_TITLE = "FeatureEditor.ReferencePage.title";
	public static final String INFO_PAGE_TITLE = "FeatureEditor.InfoPage.title";


public FeatureEditor() {
	super();
}
private void checkStaleReferences(IFeatureModel model) {
	IFeature feature = model.getFeature();
	Vector unresolved = new Vector();

	IFeaturePlugin[] plugins = feature.getPlugins();
	for (int i = 0; i < plugins.length; i++) {
		IFeaturePlugin reference = plugins[i];
		if (feature.getReferencedModel(reference) == null) {
			unresolved.add(reference);
		}
	}
	if (unresolved.size() > 0 && model.isEditable()) {
		try {
			for (int i = 0; i < unresolved.size(); i++) {
				IFeaturePlugin ref = (IFeaturePlugin) unresolved.elementAt(i);
				feature.removePlugin((IFeaturePlugin) ref);
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
protected Object createModel(Object input) throws CoreException {
	if (input instanceof IFile) return createResourceModel((IFile)input);
	return null;
}
protected void createPages() {
	firstPageId = FEATURE_PAGE;
	formWorkbook.setFirstPageSelected(false);
	FeatureFormPage componentPage =
		new FeatureFormPage(this, PDEPlugin.getResourceString(FEATURE_PAGE_TITLE));
	FeatureReferencePage referencePage =
		new FeatureReferencePage(
			componentPage,
			PDEPlugin.getResourceString(REFERENCE_PAGE_TITLE));
	InfoFormPage infoPage = 
		new InfoFormPage(componentPage,
			PDEPlugin.getResourceString(INFO_PAGE_TITLE));
	addPage(FEATURE_PAGE, componentPage);
	addPage(INFO_PAGE, infoPage);
	addPage(REFERENCE_PAGE, referencePage);
	addPage(SOURCE_PAGE, new FeatureSourcePage(this));
}
private IFeatureModel createResourceModel(IFile file) throws CoreException {
	InputStream stream = null;
	stream = file.getContents(false);

	IModelProvider provider = PDEPlugin.getDefault().getWorkspaceModelManager();
	provider.connect(file, this);
	IFeatureModel model = (IFeatureModel) provider.getModel(file, this);
	boolean cleanModel = true;
	try {
		model.load(stream, false);
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
/*
	IFeatureModel model = (IFeatureModel) getModel();
	IFeature component = model.getFeature();
	String version = component.getVersion();
	String id = component.getId();
	IFile file = (IFile) model.getUnderlyingResource();
	IContainer parent = file.getParent();
	if (parent instanceof IFolder) {
		String name = parent.getName();
		String expectedName = id + "_"+ version;
		if (name.equals(expectedName)==false) {
			MessageDialog.openInformation(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(VERSION_TITLE),
				PDEPlugin.getFormattedMessage(VERSION_MESSAGE, name));
		}
	}
*/
}

public IPDEEditorPage getHomePage() {
	return getPage(FEATURE_PAGE);
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
	if (name.equals("feature.xml")) return true;
	return false;
}
protected boolean updateModel() {
	IFeatureModel model = (IFeatureModel) getModel();
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	boolean cleanModel = true;
	String text = document.get();
	try {
		InputStream stream = new ByteArrayInputStream(text.getBytes("UTF8"));
		try {
			model.reload(stream, false);
		} catch (CoreException e) {
			cleanModel = false;
		}
		try {
			stream.close();
		} catch (IOException e) {
		}
	}
	catch (UnsupportedEncodingException e) {
		PDEPlugin.logException(e);
	}
	return cleanModel;
}
public void updateTitle() {
	firePropertyChange(IWorkbenchPart.PROP_TITLE);
}
}
