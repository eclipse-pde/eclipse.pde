package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.PDEHackFinder;
import org.eclipse.swt.SWT;

public class ModifiedTextCellEditor extends TextCellEditor {
	public ModifiedTextCellEditor(Composite parent) {
		super(parent);
		setValueValid(true);
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
	private void handleEnter() {
		fireApplyEditorValue();
	}
}
