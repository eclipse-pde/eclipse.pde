package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jdt.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewProjectWizard extends NewWizard 
	implements IExecutableExtension {

	private WizardNewProjectCreationPage mainPage;
	private ProjectStructurePage structurePage;
	private ProjectCodeGeneratorsPage codegenPage;
	private IConfigurationElement config;
	
	public static final String PLUGIN_POINT = "projectGenerators";
	public static final String TAG_DESCRIPTION = "description";
	public static final String KEY_TITLE = "NewProjectWizard.MainPage.title";
	public static final String KEY_FTITLE = "NewProjectWizard.MainPage.ftitle";
	public static final String KEY_DESC = "NewProjectWizard.MainPage.desc";
	public static final String KEY_FDESC = "NewProjectWizard.MainPage.fdesc";
	public static final String TAG_WIZARD = "wizard";
	public static final String ATT_FRAGMENT = "fragmentWizard";
	public static final String KEY_CODEGEN_MESSAGE = "NewProjectWizard.ProjectCodeGeneratorsPage.message";
	private static final String KEY_WTITLE = "NewProjectWizard.title";

	public NewProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		setNeedsProgressMonitor(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		
	}
public void addPages() {
	super.addPages();
	mainPage = new WizardNewProjectCreationPage("main");
	if (isFragmentWizard()) {
		mainPage.setTitle(PDEPlugin.getResourceString(KEY_FTITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(KEY_FDESC));
	} else {
		mainPage.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
	addPage(mainPage);

	IProjectProvider provider = new IProjectProvider() {
		public String getProjectName() {
			return mainPage.getProjectName();
		}
		public IProject getProject() {
			return mainPage.getProjectHandle();
		}
		public IPath getLocationPath() {
			return mainPage.getLocationPath();
		}
	};

	structurePage = new ProjectStructurePage(provider, isFragmentWizard());
	addPage(structurePage);
	codegenPage =
		new ProjectCodeGeneratorsPage(
				provider,
				structurePage,
				getAvailableCodegenWizards(),
				PDEPlugin.getResourceString(KEY_CODEGEN_MESSAGE),
				isFragmentWizard());
	addPage(codegenPage);
}
public boolean canFinish() {
	IWizardPage page = getContainer().getCurrentPage();
	if (page == mainPage || page == structurePage) return false;
	return super.canFinish();
}
protected WizardElement createWizardElement(IConfigurationElement config) {
	String name = config.getAttribute(WizardElement.ATT_NAME);
	String id = config.getAttribute(WizardElement.ATT_ID);
	String className = config.getAttribute(WizardElement.ATT_CLASS);
	if (name == null || id == null || className == null)
		return null;
	WizardElement element = new WizardElement(config);
	String imageName = config.getAttribute(WizardElement.ATT_ICON);
	if (imageName != null) {
		IPluginDescriptor pd =
			config.getDeclaringExtension().getDeclaringPluginDescriptor();
		Image image = PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(pd, imageName);
		element.setImage(image);
	}
	return element;
}
public void dispose() {
	super.dispose();
	PDEPlugin.getDefault().getLabelProvider().disconnect(this);
}

public ElementList getAvailableCodegenWizards() {
	ElementList wizards = new ElementList("CodegenWizards");
	IPluginRegistry registry = Platform.getPluginRegistry();
	IExtensionPoint point =
		registry.getExtensionPoint(PDEPlugin.getPluginId(), PLUGIN_POINT);
	if (point == null)
		return wizards;
	IExtension[] extensions = point.getExtensions();
	for (int i = 0; i < extensions.length; i++) {
		IConfigurationElement[] elements = extensions[i].getConfigurationElements();
		for (int j = 0; j < elements.length; j++) {
			if (elements[j].getName().equals(TAG_WIZARD)) {
				WizardElement element = createWizardElement(elements[j]);

				if (element != null) {
					String fragmentAtt =
						element.getConfigurationElement().getAttribute(ATT_FRAGMENT);
					boolean fragmentWizard =
						fragmentAtt != null && fragmentAtt.toLowerCase().equals("true");
					if (fragmentWizard == isFragmentWizard()) {
						wizards.add(element);
					}
				}
			}
		}
	}
	return wizards;
}
public boolean isFragmentWizard() {
	return false;
}
public boolean performFinish() {
	if (structurePage.finish()
		&& codegenPage.finish()) {
		BasicNewProjectResourceWizard.updatePerspective(config);
		revealSelection(mainPage.getProjectHandle());
		return true;
	}
	return false;
}


public void setInitializationData(
	IConfigurationElement config,
	String propertyName,
	Object data) throws CoreException {
		this.config = config;
}


}
