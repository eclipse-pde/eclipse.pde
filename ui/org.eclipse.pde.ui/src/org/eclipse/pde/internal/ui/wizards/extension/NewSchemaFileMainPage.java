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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;

public class NewSchemaFileMainPage extends BaseExtensionPointMainPage {
	public static final String KEY_TITLE = "NewSchemaFileWizard.title";
	public static final String KEY_DESC = "NewSchemaFileWizard.desc";
	private IPluginExtensionPoint point;
	private IContainer container;
	private boolean isPluginIdFinal;
	
	public NewSchemaFileMainPage(IContainer container) {
		this(container, null, false);
	}
	
	public NewSchemaFileMainPage(IContainer container, IPluginExtensionPoint point, boolean isPluginIdFinal){
		super(container);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		this.point = point;
		this.container = container;
		this.isPluginIdFinal = isPluginIdFinal;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		initializeValues();
		pluginIdText.setEnabled(!isPluginIdFinal);
		pluginBrowseButton.setEnabled(!isPluginIdFinal);
		setMessage(null);
	}
	public boolean finish() {
		IRunnableWithProgress operation = getOperation();
		try {
			getContainer().run(false, true, operation);
			if (point != null){
				point.setId(idText.getText());
				point.setName(nameText.getText());
				point.setSchema(schemaText.getText());
			}
				
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		} catch (CoreException e){
			return false;
		}
		return true;
	}
	protected boolean isPluginIdNeeded() {
		return true;
	}
	protected boolean isPluginIdFinal(){
		return isPluginIdFinal;
	}
	protected boolean isSharedSchemaSwitchNeeded() {
		return true;
	}
	public void initializeValues(){
		if (container!=null){
			pluginIdText.setText(container.getProject().getName());
			schemaLocationText.setText(container.getProject().getName() + "/" + container.getProjectRelativePath().toString());
		}
		if (point == null)
			return;
		if (idText!=null && point.getId()!=null)
			idText.setText(point.getId());
		if (nameText !=null && point.getName() != null)
			nameText.setText(point.getName());
		if (schemaText!= null && point.getSchema()!=null)
			schemaText.setText(point.getSchema());
		
	}
}
