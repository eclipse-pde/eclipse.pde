
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.pde.ITemplateSection;
import org.eclipse.pde.NewPluginTemplateWizard;

public class DefaultPluginTemplateWizard extends NewPluginTemplateWizard {

	/**
	 * Constructor for DefaultPluginTemplateWizard.
	 */
	public DefaultPluginTemplateWizard() {
		super();
	}

	/*
	 * @see NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection [0];
	}
}
