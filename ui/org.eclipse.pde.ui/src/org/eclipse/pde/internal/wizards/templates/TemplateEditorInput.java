
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;

public abstract class TemplateEditorInput extends FileEditorInput {

	/**
	 * Constructor for TemplateEditorInput.
	 * @param file
	 */
	public TemplateEditorInput(IFile file) {
		super(file);
	}
	
	public abstract PDEChildFormPage createPage(PDEFormPage parentPage, String title);
}
