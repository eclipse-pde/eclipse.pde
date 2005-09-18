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

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.TracingBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class TracingTab extends AbstractLauncherTab {
	
	private Image fImage;
	private TracingBlock fTracingBlock;
	
	public TracingTab() {
		fTracingBlock = new TracingBlock(this);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_DOC_SECTION_OBJ.createImage();
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());
		fTracingBlock.createControl(container);
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.LAUNCHER_TRACING);
	}

	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		fTracingBlock.activated(workingCopy);
	}
	
	public void dispose() {
		fTracingBlock.dispose();
		if (fImage != null)
			fImage.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	public void initializeFrom(ILaunchConfiguration config) {
		fTracingBlock.initializeFrom(config);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fTracingBlock.performApply(config);
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fTracingBlock.setDefaults(config);
	}
	
	public String getName() {
		return PDEUIMessages.TracingLauncherTab_name; 
	}
	
	public Image getImage() {
		return fImage;
	}
	public void validatePage() {
	}
}
