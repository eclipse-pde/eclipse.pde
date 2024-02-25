/*******************************************************************************
 * Copyright (c) 2010, 2020 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui;

import java.util.Iterator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundle;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundleVersion;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryResourceElement;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

public class SelectionDragAdapter implements DragSourceListener {

	private final LocalSelectionTransfer	selectionTransfer	= LocalSelectionTransfer.getTransfer();
	private final TextTransfer				textTransfer		= TextTransfer.getInstance();

	private final Viewer					viewer;

	public SelectionDragAdapter(Viewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		ISelection selection = viewer.getSelection();
		selectionTransfer.setSelection(selection);
		selectionTransfer.setSelectionSetTime(event.time & 0xFFFFFFFFL);
		event.doit = !selection.isEmpty();
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		if (textTransfer.isSupportedType(event.dataType)) {
			ISelection selection = selectionTransfer.getSelection();
			Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
			while (iterator.hasNext()) {
				Object item = iterator.next();
				if (item instanceof RepositoryBundle) {
					RepositoryBundle rb = (RepositoryBundle) item;
					event.data = rb.getResource()
						.toString();
					break;
				} else if (item instanceof RepositoryBundleVersion) {
					RepositoryBundleVersion rbv = (RepositoryBundleVersion) item;
					event.data = rbv.getResource()
						.toString();
					break;
				} else if (item instanceof RepositoryResourceElement) {
					RepositoryResourceElement rbe = (RepositoryResourceElement) item;
					event.data = rbe.getResource()
						.toString();
					break;
				}
			}
			return;
		}
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data = selectionTransfer.getSelection();
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		selectionTransfer.setSelection(null);
		selectionTransfer.setSelectionSetTime(0);
	}
}
