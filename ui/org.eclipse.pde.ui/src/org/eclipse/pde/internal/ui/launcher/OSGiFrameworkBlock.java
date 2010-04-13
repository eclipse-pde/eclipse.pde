/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class OSGiFrameworkBlock {

	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	private IConfigurationElement[] fConfigElements;
	private Combo fLauncherCombo;
	private Listener fListener;
	private AbstractLauncherTab fTab;
	private Combo fLaunchWithCombo;
	private BlockAdapter fBlock;

	class Listener extends SelectionAdapter implements ModifyListener {

		public void widgetSelected(SelectionEvent e) {
			if (e.widget == fLaunchWithCombo) {
				setActiveIndex();
			}
			fTab.updateLaunchConfigurationDialog();
		}

		public void modifyText(ModifyEvent e) {
			fTab.updateLaunchConfigurationDialog();
		}
	}

	/**
	 * Constructs a new instance of this block
	 * @param tab parent launch config tab
	 * @param block the content block that will contain the UI for modifying included bundles (block changes based on bundle/feature mode)
	 */
	public OSGiFrameworkBlock(AbstractLauncherTab tab, BlockAdapter block) {
		fTab = tab;
		fBlock = block;
		fConfigElements = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager().getSortedFrameworks();
		fListener = new Listener();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(8, false);
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);

		Label launchLabel = new Label(composite, SWT.NONE);
		launchLabel.setText(PDEUIMessages.PluginsTab_launchWith);
		gd = new GridData();
		gd.horizontalIndent = 5;
		launchLabel.setLayoutData(gd);

		fLaunchWithCombo = SWTFactory.createCombo(composite, SWT.READ_ONLY, 1, SWT.NONE, new String[] {PDEUIMessages.OSGiFrameworkBlock_selectedBundles, PDEUIMessages.PluginsTab_customFeatureMode});

		fLaunchWithCombo.addSelectionListener(fListener);

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.OSGiBundlesTab_frameworkLabel);
		gd = new GridData();
		gd.horizontalIndent = 5;
		label.setLayoutData(gd);

		fLauncherCombo = new Combo(composite, SWT.READ_ONLY);
		for (int i = 0; i < fConfigElements.length; i++)
			fLauncherCombo.add(fConfigElements[i].getAttribute("name")); //$NON-NLS-1$
		fLauncherCombo.addSelectionListener(fListener);

		label = new Label(composite, SWT.NONE);
		gd = new GridData();
		gd.horizontalIndent = 5;
		label.setLayoutData(gd);
		label.setText(PDEUIMessages.EquinoxPluginsTab_defaultStart);

		fDefaultStartLevel = new Spinner(composite, SWT.BORDER);
		fDefaultStartLevel.setMinimum(1);
		fDefaultStartLevel.addModifyListener(fListener);

		label = new Label(composite, SWT.NONE);
		gd = new GridData();
		gd.horizontalIndent = 5;
		label.setLayoutData(gd);
		label.setText(PDEUIMessages.EquinoxPluginsTab_defaultAuto);

		fDefaultAutoStart = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		fDefaultAutoStart.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
		fDefaultAutoStart.select(0);
		fDefaultAutoStart.addSelectionListener(fListener);
		fDefaultAutoStart.setLayoutData(new GridData());

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 8;
		label.setLayoutData(gd);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		initializeFramework(config);
		boolean auto = config.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		fDefaultAutoStart.setText(Boolean.toString(auto));
		int level = config.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		fDefaultStartLevel.setSelection(level);
	}

	private void initializeFramework(ILaunchConfiguration config) throws CoreException {
		boolean usePlugins = config.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		fLaunchWithCombo.select(usePlugins ? 0 : 1);
		setActiveIndex();

		OSGiFrameworkManager manager = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager();
		String id = config.getAttribute(IPDELauncherConstants.OSGI_FRAMEWORK_ID, manager.getDefaultFramework());

		for (int i = 0; i < fConfigElements.length; i++) {
			if (id.equals(fConfigElements[i].getAttribute(OSGiFrameworkManager.ATT_ID))) {
				fLauncherCombo.select(i);
				return;
			}
		}
		if (fLauncherCombo.getItemCount() > 0)
			fLauncherCombo.select(0);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.USE_DEFAULT, fLaunchWithCombo.getSelectionIndex() == 0);
		config.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, fLaunchWithCombo.getSelectionIndex() == 1);
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, Boolean.toString(true).equals(fDefaultAutoStart.getText()));
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, fDefaultStartLevel.getSelection());

		int index = fLauncherCombo.getSelectionIndex();
		String id = index > -1 ? fConfigElements[index].getAttribute(OSGiFrameworkManager.ATT_ID) : null;
		OSGiFrameworkManager manager = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager();

		// no need to persist the default OSGi framework
		if (manager.getDefaultFramework().equals(id))
			id = null;
		config.setAttribute(IPDELauncherConstants.OSGI_FRAMEWORK_ID, id);
	}

	public int getDefaultStartLevel() {
		return fDefaultStartLevel.getSelection();
	}

	public void setActiveIndex() {
		if (fBlock != null) {
			fBlock.setActiveBlock(fLaunchWithCombo.getSelectionIndex() + 1); // +1 to match plug-ins tab combo indices
		}
	}
}
