
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;

public class HelloWorldNewWizard extends NewPluginTemplateWizard {

	/**
	 * Constructor for HelloWorldNewWizard.
	 */
	public HelloWorldNewWizard() {
		super();
	}
	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment) {
		super.init(provider, structureData, fragment);
		setWindowTitle("New Hello World plug-in project");
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection [] {
				new HelloWorldTemplate() };
	}
}