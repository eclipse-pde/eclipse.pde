package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.editor.PDEEditorContributor;
import org.eclipse.swt.dnd.Clipboard;

public class SchemaEditorContributor extends PDEEditorContributor {

	public SchemaEditorContributor() {
		super("&Schema");
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}