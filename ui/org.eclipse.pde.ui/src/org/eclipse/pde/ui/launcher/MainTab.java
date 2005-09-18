/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.JREBlock;
import org.eclipse.pde.internal.ui.launcher.ProgramBlock;
import org.eclipse.pde.internal.ui.launcher.WorkspaceDataBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class MainTab extends AbstractLauncherTab implements IPDELauncherConstants {
	
	private WorkspaceDataBlock fDataBlock;
	protected ProgramBlock fProgramBlock;
	private JREBlock fJreBlock;
	
	private Image fImage;

	public MainTab() {
		fDataBlock = new WorkspaceDataBlock(this);
		createProgramBlock();
		fJreBlock = new JREBlock(this);
		fImage = PDEPluginImages.DESC_MAIN_TAB.createImage();
	}

	public void dispose() {
		super.dispose();
		fImage.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fDataBlock.createControl(composite);		
		fProgramBlock.createControl(composite);
		fJreBlock.createControl(composite);
		
		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}
	
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fDataBlock.initializeFrom(config);
			fProgramBlock.initializeFrom(config);
			fJreBlock.initializeFrom(config);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
		}
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.performApply(config);
		fProgramBlock.performApply(config);
		fJreBlock.performApply(config);
	}
	
	public String getName() {
		return PDEUIMessages.MainTab_name;
	}
	
	public Image getImage() {
		return fImage;
	}
	
	protected void createProgramBlock() {
		fProgramBlock = new ProgramBlock(this);		
	}

	public void validatePage() {
		setErrorMessage(fDataBlock.validate());
	}
	
}
