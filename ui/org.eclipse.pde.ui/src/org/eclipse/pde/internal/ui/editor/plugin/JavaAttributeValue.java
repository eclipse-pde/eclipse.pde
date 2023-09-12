/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;

public class JavaAttributeValue extends ResourceAttributeValue {
	private final ISchemaAttribute attInfo;
	private final IPluginModelBase model;

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
