/*
 * Created on May 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExtensionEditorSelectionPage extends WizardListSelectionPage {
	private IProject fProject;
	private IPluginBase fPluginBase;
	private IStructuredSelection fSelection;
	/**
	 * @param categories
	 * @param baseCategory
	 * @param message
	 */
	public ExtensionEditorSelectionPage(ElementList wizards) {
		super(wizards, "Ex&tension Editors:");
		//TODO translate strings
		setTitle("Extension Editors");
		setDescription("Choose one of the provided wizards to edit the selected extension");
	}
	public void init(IProject project, IPluginBase pluginBase, IStructuredSelection selection) {
		this.fProject = project;
		this.fPluginBase = pluginBase;
		this.fSelection = selection;
	}
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionEditorWizard wizard = createWizard(wizardElement);
				wizard.init(fProject, fPluginBase.getPluginModel(), fSelection);
				return wizard;
			}
			protected IExtensionEditorWizard createWizard(WizardElement element)
				throws CoreException {
				return (IExtensionEditorWizard) element.createExecutableExtension();
			}
		};
	}
}
