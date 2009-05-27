/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.IExtensionWizard;
import org.eclipse.pde.ui.templates.BaseOptionTemplateSection;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * This wizard should be used as a base class for 
 * wizards that provide new plug-in templates. 
 * These wizards are loaded during new plug-in or fragment
 * creation and are used to provide initial
 * content (Java classes, directory structure and
 * extensions).
 * <p>
 * This plug-in will be passed on to the templates to generate additional
 * content. After all templates have executed, 
 * the wizard will use the collected list of required
 * plug-ins to set up Java buildpath so that all the
 * generated Java classes can be resolved during the build.
 */

public class NewExtensionTemplateWizard extends Wizard implements IExtensionWizard {
	private ITemplateSection fSection;
	private IProject fProject;
	private IPluginModelBase fModel;
	private boolean fUpdatedDependencies;

	/**
	 * Creates a new template wizard.
	 */

	public NewExtensionTemplateWizard(ITemplateSection section) {
		Assert.isNotNull(section);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		setNeedsProgressMonitor(true);
		fSection = section;
	}

	public void init(IProject project, IPluginModelBase model) {
		this.fProject = project;
		this.fModel = model;
	}

	public void addPages() {
		fSection.addPages(this);
		setWindowTitle(fSection.getLabel());
		if (fSection instanceof BaseOptionTemplateSection) {
			((BaseOptionTemplateSection) fSection).initializeFields(fModel);
		}
	}

	public boolean performFinish() {
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					int totalWork = fSection.getNumberOfWorkUnits();
					monitor.beginTask(PDEUIMessages.NewExtensionTemplateWizard_generating, totalWork);
					updateDependencies();
					fSection.execute(fProject, fModel, monitor); // nsteps
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	private void updateDependencies() throws CoreException {
		IPluginReference[] refs = fSection.getDependencies(fModel.getPluginBase().getSchemaVersion());
		for (int i = 0; i < refs.length; i++) {
			IPluginReference ref = refs[i];
			if (!modelContains(ref)) {
				IPluginImport iimport = fModel.getPluginFactory().createImport();
				iimport.setId(ref.getId());
				iimport.setMatch(ref.getMatch());
				iimport.setVersion(ref.getVersion());
				fModel.getPluginBase().add(iimport);
				fUpdatedDependencies = true;
			}
		}
	}

	private boolean modelContains(IPluginReference ref) {
		IPluginBase plugin = fModel.getPluginBase();
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (iimport.getId().equals(ref.getId())) {
				// good enough
				return true;
			}
		}
		return false;
	}

	public boolean updatedDependencies() {
		return fUpdatedDependencies;
	}
}
