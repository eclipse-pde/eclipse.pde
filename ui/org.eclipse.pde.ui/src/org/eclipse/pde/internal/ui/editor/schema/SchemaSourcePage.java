package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.views.contentoutline.*;
import java.util.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public class SchemaSourcePage extends PDESourcePage implements IPDEEditorPage {
	public static final String SCHEMA_TYPE = "__extension_point_schema";
	private IColorManager colorManager = new ColorManager();

public SchemaSourcePage(SchemaEditor editor) {
	super(editor);
	setSourceViewerConfiguration(new XMLConfiguration(colorManager));
}
public IContentOutlinePage createContentOutlinePage() {
	return new SchemaSourceOutlinePage(getEditorInput(), getDocumentProvider(), this);
}
public void dispose() {
	super.dispose();
	colorManager.dispose();
}
}
