/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

/**
 * This wizard should be used as a base class for wizards that generate plug-in
 * content using a closed set of templates. These wizards are loaded during new
 * plug-in or fragment creation and are used to provide initial content (Java
 * classes, directories/files and extensions).
 * <p>
 * The list of templates is fixed. It must be known in advance so that the
 * required wizard pages can be created. Upon finish, the template sections are
 * executed in the order of creation.
 * 
 * @since 2.0
 */
public abstract class NewPluginTemplateWizard extends AbstractNewPluginTemplateWizard {
	private ITemplateSection[] sections;

	/**
	 * Creates a new template wizard.
	 */
	public NewPluginTemplateWizard() {
		sections = createTemplateSections();
	}

	/**
	 * Subclasses are required to implement this method by creating templates
	 * that will appear in this wizard.
	 * 
	 * @return an array of template sections that will appear in this wizard.
	 */
	public abstract ITemplateSection[] createTemplateSections();

	/**
	 * Returns templates that appear in this section.
	 * 
	 * @return an array of templates
	 */
	public final ITemplateSection[] getTemplateSections() {
		return sections;
	}

	/**
	 * Implemented by asking templates in this wizard to contribute pages.
	 *  
	 */
	protected final void addAdditionalPages() {
		// add template pages
		for (int i = 0; i < sections.length; i++) {
			sections[i].addPages(this);
		}
	}
}
