package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.ischema.*;
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
