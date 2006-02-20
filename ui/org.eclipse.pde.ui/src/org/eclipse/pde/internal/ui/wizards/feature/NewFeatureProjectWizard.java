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
package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class NewFeatureProjectWizard extends AbstractNewFeatureWizard {

	public NewFeatureProjectWizard() {
		super();
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
		return PDECore.getDefault().getModelManager().getPlugins().length > 0;
	}
	
	protected AbstractFeatureSpecPage createFirstPage() {
		return new FeatureSpecPage();
	}

	public String getFeatureId() {
		return fProvider.getFeatureData().id;
	}
	
	public String getFeatureVersion() {
		return fProvider.getFeatureData().version;	
	}

	protected IRunnableWithProgress getOperation() {
		return new CreateFeatureProjectOperation(
				fProvider.getProject(),
				fProvider.getLocationPath(),
				fProvider.getFeatureData(),
				fProvider.getPluginListSelection(),
				getShell());
	}

}
