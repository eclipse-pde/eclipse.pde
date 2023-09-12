/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;

public class ElementSectionDragAdapter extends DragSourceAdapter {
	private final ISelectionProvider fSelectionProvider;
	private Object fDragData;

	/**
	 * NavigatorDragAction constructor comment.
	 */
	public ElementSectionDragAdapter(ISelectionProvider provider) {
		fSelectionProvider = provider;

	}

	/**
	 * Returns the data to be transferred in a drag and drop
	 * operation.
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		if (event.doit == false)
			return;
		if (ModelDataTransfer.getInstance().isSupportedType(event.dataType)) {
			event.data = getSelectedModelObjects();
			fDragData = event.data;
			return;
		}
		if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			event.data = createTextualRepresentation((IStructuredSelection) fSelectionProvider.getSelection());
			fDragData = null;
			return;
		}
	}

	private String createTextualRepresentation(IStructuredSelection sel) {
		StringBuilder buf = new StringBuilder();
		for (Iterator<?> iter = sel.iterator(); iter.hasNext();) {
			String name = iter.next().toString();
			buf.append(name);
			buf.append(" "); //$NON-NLS-1$
		}
		return buf.toString();
	}

	/**
	 * All selection must be named model objects.
	 */
	@Override
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

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.doit == false || fDragData == null)
			return;
		fDragData = null;
	}

	private boolean canDrag() {
		return canCopy((IStructuredSelection) fSelectionProvider.getSelection());
	}

	private boolean canCopy(IStructuredSelection selection) {
		Object prev = null;
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
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
		return createObjectRepresentation((IStructuredSelection) fSelectionProvider.getSelection());
	}

	private ISchemaObject[] createObjectRepresentation(IStructuredSelection selection) {
		ArrayList<Object> objects = new ArrayList<>();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ISchemaObject)
				objects.add(obj);
			else
				return new ISchemaObject[0];
		}
		return objects.toArray(new ISchemaObject[objects.size()]);
	}

	public Object[] getDragData() {
		if (fDragData instanceof Object[])
			return (Object[]) fDragData;
		return new Object[] {fDragData};
	}
}
