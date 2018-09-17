/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Combo;

/**
 * Provides global access to the saved list of bundle providers.  Provides helper methods
 * to load and save provider names from and to combo boxes.  The bundle provider combo
 * is used in the plug-in, feature, fragment, and feature patch wizards.
 *
 * @since 3.8
 */
public class BundleProviderHistoryUtil {
	/**
	 * Key for bundle providers list saved in dialog settings
	 */
	private final static String S_PROVIDERS = "providers"; //$NON-NLS-1$

	/**
	 * Loads a list of providers into the provided combo control from the provided dialog settings
	 *
	 * @param combo The combo control to modify
	 * @param settings The dialog settings to look up the list in
	 */
	public static void loadHistory(Combo combo, IDialogSettings settings) {
		if (null == combo || null == settings) {
			return;
		}
		String providers[] = settings.getArray(S_PROVIDERS);
		if (null != providers) {
			for (String provider : providers) {
				combo.add(provider);
			}
		}
	}

	/**
	 * Saves the items of the given combo to the given settings.
	 * <p>
	 * If the combo text is not in the list of history items yet, it will be
	 * added. The combo text will be the most recent history value (saved first).
	 * </p>
	 *
	 * @param combo The combo to get the list/text from
	 * @param settings The dialog settings to save the list into
	 */
	public static void saveHistory(Combo combo, IDialogSettings settings) {
		if (null == combo) {
			return;
		}
		// Save all providers from the combo list + the newly entered
		String text = combo.getText();
		if (0 == text.length()) {
			return;
		}
		int indexOfText = combo.indexOf(text);
		// If the item was already in the list, remove it now
		if (indexOfText != -1) {
			combo.remove(indexOfText);
		}
		// And always add the entered text as the most recent one to the top of
		// the list
		combo.add(text, 0);
		String[] items = combo.getItems();
		if (items != null && items.length > 0) {
			settings.put(S_PROVIDERS, items);
		}
		String text2 = combo.getText();
		if (text2 != null && text2.isEmpty() && !text.isEmpty()) {
			combo.setText(text);
		}

	}
}