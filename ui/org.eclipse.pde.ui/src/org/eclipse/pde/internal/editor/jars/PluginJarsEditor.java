package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.model.Plugin;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.dialogs.*;

public class PluginJarsEditor extends PDEMultiPageEditor {
	public static final String JARS_PAGE_TITLE = "JarsEditor.JarsPage.title";
	public static final String JARS_PAGE = "JarsPage";
	public static final String SOURCE_PAGE = "SourcePage";


public PluginJarsEditor() {
	super();
}
protected Object createModel(Object input) {
	if (input instanceof IFile) return createResourceModel((IFile)input);
	return null;
}
protected void createPages() {
	firstPageId = JARS_PAGE;
	formWorkbook.setFirstPageSelected(false);
	JarsPage jarsPage = new JarsPage(this, PDEPlugin.getResourceString(JARS_PAGE_TITLE));
	addPage(JARS_PAGE, jarsPage);
	addPage(SOURCE_PAGE, new JarsSourcePage(this));
}
private IJarsModel createResourceModel(IFile file) {
	InputStream stream = null;
	try {
		stream = file.getContents(false);
	} catch (CoreException e) {
		return null;
	}
	IModelProvider provider = PDEPlugin.getDefault().getWorkspaceModelManager();
	provider.connect(file, this);
	IJarsModel model = (IJarsModel) provider.getModel(file, this);
	try {
		model.load(stream);
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
	return getPage(JARS_PAGE);
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
	if (name.equals("plugin.jars") || name.equals("fragment.jars"))
		return true;
	return false;
}
protected boolean updateModel() {
	IJarsModel model = (IJarsModel) getModel();
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	String text = document.get();
	boolean cleanModel = true;
	try {
		InputStream stream = new ByteArrayInputStream(text.getBytes("UTF8"));

		try {
			model.reload(stream);
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
	return false;
}
}
