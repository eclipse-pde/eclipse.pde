package org.eclipse.pde.internal.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PluginDevelopmentPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	private Button fExtensionButton;
	private Button fEquinoxButton;
	
	public PluginDevelopmentPage() {
		noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group group = new Group(composite, SWT.NONE);
		group.setText("Plug-in Manifest Editor Presentation");
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fExtensionButton = new Button(group, SWT.CHECK);
		fExtensionButton.setText("&Always show the Extensions and Extension Points tabs");

		fEquinoxButton = new Button(group, SWT.CHECK);
		fEquinoxButton.setText("&Show sections specific to the Equinox OSGi framework");

		initialize();
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void initialize() {
		Preferences pref = getPreferences((IProject)getElement());
		if (pref != null) {
			fExtensionButton.setSelection(pref.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true));
			fEquinoxButton.setSelection(pref.getBoolean(ICoreConstants.EQUINOX_PROPERTY, true));
		}
	}
	
	private Preferences getPreferences(IProject project) {
		return new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
	}
	
	public boolean performOk() {
		Preferences pref = getPreferences((IProject)getElement());
		if (pref != null) {
			if (!fExtensionButton.getSelection())
				pref.putBoolean(ICoreConstants.EXTENSIONS_PROPERTY, false);	
			else
				pref.remove(ICoreConstants.EXTENSIONS_PROPERTY);
			
			if (!fEquinoxButton.getSelection())
				pref.putBoolean(ICoreConstants.EQUINOX_PROPERTY, false);	
			else
				pref.remove(ICoreConstants.EQUINOX_PROPERTY);
			
			try {
				pref.flush();
			} catch (BackingStoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return super.performOk();
	}
	
}
