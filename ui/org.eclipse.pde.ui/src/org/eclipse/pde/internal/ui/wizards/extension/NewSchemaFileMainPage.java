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
package org.eclipse.pde.internal.ui.wizards.extension;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class NewSchemaFileMainPage extends BaseExtensionPointMainPage {
	private final IPluginExtensionPoint fPoint;
	private final boolean isPluginIdFinal;

	public NewSchemaFileMainPage(IContainer container) {
		this(container, null, false);
	}

	public NewSchemaFileMainPage(IContainer container, IPluginExtensionPoint point, boolean isPluginIdFinal) {
		super(container);
		setTitle(PDEUIMessages.NewSchemaFileWizard_title);
		setDescription(PDEUIMessages.NewSchemaFileWizard_desc);
		this.fPoint = point;
		this.isPluginIdFinal = isPluginIdFinal;
	}

	public boolean finish() {
		IRunnableWithProgress operation = getOperation();
		try {
			getContainer().run(true, true, operation);
			if (fPoint != null) {
				fPoint.setId(fIdText.getText());
				fPoint.setName(fNameText.getText());
				fPoint.setSchema(fSchemaText.getText());
			}

		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException | CoreException e) {
			return false;
		}
		return true;
	}

	@Override
	protected boolean isPluginIdNeeded() {
		return true;
	}

	@Override
	protected boolean isPluginIdFinal() {
		return isPluginIdFinal;
	}

	@Override
	protected boolean isSharedSchemaSwitchNeeded() {
		return true;
	}

	@Override
	public void initializeValues() {
		if (fContainer != null) {
			fPluginIdText.setText(fContainer.getProject().getName());
			if (!isPluginIdFinal())
				fSchemaLocationText.setText(fContainer.getProject().getName() + "/" + fContainer.getProjectRelativePath().toString()); //$NON-NLS-1$
		}
		if (fPoint == null)
			return;
		if (fIdText != null && fPoint.getId() != null)
			fIdText.setText(fPoint.getId());
		if (fNameText != null && fPoint.getName() != null)
			fNameText.setText(fPoint.getName());
		if (fSchemaText != null && fPoint.getSchema() != null)
			fSchemaText.setText(fPoint.getSchema());

		fPluginIdText.setEnabled(!isPluginIdFinal);
		fPluginBrowseButton.setEnabled(!isPluginIdFinal);
	}

	@Override
	protected String validateFieldContents() {
		String message = validatePluginID();
		if (message != null)
			return message;

		message = validateExtensionPointID();
		if (message != null)
			return message;

		message = validateExtensionPointName();
		if (message != null)
			return message;

		message = validateContainer();
		if (message != null)
			return message;

		message = validateExtensionPointSchema();
		if (message != null)
			return message;

		return null;
	}

	protected String validatePluginID() {
		// Verify not zero length
		String pluginID = getPluginId();
		if (pluginID.length() == 0)
			return PDEUIMessages.NewSchemaFileMainPage_missingPluginID;

		// Verify plug-in ID exists
		IPluginModelBase model = PluginRegistry.findModel(pluginID);
		if (model == null)
			return PDEUIMessages.NewSchemaFileMainPage_nonExistingPluginID;

		// Verify plug-in ID is not an external model
		if (model.getUnderlyingResource() == null)
			return PDEUIMessages.NewSchemaFileMainPage_externalPluginID;

		return null;
	}

	protected String validateContainer() {
		if (!isPluginIdFinal()) {
			// Ensure not zero length
			String newContainerName = fSchemaLocationText.getText().trim();
			if (newContainerName.length() == 0)
				return PDEUIMessages.NewSchemaFileMainPage_missingContainer;

			// Ensure valid target container
			IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
			IResource resource = root.findMember(IPath.fromOSString(newContainerName));
			if (resource instanceof IContainer) {
				fContainer = (IContainer) resource;
			} else {
				fContainer = null;
				return PDEUIMessages.NewSchemaFileMainPage_invalidContainer;
			}
		}

		// Ensure target container exists
		if (fContainer == null || !fContainer.exists())
			return PDEUIMessages.NewSchemaFileMainPage_nonExistingContainer;

		return null;
	}

}
