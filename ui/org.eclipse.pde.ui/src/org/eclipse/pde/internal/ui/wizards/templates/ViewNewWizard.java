
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;

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