/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.preferences.PDEPreferencesUtil;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
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
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			// When a radio button selection changes, we get two events.  One for the deselection of the old button and another for the selection 
			// of the new button.  We only need to update the configuration once when the selection changes.  Hence, we can ignore the deselection 
			// event of the old button.
			if (source instanceof Button && !((Button)source).getSelection())
				return;
			fTab.updateLaunchConfigurationDialog();
			if (source == fJreButton || source == fEeButton)
				updateJREEnablement();
		}
		public void modifyText(ModifyEvent e) {
			fTab.updateLaunchConfigurationDialog();
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
		fJreButton = new Button(parent, SWT.RADIO);
		fJreButton.setText(PDEUIMessages.BasicLauncherTab_jre);
		fJreButton.addSelectionListener(fListener);
		
		fJreCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		fJreCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fJreCombo.addSelectionListener(fListener);
		
		fJrePrefButton = new Button(parent, SWT.PUSH);
		fJrePrefButton.setText(PDEUIMessages.BasicLauncherTab_installedJREs); 
		fJrePrefButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String currentVM = fJreCombo.getText();
				String currentEE = parseEESelection(fEeCombo.getText());
				boolean useDefault = VMHelper.getDefaultVMInstallName().equals(currentVM);
				String[] pageIDs = new String[] {"org.eclipse.jdt.debug.ui.preferences.VMPreferencePage"}; //$NON-NLS-1$
				if (PDEPreferencesUtil.showPreferencePage(pageIDs, fTab.getControl().getShell())) {
					setJRECombo();
					if (useDefault || fJreCombo.indexOf(currentVM) == -1)
						fJreCombo.setText(VMHelper.getDefaultVMInstallName());
					else
						fJreCombo.setText(currentVM);
					setEECombo();
					setEEComboSelection(currentEE);
				}
			}
		});
		fJrePrefButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		SWTUtil.setButtonDimensionHint(fJrePrefButton);	
		
		fEeButton = new Button(parent, SWT.RADIO);
		fEeButton.setText(PDEUIMessages.BasicLauncherTab_ee);
		fEeButton.addSelectionListener(fListener);
		
		fEeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		fEeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fEeCombo.addSelectionListener(fListener);
		
		fEePrefButton = new Button(parent, SWT.PUSH);
		fEePrefButton.setText(PDEUIMessages.BasicLauncherTab_environments); 
		fEePrefButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String currentEE = parseEESelection(fEeCombo.getText());
				String[] pageIDs = new String[] {"org.eclipse.jdt.debug.ui.jreProfiles"}; //$NON-NLS-1$
				if (PDEPreferencesUtil.showPreferencePage(pageIDs, fTab.getControl().getShell())) {
					setEECombo();
					setEEComboSelection(currentEE);
				}
			}
		});
		fEePrefButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		SWTUtil.setButtonDimensionHint(fEePrefButton);	
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
		fJavaButton.setText("&java");	 //$NON-NLS-1$
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
		
		boolean useVMInstall = config.getAttribute(IPDELauncherConstants.USE_VMINSTALL, true); //$NON-NLS-1$
		fJreButton.setSelection(useVMInstall); //$NON-NLS-1$
		fEeButton.setSelection(!useVMInstall);
		
		setJRECombo();
		String vmInstallName =
			config.getAttribute(IPDELauncherConstants.VMINSTALL, VMHelper.getDefaultVMInstallName());
		fJreCombo.setText(vmInstallName);
		if (fJreCombo.getSelectionIndex() == -1)
			fJreCombo.setText(VMHelper.getDefaultVMInstallName());
		
		setEECombo();
		String eeId =
			config.getAttribute(IPDELauncherConstants.EXECUTION_ENVIRONMENT, (String) null);
		setEEComboSelection(eeId);
		
		updateJREEnablement();
	}
	
	private void setEEComboSelection (String eeId) {
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
		try {
			String javaCommand = fJavawButton.getSelection() ? null : "java"; //$NON-NLS-1$
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, javaCommand);
			
			config.setAttribute(IPDELauncherConstants.USE_VMINSTALL, fJreButton.getSelection());
			if (fJreButton.getSelection()) {
				if (fJreCombo.getSelectionIndex() == -1)
					return;
	
				String jre = fJreCombo.getText();
				if (config.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null) != null) {
					config.setAttribute(IPDELauncherConstants.VMINSTALL, jre);
				} else {
					config.setAttribute(
							IPDELauncherConstants.VMINSTALL,
						jre.equals(VMHelper.getDefaultVMInstallName()) ? null : jre);
				}
			} else {
				if (fEeCombo.getSelectionIndex() == -1)
					return;
				
				config.setAttribute(IPDELauncherConstants.EXECUTION_ENVIRONMENT, parseEESelection(fEeCombo.getText()));
			}
		} catch (CoreException e) {
		}
	}

	protected void saveBootstrapEntriesSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, fBootstrap.getText().trim());
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {		
		config.setAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
	}
	
	private void setJRECombo() {
		String[] jres = VMHelper.getVMInstallNames();
		Arrays.sort(jres, getComparator());
		fJreCombo.setItems(jres);
	}
	
	private void setEECombo() {
		IExecutionEnvironment[] eeObjects = VMHelper.getExecutionEnvironments();
		String[] ees = new String[eeObjects.length];
		for (int i = 0; i < eeObjects.length; i++) {
			String vm;
			try {
				vm = VMHelper.getVMInstallName(eeObjects[i]);
			} catch (CoreException e) {
				vm = PDEUIMessages.BasicLauncherTab_unbound;
			}
			ees[i] = NLS.bind("{0} ({1})", new String[] { eeObjects[i].getId(), vm }); //$NON-NLS-1$
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
	
	private String parseEESelection (String selection) {
		int index = selection.indexOf(" ("); //$NON-NLS-1$
		if (index == -1)
			return selection;
		return selection.substring(0, index);
	}
}
