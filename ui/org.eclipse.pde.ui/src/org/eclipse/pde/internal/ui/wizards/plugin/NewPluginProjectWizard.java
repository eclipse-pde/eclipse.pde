package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * @author melhem
 *
 */
public class NewPluginProjectWizard extends NewWizard implements IExecutableExtension {
	public static final String PLUGIN_POINT = "pluginContent";
	public static final String TAG_WIZARD = "wizard";
	public static final String DEF_PROJECT_NAME = "project_name";
	public static final String DEF_TEMPLATE_ID = "template-id";

	private IConfigurationElement fConfig;
	private PluginFieldData fPluginData;
	private IProjectProvider fProjectProvider;
	private WizardNewProjectCreationPage fMainPage;
	private ProjectStructurePage fStructurePage;
	private ContentPage fContentPage;
	private WizardListSelectionPage fWizardListPage;
	private Dictionary defaultValues;

	public NewPluginProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEPlugin.getResourceString("NewProjectWizard.title"));
		setNeedsProgressMonitor(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fPluginData = new PluginFieldData();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new WizardNewProjectCreationPage("main");
		fMainPage.setTitle(PDEPlugin.getResourceString("NewProjectWizard.MainPage.title"));
		fMainPage.setDescription(PDEPlugin.getResourceString("NewProjectWizard.MainPage.desc"));
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname!=null)
			fMainPage.setInitialProjectName(pname);
		addPage(fMainPage);
		
		fProjectProvider = new IProjectProvider() {
			public String getProjectName() {
				return fMainPage.getProjectName();
			}
			public IProject getProject() {
				return fMainPage.getProjectHandle();
			}
			public IPath getLocationPath() {
				return fMainPage.getLocationPath();
			}
		};
		
		fStructurePage = new ProjectStructurePage("page1", fProjectProvider, fPluginData, false);
		fContentPage = new ContentPage("page2", fProjectProvider, fStructurePage, fPluginData, false);
		fWizardListPage = new WizardListSelectionPage(getAvailableCodegenWizards(), fContentPage, PDEPlugin.getResourceString("WizardListSelectionPage.templates"));
		String tid = getDefaultValue(DEF_TEMPLATE_ID);
		if (tid!=null)
			fWizardListPage.setInitialTemplateId(tid);
		addPage(fStructurePage);
		addPage(fContentPage);
		addPage(fWizardListPage);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			fStructurePage.updateData();
			fContentPage.updateData();
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			IPluginContentWizard contentWizard = fWizardListPage.getSelectedWizard();
			getContainer().run(false, true,
					new NewProjectCreationOperation(fPluginData, fProjectProvider, contentWizard));
			return true;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		fConfig = config;
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
			Image image =
				PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(
					pd,
					imageName);
			element.setImage(image);
		}
		return element;
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
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(TAG_WIZARD)) {
					WizardElement element = createWizardElement(elements[j]);
					if (element != null) {
						wizards.add(element);
					}
				}
			}
		}
		return wizards;
	}
}
