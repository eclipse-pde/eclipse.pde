package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class RuntimeWorkbenchArgumentsTab extends BasicLauncherTab {
	
	private Button fShowSplashCheck;
	
	protected void createShowSplashSection(Composite parent) {
		fShowSplashCheck = new Button(parent, SWT.CHECK);
		fShowSplashCheck.setText(PDEPlugin.getResourceString("BasicLauncherTab.showSplash"));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fShowSplashCheck.setLayoutData(gd);
		fShowSplashCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void initializeShowSplashSection(ILaunchConfiguration config)
		throws CoreException {
		fShowSplashCheck.setSelection(config.getAttribute(SHOW_SPLASH, true));		
	}
	
	public void doRestoreDefaults() {
		super.doRestoreDefaults();
		fShowSplashCheck.setSelection(true);
	}
	
	protected void saveShowSplashSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(SHOW_SPLASH, fShowSplashCheck.getSelection());		
	}

}
