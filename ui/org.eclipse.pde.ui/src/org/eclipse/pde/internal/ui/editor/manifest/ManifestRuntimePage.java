package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.w3c.dom.Document;
import org.eclipse.pde.internal.ui.PDEPluginImages;


public class ManifestRuntimePage extends PDEChildFormPage {

public ManifestRuntimePage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
protected AbstractSectionForm createForm() {
	return new RuntimeForm(this);
}
}
