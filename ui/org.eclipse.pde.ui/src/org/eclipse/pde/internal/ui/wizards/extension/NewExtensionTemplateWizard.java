/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
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
 * The wizard provides a common first page that will
 * initialize the plug-in itself. This plug-in will
 * be passed on to the templates to generate additional
 * content. After all templates have executed, 
 * the wizard will use the collected list of required
 * plug-ins to set up Java buildpath so that all the
 * generated Java classes can be resolved during the build.
 */

public class NewExtensionTemplateWizard
	extends Wizard
	implements IExtensionWizard {
	private ITemplateSection section;
	IProject project;
	IPluginModelBase model;
	boolean fUpdatedDependencies;
	/**
	 * Creates a new template wizard.
	 */

	public NewExtensionTemplateWizard(ITemplateSection section) {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		setNeedsProgressMonitor(true);
		this.section = section;
	}
	
	public void init(IProject project, IPluginModelBase model) {
		this.project = project;
		this.model = model;
	}

	public void setSection(ITemplateSection section) {
		this.section = section;
	}

	public ITemplateSection getSection() {
		return section;
	}

	public void addPages() {
		section.addPages(this);
		if (getSection() != null)
			setWindowTitle(getSection().getLabel());
		if (section instanceof BaseOptionTemplateSection) {
			((BaseOptionTemplateSection)section).initializeFields(model);
		}
	}

	public boolean performFinish() {
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					doFinish(monitor);
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

	protected void doFinish(IProgressMonitor monitor) throws CoreException {
		int totalWork = section.getNumberOfWorkUnits();
		monitor.beginTask(PDEUIMessages.NewExtensionTemplateWizard_generating, totalWork); 
		updateDependencies();
		section.execute(project, model, monitor); // nsteps
	}

	private void updateDependencies() throws CoreException {
		IPluginReference[] refs = section.getDependencies(model.getPluginBase().getSchemaVersion());
		for (int i = 0; i < refs.length; i++) {
			IPluginReference ref = refs[i];
			if (!modelContains(ref)) {
				IPluginImport iimport = model.getPluginFactory().createImport();
				iimport.setId(ref.getId());
				iimport.setMatch(ref.getMatch());
				iimport.setVersion(ref.getVersion());
				model.getPluginBase().add(iimport);
				fUpdatedDependencies = true;
			}
		}
	}

	private boolean modelContains(IPluginReference ref) {
		IPluginBase plugin = model.getPluginBase();
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
