package org.eclipse.pde.internal.preferences;

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


public class ManifestPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String PROP_UPDATE_BUILD_PATH="org.eclipse.pde.updateBuildPath";
	public static final String PROP_PLATFORM_LOCATION="org.eclipse.pde.platformLocation";
	public static final String KEY_UPDATE_BUILD_PATH ="Preferences.ManifestPage.updateBuildPath";
	public static final String KEY_DESCRIPTION = "Preferences.ManifestPage.description";
	private BooleanFieldEditor editor;

public ManifestPreferencePage() {
	super(GRID);
	setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
	setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
}

protected void createFieldEditors() {
	editor =
		new BooleanFieldEditor(
			PROP_UPDATE_BUILD_PATH,
			PDEPlugin.getResourceString(KEY_UPDATE_BUILD_PATH),
			getFieldEditorParent());
	addField(editor);
}

public static boolean getUpdateBuildPath() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	if (store.contains(PROP_UPDATE_BUILD_PATH))
	   return store.getBoolean(PROP_UPDATE_BUILD_PATH);
    return true;
}

/**
 * Initializes this preference page using the passed desktop.
 *
 * @param desktop the current desktop
 */
public void init(IWorkbench workbench) {
}
}
