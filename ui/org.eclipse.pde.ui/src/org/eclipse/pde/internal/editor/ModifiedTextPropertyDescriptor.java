package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.swt.*;

public class ModifiedTextPropertyDescriptor extends TextPropertyDescriptor {
	public ModifiedTextPropertyDescriptor(String name, String displayName) {
		super(name, displayName);
	}
public CellEditor createPropertyEditor(Composite parent) {
	CellEditor editor = new ModifiedTextCellEditor(parent);
	if (getValidator() != null)
		editor.setValidator(getValidator());
	return editor;
}
}
