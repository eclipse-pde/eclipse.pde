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
 * <p> 
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */

public abstract class NewPluginTemplateWizard
	extends AbstractNewPluginTemplateWizard {
	private ITemplateSection[] sections;

	/**
	 * Creates a new template wizard.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */

	public NewPluginTemplateWizard() {
		sections = createTemplateSections();
	}

	/**
	 * Subclasses are required to implement this method by
	 * creating templates that will appear in this wizard.
	 * @return an array of template sections that will appear
	 * in this wizard.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public abstract ITemplateSection[] createTemplateSections();

	/**
	 * Returns templates that appear in this section.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	protected final ITemplateSection[] getTemplateSections() {
		return sections;
	}

	/**
	 * Implemented by asking templates in this wizard to contribute
	 * pages.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	protected final void addAdditionalPages() {
		// add template pages
		for (int i = 0; i < sections.length; i++) {
			sections[i].addPages(this);
		}
	}
}