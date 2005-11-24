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
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;

public class ElementSectionDragAdapter extends DragSourceAdapter {
	ISelectionProvider selectionProvider;
	Object dragData;
	ElementSection section;

	/**
	 * NavigatorDragAction constructor comment.
	 */
	public ElementSectionDragAdapter(ISelectionProvider provider, ElementSection section) {
		selectionProvider = provider;
		this.section = section;
	}

	/**
	 * Returns the data to be transferred in a drag and drop
	 * operation.
	 */
	public void dragSetData(DragSourceEvent event) {
		if (event.doit == false)
			return;
		if (ModelDataTransfer.getInstance().isSupportedType(event.dataType)) {
			event.data = getSelectedModelObjects();
			dragData = event.data;
			return;
		}
		if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			event.data =
				createTextualRepresentation(
					(IStructuredSelection) selectionProvider.getSelection());
			dragData = null;
			return;
		}
	}

	private String createTextualRepresentation(IStructuredSelection sel) {
		StringBuffer buf = new StringBuffer();
		for (Iterator iter = sel.iterator(); iter.hasNext();) {
			String name = iter.next().toString();
			buf.append(name);
			buf.append(" "); //$NON-NLS-1$
		}
		return buf.toString();
	}
	/**
	 * All selection must be named model objects.
	 */
	public void dragStart(DragSourceEvent event) {

		// Workaround for 1GEUS9V
		DragSource dragSource = (DragSource) event.widget;
		Control control = dragSource.getControl();
		if (control != control.getDisplay().getFocusControl()) {
			event.doit = false;
			return;
		}

		event.doit = canDrag();
	}

	public void dragFinished(DragSourceEvent event) {
		if (event.doit == false || dragData == null)
			return;
		if (event.detail == DND.DROP_MOVE) {
			ISchemaObject[] objects = (ISchemaObject[]) dragData;

			for (int i = 0; i < objects.length; i++) {
				ISchemaObject obj = objects[i];
				section.handleDelete(obj);
			}
		}
		dragData = null;
	}

	private boolean canDrag() {
		return canCopy((IStructuredSelection) selectionProvider.getSelection());
	}

	private boolean canCopy(IStructuredSelection selection) {
		Object prev = null;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof ISchemaObject))
				return false;
			if (prev != null) {
				if (prev.getClass().equals(obj.getClass()) == false)
					return false;
			} else
				prev = obj;
		}
		return true;
	}

	private ISchemaObject[] getSelectedModelObjects() {
		return createObjectRepresentation(
			(IStructuredSelection) selectionProvider.getSelection());
	}

	private ISchemaObject[] createObjectRepresentation(IStructuredSelection selection) {
		ArrayList objects = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ISchemaObject)
				objects.add(obj);
			else
				return new ISchemaObject[0];
		}
		return (ISchemaObject[]) objects.toArray(
			new ISchemaObject[objects.size()]);
	}
}
