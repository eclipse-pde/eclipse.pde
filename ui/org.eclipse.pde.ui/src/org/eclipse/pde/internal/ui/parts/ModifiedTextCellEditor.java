/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

public class ModifiedTextCellEditor extends TextCellEditor {
	private Listener traverseListener;
	
	public ModifiedTextCellEditor(Composite parent) {
		super(parent);
		setValueValid(true);
	}
	
	protected void doSetValue(Object object) {
		// Workaround for 32926
		if (object==null) object = ""; //$NON-NLS-1$
		super.doSetValue(object);
	}
	public Control createControl(Composite parent) {
		Text text = (Text) super.createControl(parent);

		traverseListener = new Listener() {
			public void handleEvent(Event e) {
				// do whatever it is you want to do on commit
				handleEnter();
				// this will prevent the return from 
				// traversing to the button
				e.doit = false;
			}
		};
		text.addListener(SWT.Traverse, traverseListener);		
		return text;
	}
	
	public void dispose() {
		Control c = getControl();
		if (c!=null && !c.isDisposed() && traverseListener!=null) {
			c.removeListener(SWT.Traverse, traverseListener);
		}
		super.dispose();
	}
	
	public void forceCommit() {
		if (isDirty())
			fireApplyEditorValue();
	}

	private void handleEnter() {
		fireApplyEditorValue();
	}
}
