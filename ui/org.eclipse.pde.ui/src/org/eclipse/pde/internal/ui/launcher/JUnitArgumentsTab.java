package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class JUnitArgumentsTab extends BasicLauncherTab {
	
	private Combo applicationCombo;
	
	protected void createApplicationSection(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("JUnitArgumentsTab.applicationName"));
		
		applicationCombo = new Combo(parent, SWT.READ_ONLY|SWT.DROP_DOWN);
		applicationCombo.setItems(JUnitLaunchConfiguration.fgApplicationNames);
		applicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		applicationCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void initializeApplicationSection(ILaunchConfiguration config)
		throws CoreException {
		applicationCombo.setText(
			config.getAttribute(APPLICATION, JUnitLaunchConfiguration.fgDefaultApp));
	}
	
	protected void doRestoreDefaults() {
		super.doRestoreDefaults();
		applicationCombo.setText(JUnitLaunchConfiguration.fgDefaultApp);
	}
	
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(APPLICATION, applicationCombo.getText());
	}

}
