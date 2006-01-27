/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;

public class NewExtensionPointMainPage extends BaseExtensionPointMainPage {
	private IPluginModelBase model;
	private IPluginExtensionPoint point;
	
	public NewExtensionPointMainPage(
			IProject project,
			IPluginModelBase model) {
		this(project, model, null);
	}
	
	public NewExtensionPointMainPage(IProject project, IPluginModelBase model, IPluginExtensionPoint point){
		super(project);
		initialize();
		this.model = model;
		this.point = point;
	}
	public void initialize(){
		setTitle(PDEUIMessages.NewExtensionPointWizard_title);
		setDescription(PDEUIMessages.NewExtensionPointWizard_desc);
	}
	public void createControl(Composite parent) {
		super.createControl(parent);
		initializeValues();
		setPageComplete(checkFieldsFilled());
		setMessage(null);
	}
	protected boolean isPluginIdFinal(){
		return true;
	}
	public boolean finish() {
		setPageComplete(false);
		final String id = fIdText.getText();
		final String name = fNameText.getText();
		final String schema = fSchemaText.getText();
		
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
	public void initializeValues(){
		if (point == null)
			return;
		if (fIdText!=null && point.getId()!=null)
			fIdText.setText(point.getId());
		if (fNameText !=null && point.getName() != null)
			fNameText.setText(point.getName());
		if (fSchemaText!= null && point.getSchema()!=null)
			fSchemaText.setText(point.getSchema());
	}
}
