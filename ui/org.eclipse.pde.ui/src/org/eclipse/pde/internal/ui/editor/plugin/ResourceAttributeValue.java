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
package org.eclipse.pde.internal.ui.editor.plugin;

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
