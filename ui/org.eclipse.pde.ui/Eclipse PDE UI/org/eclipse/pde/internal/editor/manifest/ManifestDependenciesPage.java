package org.eclipse.pde.internal.editor.manifest;

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
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.w3c.dom.Document;
import org.eclipse.pde.internal.PDEPluginImages;


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
protected Form createForm() {
	return new DependenciesForm(this);
}
}
