package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.resources.*;
import java.util.Iterator;
import java.util.Vector;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.events.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;
import java.util.Hashtable;
import org.eclipse.pde.model.plugin.*;

public class JavaAttributeWizard extends Wizard {
	private String className;
	private JavaAttributeWizardPage mainPage;
	private IProject project;
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;
	private static final String KEY_WTITLE = "JavaAttributeWizard.wtitle";

public JavaAttributeWizard(IProject project, IPluginModelBase model, ISchemaAttribute attInfo, String className) {
	this.className = className;
	this.model = model;
	this.project = project;
	this.attInfo = attInfo;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new JavaAttributeWizardPage(project, model, attInfo, className);
	addPage(mainPage);
}
public Object getValue() {
	return new JavaAttributeValue(project, model, attInfo, className);
}
public boolean performFinish() {
	boolean result = mainPage.finish();
	if (result) {
		className = mainPage.getClassName();
	}
	return result;
}
}
