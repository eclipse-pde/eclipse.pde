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
package org.eclipse.pde.internal.ui.wizards.extension;

import java.lang.reflect.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.core.resources.*;

public class NewExtensionPointMainPage extends BaseExtensionPointMainPage {
	public static final String SCHEMA_DIR = "schema";
	public static final String KEY_TITLE = "NewExtensionPointWizard.title";
	public static final String KEY_DESC = "NewExtensionPointWizard.desc";
	private IPluginModelBase model;

	public NewExtensionPointMainPage(
		IProject project,
		IPluginModelBase model) {
		super(project);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		this.model = model;
	}
	public boolean finish() {
		final String id = idText.getText();
		final String name = nameText.getText();
		final String schema = schemaText.getText();

		IPluginBase plugin = model.getPluginBase();

		IPluginExtensionPoint point = model.getFactory().createExtensionPoint();
		try {
			point.setId(id);
			if (name.length() > 0)
				point.setName(name);
			if (schema.length() > 0)
				point.setSchema(schema);

			plugin.add(point);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		if (schema.length() > 0) {
			IRunnableWithProgress operation = getOperation();
			try {
				getContainer().run(false, true, operation);
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	public String getPluginId() {
		return model.getPluginBase().getId();
	}
	public String getSchemaLocation() {
		return SCHEMA_DIR;
	}
}
