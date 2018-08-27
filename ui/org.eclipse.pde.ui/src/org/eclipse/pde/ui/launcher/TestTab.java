/*******************************************************************************
 * Copyright (c) 2009, 2015 ThoughtWorks, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ketan Padegaonkar - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

/**
 * The launch configuration tab for JUnit Plug-in Tests. This tab enhances the
 * {@link JUnitLaunchConfigurationTab} to allow for tests to (optionally)
 * run on a non-UI thread.
 *
 * <p>
 * This class may be instantiated but is not intended to be subclassed.
 * </p>
 * @since 3.5
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class TestTab extends AbstractLaunchConfigurationTab {
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;

	private final JUnitLaunchConfigurationTab junitLaunchTab;
	private Button runInUIThread;

	/**
	 * Constructor to create a new junit test tab
	 */
	public TestTab() {
		this.junitLaunchTab = new JUnitLaunchConfigurationTab();
	}

	@Override
	public void createControl(Composite parent) {
		junitLaunchTab.createControl(parent);

		Composite composite = (Composite) getControl();
		createSpacer(composite);
		createRunInUIThreadGroup(composite);
	}

	private void createRunInUIThreadGroup(Composite comp) {
		runInUIThread = new Button(comp, SWT.CHECK);
		runInUIThread.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
		runInUIThread.setText(PDEUIMessages.PDEJUnitLaunchConfigurationTab_Run_Tests_In_UI_Thread);
		GridDataFactory.fillDefaults().span(2, 0).grab(true, false).applyTo(runInUIThread);
	}

	private void createSpacer(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		GridDataFactory.fillDefaults().span(3, 0).applyTo(label);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		junitLaunchTab.initializeFrom(config);
		updateRunInUIThreadGroup(config);
	}

	private void updateRunInUIThreadGroup(ILaunchConfiguration config) {
		boolean shouldRunInUIThread = true;
		try {
			shouldRunInUIThread = config.getAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, true);
		} catch (CoreException ce) {
		}
		runInUIThread.setSelection(shouldRunInUIThread);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		junitLaunchTab.performApply(config);
		boolean selection = runInUIThread.getSelection();
		config.setAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, selection);
	}

	@Override
	public String getId() {
		return IPDELauncherConstants.TAB_TEST_ID;
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		junitLaunchTab.activated(workingCopy);
	}

	@Override
	public boolean canSave() {
		return junitLaunchTab.canSave();
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		junitLaunchTab.deactivated(workingCopy);
	}

	@Override
	public void dispose() {
		junitLaunchTab.dispose();
	}

	@Override
	public String getErrorMessage() {
		return junitLaunchTab.getErrorMessage();
	}

	@Override
	public Image getImage() {
		return junitLaunchTab.getImage();
	}

	@Override
	public String getMessage() {
		return junitLaunchTab.getMessage();
	}

	@Override
	public String getName() {
		return junitLaunchTab.getName();
	}

	@Override
	public boolean isValid(ILaunchConfiguration config) {
		return junitLaunchTab.isValid(config);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		junitLaunchTab.setDefaults(config);
	}

	@Override
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		junitLaunchTab.setLaunchConfigurationDialog(dialog);
		this.fLaunchConfigurationDialog = dialog;
	}

	@Override
	public Control getControl() {
		return junitLaunchTab.getControl();
	}

	@Override
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return fLaunchConfigurationDialog;
	}
}
