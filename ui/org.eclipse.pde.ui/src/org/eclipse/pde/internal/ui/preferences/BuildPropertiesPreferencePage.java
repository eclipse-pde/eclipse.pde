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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @see PreferencePage
 */
public class BuildPropertiesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IPreferenceConstants {
	private IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	private Button failOnError;
	private Button verbose;
	private Button debugInfo;
	private Combo javacSource;
	private Combo javacTarget;

	/**
	 *
	 */
	public BuildPropertiesPreferencePage() {
		setDescription("Options for building plug-ins, fragments and features:");
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
		composite.setLayout(layout);
		
		failOnError = new Button(composite, SWT.CHECK);
		failOnError.setText("Abort building upon compilation errors");
		failOnError.setSelection(store.getBoolean(PROP_JAVAC_FAIL_ON_ERROR));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		failOnError.setLayoutData(gd);
		
		verbose = new Button(composite, SWT.CHECK);
		verbose.setText("Run compiler with verbose output");
		verbose.setSelection(store.getBoolean(PROP_JAVAC_VERBOSE));
		gd = new GridData();
		gd.horizontalSpan = 2;
		verbose.setLayoutData(gd);
		
		debugInfo = new Button(composite, SWT.CHECK);
		debugInfo.setText("Compile source with debug information");
		debugInfo.setSelection(store.getBoolean(PROP_JAVAC_DEBUG_INFO));
		gd = new GridData();
		gd.horizontalSpan = 2;
		debugInfo.setLayoutData(gd);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText("Source Compatibility:");
		
		javacSource = new Combo(composite, SWT.READ_ONLY);
		javacSource.setItems(new String[] {"1.3", "1.4"});
		javacSource.select(javacSource.indexOf(store.getString(PROP_JAVAC_SOURCE)));
		gd = new GridData();
		gd.widthHint = 50;
		javacSource.setLayoutData(gd);
		
		label = new Label(composite, SWT.NONE);
		label.setText("Generated .class files compatibility:");
			
		javacTarget = new Combo(composite, SWT.READ_ONLY);
		javacTarget.setItems(new String[] {"1.1", "1.2", "1.3", "1.4"});
		javacTarget.select(javacTarget.indexOf(store.getString(PROP_JAVAC_TARGET)));
		gd = new GridData();
		gd.widthHint = 50;
		javacTarget.setLayoutData(gd);
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		store.setValue(PROP_JAVAC_FAIL_ON_ERROR, failOnError.getSelection());
		store.setValue(PROP_JAVAC_VERBOSE, verbose.getSelection());
		store.setValue(PROP_JAVAC_DEBUG_INFO, debugInfo.getSelection() ? "on" : "off");
		store.setValue(PROP_JAVAC_SOURCE, javacSource.getText());
		store.setValue(PROP_JAVAC_TARGET, javacTarget.getText());
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
}
