package org.eclipse.pde.internal.editor.manifest;

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

public class JavaAttributeWizard extends Wizard {
	private String className;
	private JavaAttributeWizardPage mainPage;
	private IProject project;
	private ISchemaAttribute attInfo;

public JavaAttributeWizard(IProject project, ISchemaAttribute attInfo, String className) {
	this.className = className;
	this.project = project;
	this.attInfo = attInfo;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}
public void addPages() {
	mainPage = new JavaAttributeWizardPage(project, attInfo, className);
	addPage(mainPage);
}
public Object getValue() {
	return new JavaAttributeValue(project, attInfo, className);
}
public boolean performFinish() {
	boolean result = mainPage.finish();
	if (result) {
		className = mainPage.getClassName();
	}
	return result;
}
}
