/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.ui.templates.*;

public class NewPluginTemplateChoiceWizard
	extends AbstractNewPluginTemplateWizard {
	private TemplateSelectionPage selectionPage;

	public NewPluginTemplateChoiceWizard() {
	}

	public ITemplateSection[] getTemplateSections() {
		if (selectionPage == null)
			return new ITemplateSection[0];
		return selectionPage.getSelectedTemplates();
	}

	public void addAdditionalPages() {
		selectionPage = new TemplateSelectionPage();
		addPage(selectionPage);
	}
	
	public IWizardPage getNextPage(IWizardPage page) {
		if (selectionPage == null)
			return null;
		return selectionPage.getNextVisiblePage(page);
	}
	public IWizardPage getPreviousPage(IWizardPage page) {
		return null;
	}
}