/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;

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

	@Override
	public String toString() {
		return getStringValue();
	}
}
