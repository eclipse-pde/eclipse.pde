/*
 * Created on May 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.ITemplateSection;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExtensionTreeSelectionPage extends WizardTreeSelectionPage {
	private IProject fProject;
	private IPluginBase fPluginBase;
	/**
	 * @param categories
	 * @param baseCategory
	 * @param message
	 */
	public ExtensionTreeSelectionPage(WizardCollectionElement categories,
			String baseCategory, String message) {
		super(categories, baseCategory, message);
		// TODO Auto-generated constructor stub
	}
	public void init(IProject project, IPluginBase pluginBase) {
		this.fProject = project;
		this.fPluginBase = pluginBase;
	}
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionWizard wizard = createWizard(wizardElement);
				wizard.init(fProject, fPluginBase.getPluginModel());
				return wizard;
			}
			protected IExtensionWizard createWizard(WizardElement element)
				throws CoreException {
				if (element.isTemplate()) {
					IConfigurationElement template = element.getTemplateElement();
					if (template==null) return null;
					ITemplateSection section =
						(ITemplateSection) template.createExecutableExtension("class");
					return new NewExtensionTemplateWizard(section);
				} else {
					return (IExtensionWizard) element.createExecutableExtension();
				}
			}
		};
	}
	public ISelectionProvider getSelectionProvider() {
		return wizardSelectionViewer;
	}
}
