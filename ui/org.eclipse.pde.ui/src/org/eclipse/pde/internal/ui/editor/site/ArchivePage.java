package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author melhem
 *
 */
public class ArchivePage extends PDEFormPage {

	public ArchivePage(PDEMultiPageEditor editor, String title) {
		super(editor, title);
	}

	public IContentOutlinePage createContentOutlinePage() {
		return null;
	}

	protected AbstractSectionForm createForm() {
		return new ArchiveForm(this);
	}
}
