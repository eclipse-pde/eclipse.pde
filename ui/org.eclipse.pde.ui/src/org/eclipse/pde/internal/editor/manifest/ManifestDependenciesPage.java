package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.w3c.dom.Document;
import org.eclipse.pde.internal.PDEPluginImages;
import org.eclipse.jface.action.*;


public class ManifestDependenciesPage extends PDEChildFormPage {

public ManifestDependenciesPage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return null;
/*
	return new ManifestFormOutlinePage(
		getInput(),
		getEditor().getDocumentProvider(),
		form);
*/
}
protected SectionForm createForm() {
	return new DependenciesForm(this);
}

public boolean contextMenuAboutToShow(IMenuManager manager) {
	return ((DependenciesForm)getForm()).fillContextMenu(manager);
}
}
