package org.eclipse.pde.internal.ui.wizards.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

public class PerspectiveExtensionsNewWizard extends NewPluginTemplateWizard {
	private static final String KEY_WTITLE = "PerspectiveExtensionsNewWizard.wtitle";
	/**
	 * Constructor for PerspectiveExtensionsNewWizard.
	 */
	public PerspectiveExtensionsNewWizard() {
		super();
	}

	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment,
		IConfigurationElement config) {
		super.init(provider, structureData, fragment, config);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	/**
	 * @see NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new PerspectiveExtensionsTemplate()};
	}

}
