package org.eclipse.pde.internal.ui.wizards.templates;

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
}