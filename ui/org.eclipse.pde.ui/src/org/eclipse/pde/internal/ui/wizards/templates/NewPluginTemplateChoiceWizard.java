package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.ui.templates.*;

public class NewPluginTemplateChoiceWizard
	extends AbstractNewPluginTemplateWizard {
	private TemplateSelectionPage selectionPage;

	public NewPluginTemplateChoiceWizard() {
	}

	public ITemplateSection[] getTemplateSections() {
		return selectionPage.getSelectedTemplates();
	}

	public void addAdditionalPages() {
		selectionPage = new TemplateSelectionPage();
		addPage(selectionPage);
	}
	
	public IWizardPage getNextPage(IWizardPage page) {
		return selectionPage.getNextVisiblePage(page);
	}
	public IWizardPage getPreviousPage(IWizardPage page) {
		return selectionPage.getPreviousVisiblePage(page);
	}
}