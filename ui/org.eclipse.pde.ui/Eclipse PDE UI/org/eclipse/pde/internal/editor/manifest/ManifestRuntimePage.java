package org.eclipse.pde.internal.editor.manifest;

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
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.w3c.dom.Document;
import org.eclipse.pde.internal.PDEPluginImages;


public class ManifestRuntimePage extends PDEChildFormPage {

public ManifestRuntimePage(ManifestFormPage parentPage, String title) {
	super(parentPage, title);
}
protected Form createForm() {
	return new RuntimeForm(this);
}
}
