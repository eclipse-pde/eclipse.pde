package org.eclipse.pde.internal.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.net.URL;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;
import sun.security.action.GetBooleanAction;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.util.*;

/**
 */
public class TargetPlatformPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	public static final String PROP_PLATFORM_PATH = "org.eclipse.pde.platformPath";
	public static final String KEY_PLATFORM_HOME =
		"Preferences.TargetPlatformPage.PlatformHome";
	public static final String KEY_PLATFORM_HOME_BUTTON =
		"Preferences.TargetPlatformPage.PlatformHome.Button";
	public static final String KEY_DESCRIPTION =
		"Preferences.TargetPlatformPage.Description";

	public static final String PROP_TARGET_MODE = "org.eclipse.pde.targetMode";
	public static final String KEY_TARGET_MODE =
		"Preferences.TargetPlatformPage.targetMode";
	public static final String KEY_USE_THIS =
		"Preferences.TargetPlatformPage.useThis";
	public static final String KEY_USE_OTHER =
		"Preferences.TargetPlatformPage.useOther";
	public static final String VALUE_USE_THIS = "useThis";
	public static final String VALUE_USE_OTHER = "useOther";

	private ModifiedDirectoryFieldEditor targetPathEditor;
	private RadioGroupFieldEditor modeEditor;
	private ExternalPluginsEditor pluginsEditor;
	private boolean useOther;

	class ModifiedDirectoryFieldEditor extends DirectoryFieldEditor {
		public ModifiedDirectoryFieldEditor(
			String property,
			String label,
			Composite parent,
			String buttonLabel) {
			super(property, label, parent);
			setChangeButtonText(buttonLabel);
		}
		public void adjustForNumColumns(int ncolumns) {
			super.adjustForNumColumns(ncolumns);
			Text text = super.getTextControl();
			GridData gd = (GridData) text.getLayoutData();
			gd.horizontalAlignment = GridData.FILL;
			gd.grabExcessHorizontalSpace = true;
		}
		// It is OK to specify directory that does not
		// exist - the platform will create it
		protected boolean doCheckState() {
			return true;
		}
		void setEnabled(boolean enabled) {
			Text text = getTextControl();
			text.setEnabled(enabled);
			super.getLabelControl().setEnabled(enabled);
			super.getChangeControl(text.getParent()).setEnabled(enabled);
		}
	}

	class ModifiedChoiceEditor extends RadioGroupFieldEditor {
		ModifiedChoiceEditor(Composite parent) {
			super(
				PROP_TARGET_MODE,
				PDEPlugin.getResourceString(KEY_TARGET_MODE),
				1,
				new String[][] {
					{ PDEPlugin.getResourceString(KEY_USE_THIS), VALUE_USE_THIS },
					{
					PDEPlugin.getResourceString(KEY_USE_OTHER), VALUE_USE_OTHER }
			}, parent);
		}
		protected void fireValueChanged(
			String property,
			Object oldValue,
			Object newValue) {
			super.fireValueChanged(property, oldValue, newValue);
			if (oldValue.equals(newValue) == false)
				modeChanged(newValue.equals(VALUE_USE_OTHER));
		}
	}

	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
		getPreferenceStore().setDefault(PROP_TARGET_MODE, VALUE_USE_THIS);
		String value = getPreferenceStore().getString(PROP_TARGET_MODE);
		useOther = value.equals(VALUE_USE_OTHER);
	}
	/**
	 */
	protected void createFieldEditors() {
		modeEditor = new ModifiedChoiceEditor(getFieldEditorParent());
		addField(modeEditor);
		modeEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				Object value = e.getNewValue();
				modeChanged(value.equals(VALUE_USE_OTHER));
			}
		});

		targetPathEditor =
			new ModifiedDirectoryFieldEditor(
				PROP_PLATFORM_PATH,
				PDEPlugin.getResourceString(KEY_PLATFORM_HOME),
				getFieldEditorParent(),
				PDEPlugin.getResourceString(KEY_PLATFORM_HOME_BUTTON));

		pluginsEditor = new ExternalPluginsEditor(getFieldEditorParent());
		pluginsEditor.setUseOther(useOther);
		addField(targetPathEditor);
		addField(pluginsEditor);
		modeChanged(useOther);
	}

	private void modeChanged(boolean useOther) {
		String oldPath = getPlatformPath();
		targetPathEditor.setEnabled(useOther);
		this.useOther = useOther;
		pluginsEditor.setUseOther(useOther);
		String newPath = getPlatformPath();
		boolean reloadNeeded = false;
		if (oldPath != null && newPath == null)
			reloadNeeded = true;
		if (oldPath == null && newPath != null)
			reloadNeeded = true;
		if (oldPath.equals(newPath) == false)
			reloadNeeded = true;
		if (reloadNeeded)
			pluginsEditor.reload();
	}
	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}
	/**
	 */

	public static void initializePlatformPath() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		boolean useThis = true;
		String mode = store.getString(PROP_TARGET_MODE);

		if (mode != null && mode.equals(VALUE_USE_OTHER))
			useThis = false;
		String path = store.getString(PROP_PLATFORM_PATH);
		String currentPath = computeDefaultPlatformPath();

		if (path == null
			|| path.length() == 0
			|| (useThis && !currentPath.equals(path))) {
			path = currentPath;
			store.setDefault(PROP_PLATFORM_PATH, path);
			store.setValue(PROP_PLATFORM_PATH, path);
		}
	}

	public static boolean getUseOther() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		boolean useOther = false;
		String mode = store.getString(PROP_TARGET_MODE);
		if (mode != null && mode.equals(VALUE_USE_OTHER))
			useOther = true;
		return useOther;
	}

	private static String computeDefaultPlatformPath() {
		URL installURL = BootLoader.getInstallURL();
		String file = installURL.getFile();
		IPath ppath = new Path(file).removeTrailingSeparator();
		return getCorrectPath(ppath.toOSString());
	}

	private static String getCorrectPath(String path) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (TargetPlatform.getOS().equals("win32")) {
				if (i == 0 && c == '/')
					continue;
			}
			// Some VMs may return %20 instead of a space
			if (c == '%' && i + 2 < path.length()) {
				char c1 = path.charAt(i + 1);
				char c2 = path.charAt(i + 2);
				if (c1 == '2' && c2 == '0') {
					i += 2;
					continue;
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}

	String getPlatformPath() {
		if (useOther) {
			String value = targetPathEditor.getStringValue();
			if (value != null)
				value.trim();
			return value;
		} else
			return computeDefaultPlatformPath();
	}

	/** 
	 *
	 */
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		String oldEclipseHome = store.getString(PROP_PLATFORM_PATH);
		final String newEclipseHome = getPlatformPath();
		if (!oldEclipseHome.equals(newEclipseHome)) {
			// home changed -update Java variable
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					ExternalModelManager.setEclipseHome(newEclipseHome, monitor);
				}
			};
			ProgressMonitorDialog pm = new ProgressMonitorDialog(getControl().getShell());
			try {
				pm.run(true, false, op);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			}
		}
		return super.performOk();
	}
}