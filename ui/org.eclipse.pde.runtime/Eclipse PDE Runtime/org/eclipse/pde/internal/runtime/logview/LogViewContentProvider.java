package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.jface.viewers.*;

public class LogViewContentProvider implements ITreeContentProvider, IStructuredContentProvider {
	private LogView logView;

public LogViewContentProvider(LogView logView) {
	this.logView = logView;
}
public void dispose() {}
public Object[] getChildren(Object element) {
	return ((StatusAdapter)element).getChildren();
}
public Object[] getElements(Object element) {
	return logView.getLogs();
}
public Object getParent(Object element) {
	return ((StatusAdapter)element).getParent();
}
public boolean hasChildren(Object element) {
	return ((StatusAdapter)element).hasChildren();
}
public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
public boolean isDeleted(Object element) {
	return false;
}
}
