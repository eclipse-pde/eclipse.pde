package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.ui.IWorkbench;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @see PreferencePage
 */
public class BuildOptionsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IPreferenceConstants {
	private IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	private Button failOnError;
	private Button verbose;
	private Button debugInfo;
	private Combo javacSource;
	private Combo javacTarget;

	/**
	 *
	 */
	public BuildOptionsPreferencePage() {
		setDescription(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.desc")); //$NON-NLS-1$
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
		
		failOnError = new Button(composite, SWT.CHECK);
		failOnError.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.failOnError")); //$NON-NLS-1$
		failOnError.setSelection(store.getBoolean(PROP_JAVAC_FAIL_ON_ERROR));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		failOnError.setLayoutData(gd);
		
		verbose = new Button(composite, SWT.CHECK);
		verbose.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.compilerVerbose")); //$NON-NLS-1$
		verbose.setSelection(store.getBoolean(PROP_JAVAC_VERBOSE));
		gd = new GridData();
		gd.horizontalSpan = 2;
		verbose.setLayoutData(gd);
		
		debugInfo = new Button(composite, SWT.CHECK);
		debugInfo.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.compilerDebug")); //$NON-NLS-1$
		debugInfo.setSelection(store.getBoolean(PROP_JAVAC_DEBUG_INFO));
		gd = new GridData();
		gd.horizontalSpan = 2;
		debugInfo.setLayoutData(gd);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.javacSource")); //$NON-NLS-1$
		
		javacSource = new Combo(composite, SWT.READ_ONLY);
		javacSource.setItems(new String[] {"1.3", "1.4"}); //$NON-NLS-1$ //$NON-NLS-2$
		javacSource.select(javacSource.indexOf(store.getString(PROP_JAVAC_SOURCE)));
		gd = new GridData();
		gd.widthHint = 50;
		javacSource.setLayoutData(gd);
		
		label = new Label(composite, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.javacTarget")); //$NON-NLS-1$
			
		javacTarget = new Combo(composite, SWT.READ_ONLY);
		javacTarget.setItems(new String[] {"1.1", "1.2", "1.3", "1.4"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		javacTarget.select(javacTarget.indexOf(store.getString(PROP_JAVAC_TARGET)));
		gd = new GridData();
		gd.widthHint = 50;
		javacTarget.setLayoutData(gd);
		
		Dialog.applyDialogFont(composite);
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		store.setValue(PROP_JAVAC_FAIL_ON_ERROR, failOnError.getSelection());
		store.setValue(PROP_JAVAC_VERBOSE, verbose.getSelection());
		store.setValue(PROP_JAVAC_DEBUG_INFO, debugInfo.getSelection() ? "on" : "off"); //$NON-NLS-1$ //$NON-NLS-2$
		store.setValue(PROP_JAVAC_SOURCE, javacSource.getText());
		store.setValue(PROP_JAVAC_TARGET, javacTarget.getText());
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
}
