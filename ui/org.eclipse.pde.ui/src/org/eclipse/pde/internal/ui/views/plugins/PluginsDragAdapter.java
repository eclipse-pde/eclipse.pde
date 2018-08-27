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
package org.eclipse.pde.internal.ui.views.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Control;

public class PluginsDragAdapter extends DragSourceAdapter {
	ISelectionProvider selectionProvider;

	/**
	 * NavigatorDragAction constructor comment.
	 */
	public PluginsDragAdapter(ISelectionProvider provider) {
		selectionProvider = provider;
	}

	/**
	 * Returns the data to be transferred in a drag and drop
	 * operation.
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {

		//resort to a file transfer
		if (!FileTransfer.getInstance().isSupportedType(event.dataType))
			return;

		FileAdapter[] files = getSelectedFiles();

		// Get the path of each file and set as the drag data
		final int len = files.length;
		String[] fileNames = new String[len];
		for (int i = 0, length = len; i < length; i++) {
			fileNames[i] = files[i].getFile().getAbsolutePath();
		}
		event.data = fileNames;
	}

	/**
	 * All selection must be files or folders.
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

		FileAdapter[] files = getSelectedFiles();

		if (files.length == 0) {
			event.doit = false;
			return;
		}
		event.doit = true;
	}

	private FileAdapter[] getSelectedFiles() {
		IStructuredSelection selection = (IStructuredSelection) selectionProvider.getSelection();
		ArrayList<Object> files = new ArrayList<>();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof FileAdapter)
				files.add(obj);
			else
				return new FileAdapter[0];
		}
		return files.toArray(new FileAdapter[files.size()]);
	}
}
