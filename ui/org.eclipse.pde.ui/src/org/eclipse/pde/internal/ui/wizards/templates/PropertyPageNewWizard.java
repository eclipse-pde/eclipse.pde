package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.ui.IPluginStructureData;
import org.eclipse.pde.ui.IProjectProvider;
import org.eclipse.pde.ui.templates.*;

public class PropertyPageNewWizard extends NewPluginTemplateWizard {

	/**
	 * Constructor for PropertyPageNewWizard.
	 */
	public PropertyPageNewWizard() {
		super();
	}

	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment) {
		super.init(provider, structureData, fragment);
		setWindowTitle("New plug-in project with a property page");
	}
	/**
	 * @see NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new PropertyPageTemplate()};
	}

}
