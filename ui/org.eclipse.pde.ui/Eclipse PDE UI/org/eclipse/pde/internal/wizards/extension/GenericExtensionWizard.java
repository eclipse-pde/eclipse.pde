package org.eclipse.pde.internal.wizards.extension;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.*;

public class GenericExtensionWizard extends Wizard implements IExtensionWizard {
	private IPluginModelBase model;
	private IProject project;
	private PointSelectionPage pointSelectionPage;

public GenericExtensionWizard() {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
}
public void addPages() {
	pointSelectionPage = new PointSelectionPage(model.getPluginBase());
	addPage(pointSelectionPage);
}
public IPluginExtension getNewExtension() {
	return pointSelectionPage.getNewExtension();
}
public void init(IProject project, IPluginModelBase pluginModelBase) {
	this.project = project;
	this.model = pluginModelBase;
}
public boolean performFinish() {
	return pointSelectionPage.finish();
}
}
