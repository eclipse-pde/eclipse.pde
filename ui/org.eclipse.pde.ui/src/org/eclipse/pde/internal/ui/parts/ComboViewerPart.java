/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - creation of this class
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import java.util.*;
import java.util.List;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class ComboViewerPart {
	private Control fCombo;
	private ComboViewer fComboViewer;
	private List fObjects;

	/**
	 * The magic object used to deal with null content
	 */
	public static Object NULL_OBJECT = new Object();

	public ComboViewerPart() {
	}

	public void createControl(Composite parent, FormToolkit toolkit, int style) {
		if (toolkit.getBorderStyle() == SWT.BORDER) {
			fCombo = new Combo(parent, style | SWT.BORDER);
			fComboViewer = new ComboViewer((Combo) fCombo);
		} else {
			fCombo = new CCombo(parent, style | SWT.FLAT);
			fComboViewer = new ComboViewer((CCombo) fCombo);
		}

		fObjects = new ArrayList();
		fComboViewer.setLabelProvider(new LabelProvider());
		fComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		fComboViewer.setInput(fObjects);
	}

	public Control getControl() {
		return fCombo;
	}

	public void setEnabled(boolean enabled) {
		fCombo.setEnabled(enabled);
	}

	public void refresh() {
		fComboViewer.refresh();
	}

	public void addItem(Object item) {
		fObjects.add((item == null) ? NULL_OBJECT : item);
		refresh();
	}

	public void addItem(Object item, int index) {
		fObjects.add(index, (item == null) ? NULL_OBJECT : item);
		refresh();
	}

	public Collection getItems() {
		return fObjects;
	}

	public void setItems(Object[] items) {
		fObjects.clear();
		for (int i = 0; i < items.length; i++)
			fObjects.add((items[i] == null) ? NULL_OBJECT : items[i]);
		refresh();
	}

	public void setItems(Collection items) {
		fObjects.clear();
		Iterator it = items.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			fObjects.add((o == null) ? NULL_OBJECT : o);
		}
		refresh();
	}

	public void select(Object item) {
		if (item != null)
			fComboViewer.setSelection(new StructuredSelection(item));
		else
			fComboViewer.setSelection(null);
	}

	public void select(int index) {
		if (index < fObjects.size())
			select(fObjects.get(index));
	}

	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		fComboViewer.setLabelProvider(labelProvider);
	}

	public void setComparator(ViewerComparator comparator) {
		fComboViewer.setComparator(comparator);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fComboViewer.addSelectionChangedListener(listener);
	}

	public Object getSelection() {
		return ((IStructuredSelection) fComboViewer.getSelection()).getFirstElement();
	}
}