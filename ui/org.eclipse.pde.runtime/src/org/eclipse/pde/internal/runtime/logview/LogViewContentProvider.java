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
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.jface.viewers.*;

public class LogViewContentProvider
	implements ITreeContentProvider, IStructuredContentProvider {
	private LogView logView;

	public LogViewContentProvider(LogView logView) {
		this.logView = logView;
	}
	public void dispose() {
	}
	public Object[] getChildren(Object element) {
		return ((LogEntry) element).getChildren(element);
	}
	public Object[] getElements(Object element) {
		return logView.getLogs();
	}
	public Object getParent(Object element) {
		return ((LogEntry) element).getParent(element);
	}
	public boolean hasChildren(Object element) {
		return ((LogEntry) element).hasChildren();
	}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	public boolean isDeleted(Object element) {
		return false;
	}
}
