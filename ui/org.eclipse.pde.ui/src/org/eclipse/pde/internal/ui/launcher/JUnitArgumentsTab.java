package org.eclipse.pde.internal.ui.launcher;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


public class JUnitArgumentsTab extends BasicLauncherTab {
	
	private Combo fApplicationCombo;
	private Label fApplicationLabel;
	private Button fRequiresUI;
	
	protected void createApplicationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Test Application");
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fRequiresUI = new Button(group, SWT.CHECK);
		fRequiresUI.setText("&Plug-in Tests require a user interface");
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fRequiresUI.setLayoutData(gd);
		fRequiresUI.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fApplicationLabel.setEnabled(fRequiresUI.getSelection());
				fApplicationCombo.setEnabled(fRequiresUI.getSelection());
				updateLaunchConfigurationDialog();
			}
		});
		
		fApplicationLabel = new Label(group, SWT.NONE);
		fApplicationLabel.setText(PDEPlugin.getResourceString("JUnitArgumentsTab.applicationName"));
		gd = new GridData();
		gd.horizontalIndent = 25;
		fApplicationLabel.setLayoutData(gd);
		
		fApplicationCombo = new Combo(group, SWT.READ_ONLY|SWT.DROP_DOWN);
		fApplicationCombo.setItems(getApplicationNames());
		fApplicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fApplicationCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});		
	}
	
	protected void initializeApplicationSection(ILaunchConfiguration config)
		throws CoreException {
		String junitApplication =
			config.getAttribute(APPLICATION, JUnitLaunchConfiguration.fgDefaultApp);
		fRequiresUI.setSelection(
			junitApplication.equals(JUnitLaunchConfiguration.fgDefaultApp));

		fApplicationLabel.setEnabled(fRequiresUI.getSelection());
		fApplicationCombo.setEnabled(fRequiresUI.getSelection());
		String testApplication = config.getAttribute(APP_TO_TEST, (String) null);
		if (testApplication == null
			|| testApplication.equals("")
			|| fApplicationCombo.indexOf(testApplication) == -1) {
			int index = fApplicationCombo.indexOf("org.eclipse.ui.ide.workbench");
			if (index == -1) {
				index = fApplicationCombo.indexOf("org.eclipse.ui.workbench");
			}
			if (index != -1) {
				fApplicationCombo.setText(fApplicationCombo.getItem(index));
			} else if (fApplicationCombo.getItemCount() > 0) {
				fApplicationCombo.setText(fApplicationCombo.getItem(0));
			}
		} else {
			fApplicationCombo.setText(testApplication);
		}
	}
	
	protected void doRestoreDefaults() {
		super.doRestoreDefaults();
		fRequiresUI.setSelection(true);
		fApplicationLabel.setEnabled(true);
		fApplicationCombo.setEnabled(true);
	}
	
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		try {
			if (fRequiresUI.getSelection()) {
				config.setAttribute(APPLICATION, JUnitLaunchConfiguration.fgDefaultApp);
				String text = fApplicationCombo.getText();
				if ((config.getAttribute(APP_TO_TEST, (String) null) != null)
					|| (!text.equals("org.eclipse.ui.workbench")
						&& !text.equals("org.eclipse.ui.ide.workbench"))) {
					config.setAttribute(APP_TO_TEST, fApplicationCombo.getText());
				}
			} else {
				config.setAttribute(
					APPLICATION,
					JUnitLaunchConfiguration.fgApplicationNames[1]);
			}
		} catch (CoreException e) {
		}
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(
			LOCATION + "0",
			LauncherUtils.getDefaultPath().append("runtime-test-workspace").toOSString());
		config.setAttribute(DOCLEAR, true);
		config.setAttribute(PROGARGS, LauncherUtils.getDefaultProgramArguments());
		config.setAttribute(ASKCLEAR, false);
		config.setAttribute(VMARGS, "");
	}
	
	private String[] getApplicationNames() {
		TreeSet result = new TreeSet();
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.applications")) {
					String id = extensions[j].getPluginBase().getId() + "." + extensions[j].getId();
					if (id != null && !id.startsWith("org.eclipse.pde.junit.runtime")){
						result.add(id);
					}
				}
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}

}
