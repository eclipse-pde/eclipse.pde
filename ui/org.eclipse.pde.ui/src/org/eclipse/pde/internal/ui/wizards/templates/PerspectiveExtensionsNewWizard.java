package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.ui.IPluginStructureData;
import org.eclipse.pde.ui.IProjectProvider;
import org.eclipse.pde.ui.templates.*;

public class PerspectiveExtensionsNewWizard extends NewPluginTemplateWizard {

	/**
	 * Constructor for PerspectiveExtensionsNewWizard.
	 */
	public PerspectiveExtensionsNewWizard() {
		super();
	}

	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment) {
		super.init(provider, structureData, fragment);
		setWindowTitle("New plug-in project with a perspective extension");
	}
	/**
	 * @see NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new PerspectiveExtensionsTemplate()};
	}

}
