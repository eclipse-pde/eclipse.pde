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
	private Button fFailOnError;
	private Button fVerbose;
	private Button fDebugInfo;
	private Combo fJavacSource;
	private Combo fJavacTarget;

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
		
		fFailOnError = new Button(composite, SWT.CHECK);
		fFailOnError.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.failOnError")); //$NON-NLS-1$
		fFailOnError.setSelection(store.getBoolean(PROP_JAVAC_FAIL_ON_ERROR));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fFailOnError.setLayoutData(gd);
		
		fVerbose = new Button(composite, SWT.CHECK);
		fVerbose.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.compilerVerbose")); //$NON-NLS-1$
		fVerbose.setSelection(store.getBoolean(PROP_JAVAC_VERBOSE));
		gd = new GridData();
		gd.horizontalSpan = 2;
		fVerbose.setLayoutData(gd);
		
		fDebugInfo = new Button(composite, SWT.CHECK);
		fDebugInfo.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.compilerDebug")); //$NON-NLS-1$
		fDebugInfo.setSelection(store.getBoolean(PROP_JAVAC_DEBUG_INFO));
		gd = new GridData();
		gd.horizontalSpan = 2;
		fDebugInfo.setLayoutData(gd);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.javacSource")); //$NON-NLS-1$
		
		fJavacSource = new Combo(composite, SWT.READ_ONLY);
		fJavacSource.setItems(new String[] {"1.3", "1.4"}); //$NON-NLS-1$ //$NON-NLS-2$
		fJavacSource.setText(store.getString(PROP_JAVAC_SOURCE));
		gd = new GridData();
		gd.widthHint = 100;
		fJavacSource.setLayoutData(gd);
		
		label = new Label(composite, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("BuildPropertiesPreferencePage.javacTarget")); //$NON-NLS-1$
			
		fJavacTarget = new Combo(composite, SWT.READ_ONLY);
		fJavacTarget.setItems(new String[] {"1.1", "1.2", "1.3", "1.4"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		fJavacTarget.setText(store.getString(PROP_JAVAC_TARGET));
		gd = new GridData();
		gd.widthHint = 100;
		fJavacTarget.setLayoutData(gd);
		
		Dialog.applyDialogFont(composite);
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		store.setValue(PROP_JAVAC_FAIL_ON_ERROR, fFailOnError.getSelection());
		store.setValue(PROP_JAVAC_VERBOSE, fVerbose.getSelection());
		store.setValue(PROP_JAVAC_DEBUG_INFO, fDebugInfo.getSelection());
		store.setValue(PROP_JAVAC_SOURCE, fJavacSource.getText());
		store.setValue(PROP_JAVAC_TARGET, fJavacTarget.getText());
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fFailOnError.setSelection(store.getDefaultBoolean(PROP_JAVAC_FAIL_ON_ERROR));
		fVerbose.setSelection(store.getDefaultBoolean(PROP_JAVAC_VERBOSE));
		fDebugInfo.setSelection(store.getDefaultBoolean(PROP_JAVAC_DEBUG_INFO));
		fJavacSource.setText(store.getDefaultString(PROP_JAVAC_SOURCE));
		fJavacTarget.setText(store.getDefaultString(PROP_JAVAC_TARGET));
	}
}
