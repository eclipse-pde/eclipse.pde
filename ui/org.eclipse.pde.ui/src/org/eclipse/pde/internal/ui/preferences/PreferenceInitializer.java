package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.preferences.*;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public class PreferenceInitializer extends AbstractPreferenceInitializer implements IPreferenceConstants {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		ColorManager.initializeDefaults(store);
		store.setDefault(P_USE_SOURCE_PAGE, false);
		store.setDefault(PROP_SHOW_OBJECTS, VALUE_USE_IDS);
	}

}
