package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.resources.*;

public class ResourceAttributeValue {
	private IProject project;
	private String stringValue;

public ResourceAttributeValue(IProject project, String stringValue) {
	this.project = project;
	this.stringValue = stringValue;
}
public IProject getProject() {
	return project;
}
public String getStringValue() {
	return stringValue;
}
public String toString() {
	return getStringValue();
}
}
