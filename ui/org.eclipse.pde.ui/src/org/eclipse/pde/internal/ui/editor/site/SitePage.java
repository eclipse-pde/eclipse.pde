package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.pde.internal.ui.editor.*;

public class SitePage extends PDEFormPage {

public SitePage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new SiteOutlinePage(this);
}
protected AbstractSectionForm createForm() {
	return new SiteForm(this);
}
}
