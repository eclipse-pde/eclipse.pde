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
package org.eclipse.pde.internal.ui.neweditor;

import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

public class PDEFormSelectionProvider
	implements ISelectionProvider {
	private Vector listeners = new Vector();
	private ISelection selection;
	private PDESourcePage sourcePage;

	public PDEFormSelectionProvider() {
	}

	public void setSourcePage(PDESourcePage sourcePage) {
		this.sourcePage = sourcePage;
		hookSourceSelectionProvider();
	}

	private void hookSourceSelectionProvider() {
		if (sourcePage == null)
			return;
		ISelectionProvider sourceProvider = sourcePage.getSelectionProvider();
		sourceProvider
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				if (sourcePage.isVisible())
					setSelection(e.getSelection());
			}
		});
	}

	public PDESourcePage getSourcePage() {
		return sourcePage;
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.addElement(listener);
	}

	public ISelection getSelection() {
		if (sourcePage != null
			&& sourcePage.isVisible()
			&& sourcePage.getSelectionProvider() != null)
			return sourcePage.getSelectionProvider().getSelection();
		else
			return selection;
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.removeElement(listener);
	}
	
	public synchronized void setSelection(ISelection selection) {
		this.selection = selection;
		SelectionChangedEvent event =
			new SelectionChangedEvent(this, selection);

		for (Iterator iter = ((Vector) listeners.clone()).iterator();
			iter.hasNext();
			) {
			ISelectionChangedListener listener =
				(ISelectionChangedListener) iter.next();
			listener.selectionChanged(event);
		}
	}
}