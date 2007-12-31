/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bugs 202583,207344
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class LogViewContentProvider implements ITreeContentProvider {
	private LogView logView;

	public LogViewContentProvider(LogView logView) {
		this.logView = logView;
	}

	public void dispose() { // do nothing
	}

	public Object[] getChildren(Object element) {
		return ((AbstractEntry) element).getChildren(element);
	}

	public Object[] getElements(Object element) {
		return logView.getElements();
	}

	public Object getParent(Object element) {
		if (element instanceof LogSession) {
			return null;
		}
		return ((AbstractEntry) element).getParent(element);
	}

	public boolean hasChildren(Object element) {
		return ((AbstractEntry) element).getChildren(element).length > 0;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
	}

	public boolean isDeleted(Object element) {
		return false;
	}
}
