/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.LinkedList;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Manages the list of launch shortcuts available in the {@link PDELauncherFormEditor}.
 * Provides a recommended order for actions that is persisted between workbench launches.
 * This could also be applied to {@link LaunchShortcutOverviewPage} in the future.
 * @since 4.3
 */
public class PDEEditorLaunchManager {

	private static final String SETTINGS_SECTION = "PDEFormActionManager"; //$NON-NLS-1$
	private static final String SETTINGS_RECENT_LAUNCHES = "RecentLaunches"; //$NON-NLS-1$
	private static final int MAX_LAUNCHES = 3;

	private static PDEEditorLaunchManager fDefault;
	private List<String> recentLaunches;

	/**
	 * Returns the static default instance of this manager
	 * @return the default manager
	 */
	public static PDEEditorLaunchManager getDefault() {
		if (fDefault == null) {
			fDefault = new PDEEditorLaunchManager();
		}
		return fDefault;
	}

	/**
	 * Marks the launcher with the given launcher id as being recently launched
	 *
	 * @param id launcher id, 'id' attribute of the PDE launch shortcut extension
	 */
	public void setRecentLaunch(String id) {
		if (recentLaunches == null) {
			initialize();
		}
		// Reorder list to put this id on top
		int currentIndex = recentLaunches.indexOf(id);
		if (currentIndex == -1) {
			recentLaunches.add(0, id);
		} else if (currentIndex != 0) {
			recentLaunches.remove(currentIndex);
			recentLaunches.add(0, id);
		}

		// As only single items can be added to the list, just remove the last item if we are over max
		if (recentLaunches.size() > MAX_LAUNCHES) {
			recentLaunches.remove(recentLaunches.size() - 1);
		}

		// Persist the settings
		persist();
	}

	/**
	 * Returns an ordered list of String launcher ids taken from PDE launch
	 * shortcut extensions.  The most recently launched entry is at
	 * index 0.
	 *
	 * @return ordered list of launcher IDs, possibly empty
	 */
	public List<String> getRecentLaunches() {
		if (recentLaunches == null) {
			initialize();
		}
		return recentLaunches;
	}

	private void initialize() {
		recentLaunches = new LinkedList<>();
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettingsSection(SETTINGS_SECTION);
		String[] result = settings.getArray(SETTINGS_RECENT_LAUNCHES);
		if (result != null) {
			for (String resultString : result) {
				recentLaunches.add(resultString);
			}
		}
	}

	private void persist() {
		if (recentLaunches != null) {
			IDialogSettings settings = PDEPlugin.getDefault().getDialogSettingsSection(SETTINGS_SECTION);
			String[] result = recentLaunches.toArray(new String[recentLaunches.size()]);
			settings.put(SETTINGS_RECENT_LAUNCHES, result);
		}
	}

}
