package org.eclipse.pde.ui.templates;

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
 * content. After all the templates have executed, 
 * the wizard will use the collected list of required
 * plug-ins to set up Java buildpath so that all the
 * generated Java classes can be resolved during the build.
 */

public abstract class NewPluginTemplateWizard
	extends AbstractNewPluginTemplateWizard {
	private ITemplateSection[] sections;

	/**
	 * Creates a new template wizard.
	 */

	public NewPluginTemplateWizard() {
		sections = createTemplateSections();
	}

/**
 * Subclasses are required to implement this method by
 * creating templates that will appear in this wizard.
 * @return an array of template sections that will appear
 * in this wizard.
 */
	public abstract ITemplateSection[] createTemplateSections();

/**
 * Returns templates that appear in this section.
 */
	protected final ITemplateSection[] getTemplateSections() {
		return sections;
	}

/**
 * Implemented by asking templates in this wizard to contribute
 * pages.
 */
	protected final void addAdditionalPages() {
		// add template pages
		for (int i = 0; i < sections.length; i++) {
			sections[i].addPages(this);
		}
	}
}