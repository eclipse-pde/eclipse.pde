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
package org.eclipse.pde.internal.ui.wizards.extension;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class NewExtensionPointMainPage extends BaseExtensionPointMainPage {
	private IPluginModelBase fModel;
	private IPluginExtensionPoint fPoint;

	public NewExtensionPointMainPage(IProject project, IPluginModelBase model) {
		this(project, model, null);
	}

	public NewExtensionPointMainPage(IProject project, IPluginModelBase model, IPluginExtensionPoint point) {
		super(project);
		initialize();
		this.fModel = model;
		this.fPoint = point;
	}

	public void initialize() {
		setTitle(PDEUIMessages.NewExtensionPointWizard_title);
		setDescription(PDEUIMessages.NewExtensionPointWizard_desc);
	}

	protected boolean isPluginIdFinal() {
		return true;
	}

	public boolean finish() {
		setPageComplete(false);
		final String id = fIdText.getText();
		final String name = fNameText.getText();
		final String schema = fSchemaText.getText();

		IPluginBase plugin = fModel.getPluginBase();

		IPluginExtensionPoint point = fModel.getFactory().createExtensionPoint();
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
				getContainer().run(true, true, operation);
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
		return fModel.getPluginBase().getId();
	}

	protected void initializeValues() {
		if (fPoint == null)
			return;
		if (fIdText != null && fPoint.getId() != null)
			fIdText.setText(fPoint.getId());
		if (fNameText != null && fPoint.getName() != null)
			fNameText.setText(fPoint.getName());
		if (fSchemaText != null && fPoint.getSchema() != null)
			fSchemaText.setText(fPoint.getSchema());
	}

	protected String validateFieldContents() {
		String message = validateExtensionPointID();
		if (message != null)
			return message;

		message = validateExtensionPointName();
		if (message != null)
			return message;

		message = validateExtensionPointSchema();
		if (message != null)
			return message;

		return null;
	}

	protected String validateExtensionPointSchema() {
		// Do not validate "Extension Point Schema" Field
		return null;
	}

}
