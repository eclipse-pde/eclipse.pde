package org.eclipse.pde.internal.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PluginDevelopmentPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	private Button fOSGiButton;
	
	public PluginDevelopmentPage() {
		noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fOSGiButton = new Button(composite, SWT.CHECK);
		fOSGiButton.setText(PDEUIMessages.PluginDevelopmentPage_osgiCheckbox);
		fOSGiButton.setLayoutData(new GridData());

		initialize();
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void initialize() {
		Preferences pref = getPreferences((IProject)getElement());
		if (pref != null) {
			fOSGiButton.setSelection(pref.getBoolean(PDECore.PURE_OSGI, false));
		}
	}
	
	private Preferences getPreferences(IProject project) {
		return new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fOSGiButton.setEnabled(false);
	}
	
	public boolean performOk() {
		Preferences pref = getPreferences((IProject)getElement());
		if (pref != null) {
			if (fOSGiButton.getSelection())
				pref.putBoolean(PDECore.PURE_OSGI, true);	
			else
				pref.remove(PDECore.PURE_OSGI);
			
			try {
				pref.flush();
			} catch (BackingStoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return super.performOk();
	}
	
}
