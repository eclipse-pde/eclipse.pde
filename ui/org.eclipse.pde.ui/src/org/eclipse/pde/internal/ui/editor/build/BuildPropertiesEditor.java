package org.eclipse.pde.internal.ui.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.core.*;

import java.util.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.core.plugin.Plugin;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.dialogs.*;

public class BuildPropertiesEditor extends PDEMultiPageEditor {
	public static final String BUILD_PAGE_TITLE = "BuildEditor.BuildPage.title";
	public static final String BUILD_PAGE = "BuildPage";
	public static final String SOURCE_PAGE = "SourcePage";


public BuildPropertiesEditor() {
	super();
}
protected Object createModel(Object input) throws CoreException {
	if (input instanceof IFile) return createResourceModel((IFile)input);
	return null;
}

protected void createPages() {
	firstPageId = BUILD_PAGE;
	formWorkbook.setFirstPageSelected(false);
	BuildPage buildPage =
		new BuildPage(this, PDEPlugin.getResourceString(BUILD_PAGE_TITLE));
	addPage(BUILD_PAGE, buildPage);
	addPage(SOURCE_PAGE, new BuildSourcePage(this));
}
private IBuildModel createResourceModel(IFile file) throws CoreException {
	InputStream stream = null;

	stream = file.getContents(false);

	IModelProvider provider = PDEPlugin.getDefault().getWorkspaceModelManager();
	provider.connect(file, this);
	IBuildModel model = (IBuildModel) provider.getModel(file, this);
	try {
		model.load(stream, false);
	} catch (CoreException e) {
	}
	try {
		stream.close();
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
	return model;
}
public void dispose() {
	super.dispose();
	IModelProvider provider = PDEPlugin.getDefault().getWorkspaceModelManager();
	IModel model = (IModel)getModel();
	provider.disconnect(model.getUnderlyingResource(), this);
}
public IPDEEditorPage getHomePage() {
	return getPage(BUILD_PAGE);
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
	if (name.equals("build.properties"))
		return true;
	return false;
}
protected boolean updateModel() {
	IBuildModel model = (IBuildModel) getModel();
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	String text = document.get();
	boolean cleanModel = true;
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
}
