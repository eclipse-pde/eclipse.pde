package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.ui.actions.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.base.model.*;


public class ManifestFormPage extends PDEFormPage {


public ManifestFormPage(ManifestEditor editor, String title) {
	super(editor, title);
}
private void commitAllPages() {}
public IContentOutlinePage createContentOutlinePage() {
	return new ManifestFormOutlinePage(this);
}
protected Form createForm() {
	return new ManifestForm(this);
}
}
