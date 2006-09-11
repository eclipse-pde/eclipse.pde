/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class OSGiFrameworkBlock {
	
	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	private IConfigurationElement[] fConfigElements;
	private Combo fLauncherCombo;
	private Listener fListener;
	private AbstractLauncherTab fTab;
	
	class Listener extends SelectionAdapter implements ModifyListener{
		
		public void widgetSelected(SelectionEvent e) {
			fTab.updateLaunchConfigurationDialog();
		}

		public void modifyText(ModifyEvent e) {
			fTab.updateLaunchConfigurationDialog();
		}
	}
	
	public OSGiFrameworkBlock(AbstractLauncherTab tab) {
		fTab = tab;
		fConfigElements = PDEPlugin.getDefault().getOSGiFrameworkManager().getSortedFrameworks();
		fListener = new Listener();
	}
	
	public void createControl(Composite parent) {
		Group composite = new Group(parent, SWT.NONE);
		composite.setLayout(new GridLayout(6, false));
		composite.setText(PDEUIMessages.OSGiFrameworkBlock_defaultGroup);

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.OSGiBundlesTab_frameworkLabel);

		fLauncherCombo = new Combo(composite, SWT.READ_ONLY);
		for (int i = 0; i < fConfigElements.length; i++) 
			fLauncherCombo.add(fConfigElements[i].getAttribute("name")); //$NON-NLS-1$
		fLauncherCombo.addModifyListener(fListener);
		
		label = new Label(composite, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);
		label.setText(PDEUIMessages.EquinoxPluginsTab_defaultStart);

		fDefaultStartLevel = new Spinner(composite, SWT.BORDER);
		fDefaultStartLevel.setMinimum(1);
		fDefaultStartLevel.addModifyListener(fListener);

		label = new Label(composite, SWT.NONE);
		gd = new GridData();
		gd.horizontalIndent = 20;
		label.setLayoutData(gd);
		label.setText(PDEUIMessages.EquinoxPluginsTab_defaultAuto);

		fDefaultAutoStart = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		fDefaultAutoStart.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
		fDefaultAutoStart.select(0);
		fDefaultAutoStart.addSelectionListener(fListener);
	}
	
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		initializeFramework(config);
		boolean auto = config.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		fDefaultAutoStart.setText(Boolean.toString(auto));
		int level = config.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		fDefaultStartLevel.setSelection(level);
	}
	
	private void initializeFramework(ILaunchConfiguration config) throws CoreException {
		OSGiFrameworkManager manager = PDEPlugin.getDefault().getOSGiFrameworkManager();
		String id = config.getAttribute(IPDELauncherConstants.OSGI_FRAMEWORK_ID, 
				  					    manager.getDefaultFramework());
		
		for (int i = 0; i < fConfigElements.length; i++) {
			if (id.equals(fConfigElements[i].getAttribute(OSGiFrameworkManager.ATT_ID))){ 
				fLauncherCombo.select(i);
				return;
			}
		}
		if (fLauncherCombo.getItemCount() > 0)
			fLauncherCombo.select(0);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, 
				Boolean.toString(true).equals(fDefaultAutoStart.getText()));
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, fDefaultStartLevel.getSelection());
		
		int index = fLauncherCombo.getSelectionIndex();
		String id = index > -1 ? fConfigElements[index].getAttribute(OSGiFrameworkManager.ATT_ID) : null;
		OSGiFrameworkManager manager = PDEPlugin.getDefault().getOSGiFrameworkManager();

		// no need to persist the default OSGi framework
		if (manager.getDefaultFramework().equals(id))
			id = null;
		config.setAttribute(IPDELauncherConstants.OSGI_FRAMEWORK_ID, id);
	}
	
	public int getDefaultStartLevel() { 
		return fDefaultStartLevel.getSelection();
	}
}
