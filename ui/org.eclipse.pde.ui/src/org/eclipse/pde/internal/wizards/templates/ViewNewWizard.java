
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.pde.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;

public class ViewNewWizard extends NewPluginTemplateWizard {

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
		setWindowTitle("New plug-in project with a sample view");
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection [] {
				new ViewTemplate() };
	}
}