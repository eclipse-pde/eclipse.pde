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

/**
 */
public class PDEBasePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String PROP_PLATFORM_PATH="org.eclipse.pde.platformPath";
	public static final String PROP_PLATFORM_LOCATION="org.eclipse.pde.platformLocation";
	public static final String KEY_PLATFORM_HOME ="Preferences.MainPage.PlatformHome";
	public static final String KEY_PLATFORM_HOME_BUTTON ="Preferences.MainPage.PlatformHome.Button";
	public static final String KEY_ARGUMENTS ="Preferences.MainPage.Arguments";
	public static final String KEY_VM_ARGUMENTS ="Preferences.MainPage.VMArguments";
	public static final String KEY_DESCRIPTION ="Preferences.MainPage.Description";
	public static final String KEY_LOCATION ="Preferences.MainPage.Location";
	public static final String KEY_LOCATION_BUTTON ="Preferences.MainPage.Location.Button";
	public static final String PROP_PLATFORM_ARGS="org.eclipse.pde.platformArgs";
	public static final String PROP_VM_ARGS="org.eclipse.pde.vmArgs";
	
	public static final String RT_WORKSPACE = "runtime-workspace";
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
			GridData gd = (GridData)text.getLayoutData();
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
public PDEBasePreferencePage() {
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
	DirectoryFieldEditor platform =
		new ModifiedDirectoryFieldEditor(
			PROP_PLATFORM_LOCATION,
			PDEPlugin.getResourceString(KEY_LOCATION),
			getFieldEditorParent(),
			PDEPlugin.getResourceString(KEY_LOCATION_BUTTON));
	StringFieldEditor args =
		new StringFieldEditorWithSpace(
			PROP_PLATFORM_ARGS,
			PDEPlugin.getResourceString(KEY_ARGUMENTS),
			getFieldEditorParent());
	StringFieldEditor vmargs =
		new StringFieldEditorWithSpace(
			PROP_VM_ARGS,
			PDEPlugin.getResourceString(KEY_VM_ARGUMENTS),
			getFieldEditorParent());

	ExternalPluginsEditor plugins = new ExternalPluginsEditor(getFieldEditorParent());
	addField(editor);
	addField(platform);
	addField(args);
	addField(vmargs);
	addField(plugins);
}
/**
 * @return java.lang.String
 */
public String getPlatformPath() {
	String value=editor.getStringValue();
	if (value!=null) value.trim();
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
		path = ppath.toOSString();
		store.setDefault(PROP_PLATFORM_PATH, path);
		store.setValue(PROP_PLATFORM_PATH, path);
		IPath runtimeWorkspace = ppath.append(RT_WORKSPACE);
		boolean locationSet = store.contains(PROP_PLATFORM_LOCATION);
		path = runtimeWorkspace.toOSString();
		store.setDefault(PROP_PLATFORM_LOCATION, path);
		if (!locationSet)
			store.setValue(PROP_PLATFORM_LOCATION, path);
	}
}
/** 
 *
 */
public boolean performOk() {
	IPreferenceStore store = getPreferenceStore();
	String oldEclipseHome = store.getString(PROP_PLATFORM_PATH);
	String newEclipseHome = editor.getStringValue();
	if (!oldEclipseHome.equals(newEclipseHome)) {
		// home changed -update Java variable
		ExternalModelManager.setEclipseHome(newEclipseHome);
	}
	return super.performOk();
}
}
