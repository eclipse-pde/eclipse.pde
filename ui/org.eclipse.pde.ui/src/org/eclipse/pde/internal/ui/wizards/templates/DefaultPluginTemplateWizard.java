
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.templates.*;



public class DefaultPluginTemplateWizard extends NewPluginTemplateWizard {

	/**
	 * Constructor for DefaultPluginTemplateWizard.
	 */
	public DefaultPluginTemplateWizard() {
		super();
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection [0];
	}
}
