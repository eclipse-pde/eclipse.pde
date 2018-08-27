/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.IBundleContentWizard;
import org.eclipse.pde.ui.IFieldData;

/**
 * This class is used as a common base for plug-in content wizards that are
 * implemented using PDE template support. The assumption is that one or more
 * templates will be used to generate plug-in content. Dependencies, new files
 * and wizard pages are all computed based on the templates.
 *
 * @since 2.0
 */
public abstract class AbstractNewPluginTemplateWizard extends Wizard implements IBundleContentWizard {
	private IFieldData data;

	/**
	 * Creates a new template wizard.
	 */
	public AbstractNewPluginTemplateWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IFieldData data) {
		this.data = data;
		setWindowTitle(PDEUIMessages.PluginCodeGeneratorWizard_title);
	}

	/**
	 * Returns the field data passed to the wizard during the initialization.
	 *
	 * @return the parent wizard field data
	 */
	public IFieldData getData() {
		return data;
	}

	/**
	 * This wizard adds a mandatory first page. Subclasses implement this method
	 * to add additional pages to the wizard.
	 */
	protected abstract void addAdditionalPages();

	/**
	 * Implements wizard method. Subclasses cannot override it.
	 */
	@Override
	public final void addPages() {
		addAdditionalPages();
	}

	@Override
	public boolean performFinish() {
		// do nothing - all the work is in the other 'performFinish'
		return true;
	}

	/**
	 * Implements the interface method by looping through template sections and
	 * executing them sequentially.
	 *
	 * @param project
	 *            the project
	 * @param model
	 *            the plug-in model
	 * @param monitor
	 *            the progress monitor to track the execution progress as part
	 *            of the overall new project creation operation
	 * @return <code>true</code> if the wizard completed the operation with
	 *         success, <code>false</code> otherwise.
	 */
	@Override
	public boolean performFinish(IProject project, IPluginModelBase model, IProgressMonitor monitor) {
		try {
			ITemplateSection[] sections = getTemplateSections();

			SubMonitor subMonitor = SubMonitor.convert(monitor, sections.length);
			for (ITemplateSection section : sections) {
				section.execute(project, model, subMonitor.split(1));
			}
			//No reason to do this any more with the new editors
			//saveTemplateFile(project, null);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		}

		return true;
	}

	/**
	 * Returns the template sections used in this wizard.
	 *
	 * @return the array of template sections
	 */
	public abstract ITemplateSection[] getTemplateSections();

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList<IPluginReference> result = new ArrayList<>();
		ITemplateSection[] sections = getTemplateSections();
		for (ITemplateSection section : sections) {
			IPluginReference[] refs = section.getDependencies(schemaVersion);
			for (int j = 0; j < refs.length; j++) {
				if (!result.contains(refs[j]))
					result.add(refs[j]);
			}
		}
		return result.toArray(new IPluginReference[result.size()]);
	}

	@Override
	public String[] getNewFiles() {
		ArrayList<String> result = new ArrayList<>();
		ITemplateSection[] sections = getTemplateSections();
		for (ITemplateSection section : sections) {
			String[] newFiles = section.getNewFiles();
			for (int j = 0; j < newFiles.length; j++) {
				if (!result.contains(newFiles[j]))
					result.add(newFiles[j]);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns whether this wizard has at least one page
	 * @return whether this wizard has at least one page
	 */
	public boolean hasPages() {
		return getTemplateSections().length > 0;
	}

	@Override
	public String[] getImportPackages() {
		return new String[0];
	}
}
