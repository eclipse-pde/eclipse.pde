package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.part.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.*;
import org.eclipse.core.runtime.OperationCanceledException;
import java.lang.reflect.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.schema.*;
import java.io.*;
import org.eclipse.pde.internal.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.jface.window.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.editor.schema.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.codegen.*;

public class NewExtensionPointMainPage extends BaseExtensionPointMainPage {
	private IProject project;
	public static final String SCHEMA_DIR="schema";
	public static final String KEY_TITLE ="NewExtensionPointWizard.title";
	public static final String KEY_DESC ="NewExtensionPointWizard.desc";
	private IPluginModelBase model;

public NewExtensionPointMainPage(IProject project, IPluginModelBase model) {
	super(project);
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
	this.project = project;
	this.model = model;
}
public boolean finish() {
	final boolean openFile = openSchemaButton.getSelection();
	final String id = idText.getText();
	final String name = nameText.getText();
	final String schema = schemaText.getText();

	IPluginBase plugin = model.getPluginBase();

	IPluginExtensionPoint point = model.getFactory().createExtensionPoint();
	try {
		point.setId(id);
		if (name.length() > 0)
			point.setName(name);
		if (schema.length() > 0)
			point.setSchema(schema);

		plugin.add(point);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}

	IRunnableWithProgress operation = getOperation();
	try {
		getContainer().run(false, true, operation);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	} catch (InterruptedException e) {
		return false;
	}
	return true;
}
public String getPluginId() {
	return model.getPluginBase().getId();
}
public String getSchemaLocation() {
	return SCHEMA_DIR;
}
}
