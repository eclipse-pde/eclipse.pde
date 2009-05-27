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
package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class NewFeatureProjectWizard extends AbstractNewFeatureWizard {

	private String fId;
	private String fVersion;

	public NewFeatureProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setWindowTitle(PDEUIMessages.NewFeatureWizard_wtitle);
	}

	public void addPages() {
		super.addPages();
		if (hasInterestingProjects()) {
			fSecondPage = new PluginListPage();
			addPage(fSecondPage);
		}
	}

	private boolean hasInterestingProjects() {
		return PluginRegistry.getActiveModels().length > 0;
	}

	protected AbstractFeatureSpecPage createFirstPage() {
		return new FeatureSpecPage();
	}

	public String getFeatureId() {
		return fId;
	}

	public String getFeatureVersion() {
		return fVersion;
	}

	protected IRunnableWithProgress getOperation() {
		FeatureData data = fProvider.getFeatureData();
		fId = data.id;
		fVersion = data.version;
		ILaunchConfiguration config = fProvider.getLaunchConfiguration();
		if (config == null)
			return new CreateFeatureProjectOperation(fProvider.getProject(), fProvider.getLocationPath(), data, fProvider.getPluginListSelection(), getShell());
		return new CreateFeatureProjectFromLaunchOperation(fProvider.getProject(), fProvider.getLocationPath(), data, config, getShell());
	}

}
