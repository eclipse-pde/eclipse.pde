package org.eclipse.pde.internal.ui.wizards.product;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;

public class NewProductFileWizadPage extends WizardNewFileCreationPage {
	
	private Button fButton;
	private Label fLabel;
	private Combo fCombo;
	
	public NewProductFileWizadPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setDescription("Create a new product configuration file and initialize its content.\nThe file name must end with '.prod'.");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAdvancedControls(Composite parent) {
		String[] launchConfigs = getLaunchConfigurations();
		if (launchConfigs.length == 0)
			return;
		
		Group group = new Group(parent, SWT.NONE);
		group.setText("Initial content");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fButton = new Button(group, SWT.CHECK);
		fButton.setText("Initialize the file with data from an existing launch configuration");
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fButton.setLayoutData(gd);
		fButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fButton.getSelection();
				fLabel.setEnabled(enabled);
				fCombo.setEnabled(enabled);
			}
		});
		
		fLabel = new Label(group, SWT.NONE);
		fLabel.setText("Launch configurations:");
		gd = new GridData();
		gd.horizontalIndent = 25;
		fLabel.setLayoutData(gd);
		fLabel.setEnabled(false);
		
		fCombo = new Combo(group, SWT.SINGLE|SWT.READ_ONLY);
		fCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fCombo.setItems(launchConfigs);
		fCombo.setText(fCombo.getItem(0));
		fCombo.setEnabled(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		if (!getFileName().trim().endsWith(".prod")) {
			setErrorMessage("The file name must end with '.prod'");
			return false;
		}
		return super.validatePage();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
	 */
	protected void createLinkTarget() {
	}
	
	private String[] getLaunchConfigurations() {
		ArrayList list = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench");
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i]))
					list.add(configs[i].getName());
			}
		} catch (CoreException e) {
		}
		return (String[])list.toArray(new String[list.size()]);
	}
	
	public ILaunchConfiguration getSelectedLaunchConfiguration() {
		if (!fButton.getSelection() || !fButton.isEnabled())
			return null;
		
		String configName = fCombo.getText();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench");
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configs.length; i++) {
				if (configs[i].getName().equals(configName) && !DebugUITools.isPrivate(configs[i]))
					return configs[i];
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
}
