/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;
import org.eclipse.swt.custom.BusyIndicator;
public class ClassAttributeRow extends ReferenceAttributeRow {
	public ClassAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#openReference()
	 */
	protected void openReference() {
		String name = PDEJavaHelper.trimNonAlphaChars(text.getText()).replace('$', '.');
		name = PDEJavaHelper.createClass(
				name, getProject(),
				createJavaAttributeValue(name), true);
		text.setText(name);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#browse()
	 */
	protected void browse() {
		BusyIndicator.showWhile(text.getDisplay(), new Runnable() {
			public void run() {
				doOpenSelectionDialog();
			}
		});
	}
	private JavaAttributeValue createJavaAttributeValue(String name) {
		IProject project = part.getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) part.getPage().getModel();
		return new JavaAttributeValue(project, model, getAttribute(), name);
	}
	private void doOpenSelectionDialog() {
		IResource resource = getPluginBase().getModel().getUnderlyingResource();
		IProject project = (resource == null) ? null : resource.getProject();
		if (project != null) {
			String type = PDEJavaHelper.selectType();
			if (type != null)
				text.setText(type);
		}

	}
	private IPluginBase getPluginBase() {
		IBaseModel model = part.getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}
}
