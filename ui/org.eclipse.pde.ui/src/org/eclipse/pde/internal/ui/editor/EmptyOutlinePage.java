/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;

public class EmptyOutlinePage implements ISortableContentOutlinePage {
	private Composite control;

	/**
	 *
	 */
	public EmptyOutlinePage() {
	}

	@Override
	public void createControl(Composite parent) {
		control = new Composite(parent, SWT.NULL);
	}

	@Override
	public void dispose() {
	}

	@Override
	public Control getControl() {
		return control;
	}

	@Override
	public void setActionBars(IActionBars actionBars) {
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
	}

	@Override
	public ISelection getSelection() {
		return () -> true;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
	}

	@Override
	public void setSelection(ISelection selection) {
	}

	@Override
	public void sort(boolean sorting) {
	}
}
