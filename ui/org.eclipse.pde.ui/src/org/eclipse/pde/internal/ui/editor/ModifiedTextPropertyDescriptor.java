/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.ui.parts.*;

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
