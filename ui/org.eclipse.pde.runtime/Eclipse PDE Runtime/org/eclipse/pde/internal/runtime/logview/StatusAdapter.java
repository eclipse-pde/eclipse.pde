package org.eclipse.pde.internal.runtime.logview;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.*;

public class StatusAdapter extends PlatformObject {
	private IStatus status;
	private static final String KEY_ERROR = "LogView.severity.error";
	private static final String KEY_WARNING = "LogView.severity.warning";
	private static final String KEY_INFO = "LogView.severity.info";
	private StatusAdapter[] childAdapters;
	private StatusAdapter parent;

public StatusAdapter(IStatus status) {
	this(null, status);
}
public StatusAdapter(StatusAdapter parent, IStatus status) {
	this.parent = parent;
	this.status = status;
}
public Object[] getChildren() {
	if (childAdapters == null) {
		IStatus[] children = status.getChildren();
		childAdapters = new StatusAdapter[children.length];
		for (int i = 0; i < children.length; i++) {
			childAdapters[i] = new StatusAdapter(this, children[i]);
		}
	}
	return childAdapters;
}
public StatusAdapter getParent() {
	return parent;
}
public String getSeverityText() {
	return getSeverityText(status.getSeverity());
}
private String getSeverityText(int severity) {
	switch (severity) {
		case IStatus.ERROR:
		return PDERuntimePlugin.getResourceString(KEY_ERROR);
		case IStatus.WARNING:
		return PDERuntimePlugin.getResourceString(KEY_WARNING);
		case IStatus.INFO:
		return PDERuntimePlugin.getResourceString(KEY_INFO);
	}
	return "?";
}
public IStatus getStatus() {
	return status;
}
public boolean hasChildren() {
	return status.isMultiStatus();
}
public String toString() {
	return getSeverityText();
}
}
