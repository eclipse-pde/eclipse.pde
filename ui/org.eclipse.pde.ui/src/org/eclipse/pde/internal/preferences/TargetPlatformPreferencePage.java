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
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import java.lang.reflect.InvocationTargetException;

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

	private DirectoryFieldEditor editor;

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
			gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;
			gd.grabExcessHorizontalSpace = true;
			Button button = super.getChangeControl(text.getParent());
			button.setLayoutData(null);
		}
		// It is OK to specify directory that does not
		// exist - the platform will create it
		protected boolean doCheckState() {
			return true;
		}
	}

	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	}
	/**
	 */
	protected void createFieldEditors() {
		editor =
			new ModifiedDirectoryFieldEditor(
				PROP_PLATFORM_PATH,
				PDEPlugin.getResourceString(KEY_PLATFORM_HOME),
				getFieldEditorParent(),
				PDEPlugin.getResourceString(KEY_PLATFORM_HOME_BUTTON));

		ExternalPluginsEditor plugins =
			new ExternalPluginsEditor(getFieldEditorParent());
		addField(editor);
		addField(plugins);
	}
	/**
	 * @return java.lang.String
	 */
	public String getPlatformPath() {
		String value = editor.getStringValue();
		if (value != null)
			value.trim();
		return value;
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
		String path = store.getString(PROP_PLATFORM_PATH);
		if (path == null || path.length() == 0) {
			URL installURL = BootLoader.getInstallURL();
			String file = installURL.getFile();
			IPath ppath = new Path(file).removeTrailingSeparator();
			path = getCorrectPath(ppath.toOSString());
			store.setDefault(PROP_PLATFORM_PATH, path);
			store.setValue(PROP_PLATFORM_PATH, path);
		}
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

	/** 
	 *
	 */
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		String oldEclipseHome = store.getString(PROP_PLATFORM_PATH);
		final String newEclipseHome = editor.getStringValue();
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