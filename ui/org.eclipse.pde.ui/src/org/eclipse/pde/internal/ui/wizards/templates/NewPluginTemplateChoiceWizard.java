/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.templates.*;

public class NewPluginTemplateChoiceWizard
	extends AbstractNewPluginTemplateWizard {
	private TemplateSelectionPage fSelectionPage;
    private ITemplateSection[] fCandiates;

	public NewPluginTemplateChoiceWizard() {
	}

	public ITemplateSection[] getTemplateSections() {
        if (fSelectionPage != null) {
            return fSelectionPage.getSelectedTemplates();
        }
        return getCandidates();
    }

	public void addAdditionalPages() {
		fSelectionPage = new TemplateSelectionPage(getCandidates());
		addPage(fSelectionPage);
	}
	
	public IWizardPage getNextPage(IWizardPage page) {
		if (fSelectionPage == null)
			return null;
		return fSelectionPage.getNextVisiblePage(page);
	}
	public IWizardPage getPreviousPage(IWizardPage page) {
		return null;
	}
    private ITemplateSection[] getCandidates() {
        if (fCandiates == null) {
            createCandidates();
        }
        return fCandiates;

    }

    private void createCandidates() {
        ArrayList candidates;
        candidates = new ArrayList();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements = registry
                .getConfigurationElementsFor(PDEPlugin.getPluginId(),
                        "templates"); //$NON-NLS-1$
        for (int i = 0; i < elements.length; i++) {
            IConfigurationElement element = elements[i];
            addTemplate(element, candidates);
        }
        fCandiates = (ITemplateSection[]) candidates
                .toArray(new ITemplateSection[candidates.size()]);
    }

    private void addTemplate(IConfigurationElement config, ArrayList result) {
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
}
