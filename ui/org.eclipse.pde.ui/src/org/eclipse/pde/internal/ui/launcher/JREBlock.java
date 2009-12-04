/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 195433, 218210
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.*;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.launching.launcher.VMHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class JREBlock {

	private AbstractLauncherTab fTab;
	private Listener fListener = new Listener();
	private Button fJavawButton;
	private Button fJavaButton;
	private Button fJreButton;
	private Button fEeButton;
	private Button fJrePrefButton;
	private Button fEePrefButton;
	private Combo fJreCombo;
	private Combo fEeCombo;
	private Text fBootstrap;

	class Listener extends SelectionAdapter implements ModifyListener {
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			// When a radio button selection changes, we get two events.  One for the deselection of the old button and another for the selection 
			// of the new button.  We only need to update the configuration once when the selection changes.  Hence, we can ignore the deselection 
			// event of the old button.
			if (source instanceof Button && !((Button) source).getSelection())
				return;
			fTab.updateLaunchConfigurationDialog();
			if (source == fJreButton || source == fEeButton)
				updateJREEnablement();
		}

		public void modifyText(ModifyEvent e) {
			fTab.scheduleUpdateJob();
		}
	}

	public JREBlock(AbstractLauncherTab tab) {
		fTab = tab;
	}

	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.MainTab_jreSection);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createJavaExecutableSection(group);
		createJRESection(group);
		createBootstrapEntriesSection(group);
	}

	protected void createJRESection(Composite parent) {
		fEeButton = new Button(parent, SWT.RADIO);
		fEeButton.setText(PDEUIMessages.BasicLauncherTab_ee);
		fEeButton.addSelectionListener(fListener);

		fEeCombo = SWTFactory.createCombo(parent, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
		fEeCombo.addSelectionListener(fListener);

		fEePrefButton = new Button(parent, SWT.PUSH);
		fEePrefButton.setText(PDEUIMessages.BasicLauncherTab_environments);
		fEePrefButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String currentEE = parseEESelection(fEeCombo.getText());
				if (SWTFactory.showPreferencePage(fTab.getControl().getShell(), "org.eclipse.jdt.debug.ui.jreProfiles", null) == Window.OK) { //$NON-NLS-1$
					setEECombo();
					setEEComboSelection(currentEE);
				}
			}
		});
		fEePrefButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		SWTUtil.setButtonDimensionHint(fEePrefButton);

		fJreButton = new Button(parent, SWT.RADIO);
		fJreButton.setText(PDEUIMessages.BasicLauncherTab_jre);
		fJreButton.addSelectionListener(fListener);

		fJreCombo = SWTFactory.createCombo(parent, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
		fJreCombo.addSelectionListener(fListener);

		fJrePrefButton = new Button(parent, SWT.PUSH);
		fJrePrefButton.setText(PDEUIMessages.BasicLauncherTab_installedJREs);
		fJrePrefButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String currentVM = fJreCombo.getText();
				String currentEE = parseEESelection(fEeCombo.getText());
				boolean useDefault = VMUtil.getDefaultVMInstallName().equals(currentVM);
				if (SWTFactory.showPreferencePage(fTab.getControl().getShell(), "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", null) == Window.OK) { //$NON-NLS-1$
					setJRECombo();
					if (useDefault || fJreCombo.indexOf(currentVM) == -1)
						fJreCombo.setText(VMUtil.getDefaultVMInstallName());
					else
						fJreCombo.setText(currentVM);
					setEECombo();
					setEEComboSelection(currentEE);
				}
			}
		});
		fJrePrefButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		SWTUtil.setButtonDimensionHint(fJrePrefButton);
	}

	protected void createJavaExecutableSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEUIMessages.BasicLauncherTab_javaExec);

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		layout.horizontalSpacing = 20;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);

		fJavawButton = new Button(composite, SWT.RADIO);
		fJavawButton.setText(PDEUIMessages.BasicLauncherTab_javaExecDefault); // 
		fJavawButton.addSelectionListener(fListener);

		fJavaButton = new Button(composite, SWT.RADIO);
		fJavaButton.setText("&java"); //$NON-NLS-1$
		fJavaButton.addSelectionListener(fListener);
	}

	private void createBootstrapEntriesSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEUIMessages.BasicLauncherTab_bootstrap);

		fBootstrap = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		gd.horizontalSpan = 2;
		fBootstrap.setLayoutData(gd);
		fBootstrap.addModifyListener(fListener);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		initializeJRESection(config);
		initializeBootstrapEntriesSection(config);
	}

	private void initializeJRESection(ILaunchConfiguration config) throws CoreException {
		String javaCommand = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, "javaw"); //$NON-NLS-1$
		fJavawButton.setSelection(javaCommand.equals("javaw")); //$NON-NLS-1$
		fJavaButton.setSelection(!fJavawButton.getSelection());

		String jre = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
		IPath jrePath = null;
		if (jre != null) {
			jrePath = Path.fromPortableString(jre);
		}
		String vmInstallName = null;
		String eeId = null;
		if (jrePath == null) {
			vmInstallName = config.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);
			if (vmInstallName == null) {
				vmInstallName = VMHelper.getDefaultVMInstallName(config);
			}
		} else {
			eeId = JavaRuntime.getExecutionEnvironmentId(jrePath);
			if (eeId == null) {
				vmInstallName = JavaRuntime.getVMInstallName(jrePath);
			}
		}
		fJreButton.setSelection(vmInstallName != null);
		fEeButton.setSelection(eeId != null);

		setJRECombo();
		if (vmInstallName == null) {
			vmInstallName = JavaRuntime.getVMInstallName(jrePath);
			if (vmInstallName == null) {
				vmInstallName = VMHelper.getDefaultVMInstallName(config);
			}
		}
		fJreCombo.setText(vmInstallName);
		if (fJreCombo.getSelectionIndex() == -1)
			fJreCombo.setText(VMUtil.getDefaultVMInstallName());

		setEECombo();
		setEEComboSelection(eeId);

		updateJREEnablement();
	}

	private void setEEComboSelection(String eeId) {
		if (eeId != null) {
			String[] items = fEeCombo.getItems();
			for (int i = 0; i < items.length; i++) {
				if (parseEESelection(items[i]).equals(eeId)) {
					fEeCombo.select(i);
					return;
				}
			}
		}
		if (fEeCombo.getItemCount() > 0 && fEeCombo.getSelectionIndex() == -1)
			fEeCombo.select(0);
	}

	private void updateJREEnablement() {
		fJreCombo.setEnabled(fJreButton.getSelection());
		fJrePrefButton.setEnabled(fJreButton.getSelection());
		fEeCombo.setEnabled(fEeButton.getSelection());
		fEePrefButton.setEnabled(fEeButton.getSelection());
	}

	private void initializeBootstrapEntriesSection(ILaunchConfiguration config) throws CoreException {
		fBootstrap.setText(config.getAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, "")); //$NON-NLS-1$
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		saveJRESection(config);
		saveBootstrapEntriesSection(config);
	}

	protected void saveJRESection(ILaunchConfigurationWorkingCopy config) {
		String javaCommand = fJavawButton.getSelection() ? null : "java"; //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, javaCommand);

		IPath jrePath = null;
		if (fJreButton.getSelection()) {
			if (fJreCombo.getSelectionIndex() != -1) {
				String jreName = fJreCombo.getText();

				String defaultVM = null;
				try {
					defaultVM = VMHelper.getDefaultVMInstallName(config);
				} catch (CoreException e) {
				}
				// Only set the jrePath if name is different from the default
				if (defaultVM == null || !jreName.equals(defaultVM)) {
					IVMInstall install = VMHelper.getVMInstall(jreName);
					// remove the name to make portable
					jrePath = JavaRuntime.newJREContainerPath(install);
				}
			}
		} else {
			if (fEeCombo.getSelectionIndex() != -1) {
				IExecutionEnvironment environment = VMUtil.getExecutionEnvironment(parseEESelection(fEeCombo.getText()));
				if (environment != null) {
					jrePath = JavaRuntime.newJREContainerPath(environment);
				}
			}
		}
		String attr = null;
		if (jrePath != null) {
			attr = jrePath.toPortableString();
		}
		// remove old attribute first
		config.removeAttribute(IPDELauncherConstants.VMINSTALL);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, attr);
	}

	protected void saveBootstrapEntriesSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, fBootstrap.getText().trim());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
	}

	private void setJRECombo() {
		String[] jres = VMUtil.getVMInstallNames();
		Arrays.sort(jres, getComparator());
		fJreCombo.setItems(jres);
	}

	private void setEECombo() {
		IExecutionEnvironment[] eeObjects = VMUtil.getExecutionEnvironments();
		String[] ees = new String[eeObjects.length];
		for (int i = 0; i < eeObjects.length; i++) {
			String vm;
			try {
				vm = VMUtil.getVMInstallName(eeObjects[i]);
			} catch (CoreException e) {
				vm = PDEUIMessages.BasicLauncherTab_unbound;
			}
			ees[i] = NLS.bind("{0} ({1})", new String[] {eeObjects[i].getId(), vm}); //$NON-NLS-1$
		}
		Arrays.sort(ees, getComparator());
		fEeCombo.setItems(ees);
	}

	private Comparator getComparator() {
		return new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		};
	}

	public String validate() {
		if (fEeButton.getSelection() && fEeCombo.getText().indexOf(PDEUIMessages.BasicLauncherTab_unbound) != -1)
			return NLS.bind(PDEUIMessages.BasicLauncherTab_noJreForEeMessage, parseEESelection(fEeCombo.getText()));
		return null;
	}

	private String parseEESelection(String selection) {
		int index = selection.indexOf(" ("); //$NON-NLS-1$
		if (index == -1)
			return selection;
		return selection.substring(0, index);
	}
}
