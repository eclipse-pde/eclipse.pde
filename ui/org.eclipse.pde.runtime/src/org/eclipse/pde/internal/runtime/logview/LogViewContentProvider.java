package org.eclipse.pde.internal.runtime.logview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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