/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 171767
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that displays and edits the main launching arguments
 * of an Eclipse application.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MainTab extends AbstractLauncherTab
		implements IPDELauncherConstants, org.eclipse.pde.ui.launcher.IPDELauncherConstants {

	protected WorkspaceDataBlock fDataBlock;
	protected ProgramBlock fProgramBlock;
	protected JREBlock fJreBlock;

	private Image fImage;

	/**
	 * Contructor to create a new main tab
	 */
	public MainTab() {
		super();
		createWorkspaceDataBlock();
		createProgramBlock();
		createJREBlock();
		fImage = PDEPluginImages.DESC_MAIN_TAB.createImage();
	}

	@Override
	public void dispose() {
		fImage.dispose();
		super.dispose();
	}

	@Override
	public void createControl(Composite parent) {
		final ScrolledComposite scrollContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrollContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite composite = new Composite(scrollContainer, SWT.NONE);
		scrollContainer.setContent(composite);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fDataBlock.createControl(composite);
		fProgramBlock.createControl(composite);
		fJreBlock.createControl(composite);

		// Add listener for each control to recalculate scroll bar when it is entered.
		// This results in scrollbar scrolling when user tabs to a control that is not in the field of view.
		Listener listener = e -> {
			Control child = (Control) e.widget;
			Rectangle bounds = child.getBounds();
			Rectangle area = scrollContainer.getClientArea();
			Point origin = scrollContainer.getOrigin();
			if (origin.x > bounds.x)
				origin.x = Math.max(0, bounds.x);
			if (origin.y > bounds.y)
				origin.y = Math.max(0, bounds.y);
			if (origin.x + area.width < bounds.x + bounds.width)
				origin.x = Math.max(0, bounds.x + bounds.width - area.width);
			if (origin.y + area.height < bounds.y + bounds.height)
				origin.y = Math.max(0, bounds.y + bounds.height - area.height);
			scrollContainer.setOrigin(origin);
		};
		Control[] controls = composite.getChildren();
		for (Control control : controls)
			control.addListener(SWT.Activate, listener);

		Dialog.applyDialogFont(composite);
		composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrollContainer.setExpandHorizontal(true);
		setControl(scrollContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_BASIC);
	}

	/**
	 * Applies the given data to this page.
	 *
	 * @param data the data to apply
	 * @since 3.7
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void applyData(Object data) {
		if (data == LOCATION)
			fDataBlock.selectWorkspaceLocation();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fDataBlock.initializeFrom(config, false);
			fProgramBlock.initializeFrom(config);
			fJreBlock.initializeFrom(config);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config, false);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.performApply(config, false);
		fProgramBlock.performApply(config);
		fJreBlock.performApply(config);
	}

	@Override
	public String getName() {
		return PDEUIMessages.MainTab_name;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	/**
	 * Creates the Workspace Data group on the tab
	 */
	protected void createWorkspaceDataBlock() {
		fDataBlock = new WorkspaceDataBlock(this);
	}

	/**
	 * Creates the Program To Run group on the tab
	 *
	 */
	protected void createProgramBlock() {
		fProgramBlock = new ProgramBlock(this);
	}

	/**
	 * Creates the Java Runtime Environment group on the tab
	 *
	 * @since 3.4
	 */
	protected void createJREBlock() {
		fJreBlock = new JREBlock(this);
	}

	@Override
	public void validateTab() {
		String error = fDataBlock.validate();
		if (error == null)
			error = fJreBlock.validate();
		setErrorMessage(error);
	}

	@Override
	public String getId() {
		return org.eclipse.pde.launching.IPDELauncherConstants.TAB_MAIN_ID;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#initializeAttributes()
	 */
	@Override
	protected void initializeAttributes() {
		super.initializeAttributes();
		getAttributesLabelsForPrototype().put(IPDEConstants.LAUNCHER_PDE_VERSION, PDEUIMessages.MainTab_AttributeLabel_LauncherPDEVersion);
		getAttributesLabelsForPrototype().put(IPDEConstants.APPEND_ARGS_EXPLICITLY, PDEUIMessages.MainTab_AttributeLabel_AppendArgs);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.LOCATION, PDEUIMessages.MainTab_AttributeLabel_WorkspaceLocation);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.DOCLEAR, PDEUIMessages.MainTab_AttributeLabel_ClearWorkspace);
		getAttributesLabelsForPrototype().put(IPDEConstants.DOCLEARLOG, PDEUIMessages.MainTab_AttributeLabel_ClearLogOnly);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.ASKCLEAR, PDEUIMessages.MainTab_AttributeLabel_ClearAskForConfirmation);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.USE_PRODUCT, PDEUIMessages.MainTab_AttributeLabel_UseProduct);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.PRODUCT, PDEUIMessages.MainTab_AttributeLabel_Product);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.APPLICATION, PDEUIMessages.MainTab_AttributeLabel_Application);
		getAttributesLabelsForPrototype().put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, PDEUIMessages.MainTab_AttributeLabel_JavaExecutable);
		getAttributesLabelsForPrototype().put(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, PDEUIMessages.MainTab_AttributeLabel_JREContainerPath);
		getAttributesLabelsForPrototype().put(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDEUIMessages.MainTab_AttributeLabel_SourcePathProvider);
		getAttributesLabelsForPrototype().put(IPDELauncherConstants.BOOTSTRAP_ENTRIES, PDEUIMessages.MainTab_AttributeLabel_BootstrapEntries);
	}
}
