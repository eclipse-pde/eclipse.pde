package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.pde.model.plugin.*;
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


public class ManifestTemplatePage extends PDEChildFormPage {
	private IFile templateFile;

public ManifestTemplatePage(ManifestFormPage parentPage, String title, IFile templateFile) {
	super(parentPage, title);
	this.templateFile = templateFile;
}

public IContentOutlinePage createContentOutlinePage() {
	return null;
}

public IFile getTemplateFile() {
	return templateFile;
}

protected AbstractSectionForm createForm() {
	return new TemplateForm(this);
}

public boolean contextMenuAboutToShow(IMenuManager manager) {
	return ((DependenciesForm)getForm()).fillContextMenu(manager);
}
}
