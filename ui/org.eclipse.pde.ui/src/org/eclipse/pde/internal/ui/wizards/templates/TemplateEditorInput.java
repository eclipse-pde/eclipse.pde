
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

public class TemplateEditorInput extends FileEditorInput {
	private String firstPageId;

	/**
	 * Constructor for TemplateEditorInput.
	 * @param file
	 */
	public TemplateEditorInput(IFile file, String firstPageId) {
		super(file);
		this.firstPageId = firstPageId;
	}
	
	public String getFirstPageId() {
		return firstPageId;
	}
}
