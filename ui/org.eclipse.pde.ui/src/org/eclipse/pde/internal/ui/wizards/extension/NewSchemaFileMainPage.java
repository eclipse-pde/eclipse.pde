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

import java.lang.reflect.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;

public class NewSchemaFileMainPage extends BaseExtensionPointMainPage {
	private IPluginExtensionPoint point;
	private IContainer container;
	private boolean isPluginIdFinal;
	
	public NewSchemaFileMainPage(IContainer container) {
		this(container, null, false);
	}
	
	public NewSchemaFileMainPage(IContainer container, IPluginExtensionPoint point, boolean isPluginIdFinal){
		super(container);
		setTitle(PDEUIMessages.NewSchemaFileWizard_title);
		setDescription(PDEUIMessages.NewSchemaFileWizard_desc);
		this.point = point;
		this.container = container;
		this.isPluginIdFinal = isPluginIdFinal;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		initializeValues();
		fPluginIdText.setEnabled(!isPluginIdFinal);
		fPluginBrowseButton.setEnabled(!isPluginIdFinal);
		setMessage(null);
	}
	public boolean finish() {
		IRunnableWithProgress operation = getOperation();
		try {
			getContainer().run(false, true, operation);
			if (point != null){
				point.setId(fIdText.getText());
				point.setName(fNameText.getText());
				point.setSchema(fSchemaText.getText());
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
			fPluginIdText.setText(container.getProject().getName());
			if (!isPluginIdFinal())
				fSchemaLocationText.setText(container.getProject().getName() + "/" + container.getProjectRelativePath().toString()); //$NON-NLS-1$
		}
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
