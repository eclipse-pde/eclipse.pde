package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * @see PreferencePage
 */
public class InProgressPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, ICoreConstants {
	private Button enableRuntimeSupport;
	private Preferences store = PDECore.getDefault().getPluginPreferences();
	/**
	 *
	 */
	public InProgressPreferencePage() {
		//setDescription("Work in Progress"); //$NON-NLS-1$
	}

	/**
	 * @see PreferencePage#init
	 */
	public void init(IWorkbench workbench)  {
	}

	/**
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite parent)  {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 15;
		composite.setLayout(layout);
		
		enableRuntimeSupport = new Button(composite, SWT.CHECK);
		enableRuntimeSupport.setText("&Enable OSGi runtime support"); //$NON-NLS-1$
		enableRuntimeSupport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				warnRestart();
			}
		});
		enableRuntimeSupport.setSelection(store.getBoolean(ENABLE_ALT_RUNTIME));
		
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	private void warnRestart() {
		boolean newValue = enableRuntimeSupport.getSelection();
		boolean oldValue = store.getBoolean(ENABLE_ALT_RUNTIME);
		if (newValue!=oldValue)
			MessageDialog.openInformation(getShell(), "PDE Work in Progress", 
				"The platform must be restarted for this change to take effect.");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		store.setValue(ENABLE_ALT_RUNTIME, enableRuntimeSupport.getSelection());
		PDECore.getDefault().savePluginPreferences();
		return super.performOk();
	}
}
