/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

public class ModifiedTextCellEditor extends TextCellEditor {
	public ModifiedTextCellEditor(Composite parent) {
		super(parent);
		setValueValid(true);
	}
	
	protected void doSetValue(Object object) {
		// Workaround for 32926
		if (object==null) object = "";
		super.doSetValue(object);
	}
	public Control createControl(Composite parent) {
		Text text = (Text) super.createControl(parent);

		text.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				// do whatever it is you want to do on commit
				handleEnter();
				// this will prevent the return from 
				// traversing to the button
				e.doit = false;
			}
		});
		return text;
	}
	
	public void forceCommit() {
		if (isDirty())
			fireApplyEditorValue();
	}

	private void handleEnter() {
		fireApplyEditorValue();
	}
}
