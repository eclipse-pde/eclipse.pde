
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

public class ViewNewWizard extends NewPluginTemplateWizard {
	private static final String KEY_WTITLE = "ViewNewWizard.wtitle";

	/**
	 * Constructor for HelloWorldNewWizard.
	 */
	public ViewNewWizard() {
		super();
	}
	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment) {
		super.init(provider, structureData, fragment);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection [] {
				new ViewTemplate() };
	}
}