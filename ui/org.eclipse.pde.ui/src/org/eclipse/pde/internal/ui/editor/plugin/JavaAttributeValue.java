/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;

public class JavaAttributeValue extends ResourceAttributeValue {
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;

	public JavaAttributeValue(IProject project, IPluginModelBase model, ISchemaAttribute attInfo, String className) {
		super(project, className);
		this.attInfo = attInfo;
		this.model = model;
	}

	public ISchemaAttribute getAttributeInfo() {
		return attInfo;
	}

	public IPluginModelBase getModel() {
		return model;
	}

	public String getClassName() {
		return getStringValue();
	}
}
