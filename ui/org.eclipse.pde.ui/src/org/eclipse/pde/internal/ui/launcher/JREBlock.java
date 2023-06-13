/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *     Les Jones <lesojones@gmail.com> - bug 195433, 218210
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

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
		@Override
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			// When a radio button selection changes, we get two events.  One for the deselection of the old button and another for the selection
			// of the new button.  We only need to update the configuration once when the selection changes.  Hence, we can ignore the deselection
			// event of the old button.
			if (source instanceof Button && !((Button) source).getSelection())
				return;
			fTab.updateLaunchConfigurationDialog();
			if (source == fEeCombo || source == fEeButton || source == fJreCombo || source == fJreButton) {
				updateBootstrapEnablement();
			}
			if (source == fJreButton || source == fEeButton)
				updateJREEnablement();
		}

		@Override
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
		fEePrefButton.addSelectionListener(widgetSelectedAdapter(e -> {
			String currentEE = parseEESelection(fEeCombo.getText());
			if (SWTFactory.showPreferencePage(fTab.getControl().getShell(), "org.eclipse.jdt.debug.ui.jreProfiles", null) == Window.OK) { //$NON-NLS-1$
				// The launch dialog may have been closed while the preference page was open
				if (!fTab.getControl().isDisposed()) {
					setEECombo();
					setEEComboSelection(currentEE);
				}
			}
		}));
		fEePrefButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		SWTUtil.setButtonDimensionHint(fEePrefButton);

		fJreButton = new Button(parent, SWT.RADIO);
		fJreButton.setText(PDEUIMessages.BasicLauncherTab_jre);
		fJreButton.addSelectionListener(fListener);

		fJreCombo = SWTFactory.createCombo(parent, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
		fJreCombo.addSelectionListener(fListener);

		fJrePrefButton = new Button(parent, SWT.PUSH);
		fJrePrefButton.setText(PDEUIMessages.BasicLauncherTab_installedJREs);
		fJrePrefButton.addSelectionListener(widgetSelectedAdapter(e -> {
			String currentVM = fJreCombo.getText();
			String currentEE = parseEESelection(fEeCombo.getText());
			boolean useDefault = VMUtil.getDefaultVMInstallName().equals(currentVM);
			if (SWTFactory.showPreferencePage(fTab.getControl().getShell(), "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", null) == Window.OK) { //$NON-NLS-1$
				// The launch dialog may have been closed while the preference page was open
				if (!fTab.getControl().isDisposed()) {
					setJRECombo();
					if (useDefault || fJreCombo.indexOf(currentVM) == -1)
						fJreCombo.setText(VMUtil.getDefaultVMInstallName());
					else
						fJreCombo.setText(currentVM);
					setEECombo();
					setEEComboSelection(currentEE);
					updateBootstrapEnablement();
				}
			}
		}));
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
		String javawText = NLS.bind(PDEUIMessages.BasicLauncherTab_javaExecDefault, javaCommand);
		fJavawButton.setText(javawText);
		fJavawButton.setSelection(javaCommand.equals("javaw")); //$NON-NLS-1$
		fJavaButton.setSelection(!fJavawButton.getSelection());

		String jre = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
		IPath jrePath = null;
		if (jre != null) {
			jrePath = IPath.fromPortableString(jre);
		}
		String vmInstallName = null;
		String eeId = null;
		if (jrePath == null) {
			// Try to get a default EE based on default VM install first
			IVMInstall install = JavaRuntime.getDefaultVMInstall();
			IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager()
					.getExecutionEnvironments();
			for (IExecutionEnvironment iExecutionEnvironment : systemEnvs) {
				if (iExecutionEnvironment.isStrictlyCompatible(install)) {
					eeId = iExecutionEnvironment.getId();
					break;
				}
			}
			// Try to get a default EE based on the selected plug-ins in the config
			if (eeId == null) {
				eeId = VMHelper.getDefaultEEName(config);
			}
			if (eeId == null) {
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
		setEECombo();
		setJREComboSelection(vmInstallName);
		setEEComboSelection(eeId);

		updateJREEnablement();
		updateBootstrapEnablement();
	}

	private void updateBootstrapEnablement() {
		IPath jrePath = null;
		if (fJreButton.getSelection()) {
			if (fJreCombo.getSelectionIndex() != -1) {
				String jreName = fJreCombo.getText();
				IVMInstall install = VMHelper.getVMInstall(jreName);
				// remove the name to make portable
				jrePath = JavaRuntime.newJREContainerPath(install);
			}
		} else {
			if (fEeCombo.getSelectionIndex() != -1) {
				IExecutionEnvironment environment = VMUtil
						.getExecutionEnvironment(parseEESelection(fEeCombo.getText()));
				if (environment != null) {
					jrePath = JavaRuntime.newJREContainerPath(environment);
				}
			}
		}
		if (jrePath != null) {
			IVMInstall vmInstall = JavaRuntime.getVMInstall(jrePath);
			if (vmInstall != null) {
				boolean modularJava = JavaRuntime.isModularJava(vmInstall);
				fBootstrap.setEnabled(!modularJava);
			}
		}

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

	private void setJREComboSelection(String vmInstallName) {
		if (vmInstallName != null) {
			fJreCombo.setText(vmInstallName);
		}
		if (fJreCombo.getSelectionIndex() == -1) {
			fJreCombo.setText(VMUtil.getDefaultVMInstallName());
		}
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
				IVMInstall install = VMHelper.getVMInstall(jreName);
				// remove the name to make portable
				jrePath = JavaRuntime.newJREContainerPath(install);
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
		fEeCombo.setItems(ees);
	}

	private Comparator<Object> getComparator() {
		return (arg0, arg1) -> arg0.toString().compareTo(arg1.toString());
	}

	public String validate() {
		if (fEeButton.getSelection() && fEeCombo.getText().contains(PDEUIMessages.BasicLauncherTab_unbound))
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
