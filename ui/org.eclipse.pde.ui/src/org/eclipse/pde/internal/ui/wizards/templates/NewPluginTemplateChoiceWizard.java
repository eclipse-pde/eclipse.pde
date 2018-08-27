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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.swt.widgets.Composite;

public class NewPluginTemplateChoiceWizard extends AbstractNewPluginTemplateWizard {
	private TemplateSelectionPage fSelectionPage;
	private ITemplateSection[] fCandidates;

	public NewPluginTemplateChoiceWizard() {
	}

	@Override
	public ITemplateSection[] getTemplateSections() {
		if (fSelectionPage != null) {
			return fSelectionPage.getSelectedTemplates();
		}
		return getCandidates();
	}

	@Override
	public void addAdditionalPages() {
		fSelectionPage = new TemplateSelectionPage(getCandidates());
		addPage(fSelectionPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (fSelectionPage == null)
			return null;
		return fSelectionPage.getNextVisiblePage(page);
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		return null;
	}

	private ITemplateSection[] getCandidates() {
		if (fCandidates == null) {
			createCandidates();
		}
		return fCandidates;

	}

	// calculate canFinish only on selected templateSections and the status of their pages
	@Override
	public boolean canFinish() {
		ITemplateSection[] sections = fSelectionPage.getSelectedTemplates();
		for (ITemplateSection section : sections) {
			int pageCount = section.getPageCount();
			for (int j = 0; j < pageCount; j++) {
				WizardPage page = section.getPage(j);
				if (page != null && !page.isPageComplete())
					return false;
			}
		}
		return true;
	}

	private void createCandidates() {
		ArrayList<Object> candidates;
		candidates = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(PDEPlugin.getPluginId(), "templates"); //$NON-NLS-1$
		for (IConfigurationElement element : elements) {
			addTemplate(element, candidates);
		}
		fCandidates = candidates.toArray(new ITemplateSection[candidates.size()]);
	}

	private void addTemplate(IConfigurationElement config, ArrayList<Object> result) {
		if (config.getName().equalsIgnoreCase("template") == false) //$NON-NLS-1$
			return;

		try {
			Object template = config.createExecutableExtension("class"); //$NON-NLS-1$
			if (template instanceof ITemplateSection) {
				result.add(template);
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	// by default, all pages in wizard get created.  We add all the pages from the template sections and we don't want to initialize them yet
	// Therefore, the createPageControls only initializes the first page, allowing the other to be created as needed.
	@Override
	public void createPageControls(Composite pageContainer) {
		fSelectionPage.createControl(pageContainer);
	}
}
