/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class ColorManager implements IColorManager, IPDEColorConstants {

	private static ColorManager fColorManager;
	private Map<String, Color> fColorTable = new HashMap<>(5);
	private static int counter = 0;

	public ColorManager() {
		initialize();
	}

	public static IColorManager getDefault() {
		if (fColorManager == null)
			fColorManager = new ColorManager();

		counter += 1;
		return fColorManager;
	}

	public static void initializeDefaults(IPreferenceStore store) {
		PreferenceConverter.setDefault(store, P_DEFAULT, DEFAULT);
		PreferenceConverter.setDefault(store, P_PROC_INSTR, PROC_INSTR);
		PreferenceConverter.setDefault(store, P_STRING, STRING);
		PreferenceConverter.setDefault(store, P_EXTERNALIZED_STRING, EXTERNALIZED_STRING);
		PreferenceConverter.setDefault(store, P_TAG, TAG);
		PreferenceConverter.setDefault(store, P_XML_COMMENT, XML_COMMENT);
		PreferenceConverter.setDefault(store, P_HEADER_KEY, HEADER_KEY);
		PreferenceConverter.setDefault(store, P_HEADER_OSGI, HEADER_OSGI);
		store.setDefault(P_HEADER_OSGI + IPDEColorConstants.P_BOLD_SUFFIX, true);
		PreferenceConverter.setDefault(store, P_HEADER_VALUE, HEADER_VALUE);
		PreferenceConverter.setDefault(store, P_HEADER_ATTRIBUTES, HEADER_ATTRIBUTES);
		store.setDefault(P_HEADER_ATTRIBUTES + IPDEColorConstants.P_ITALIC_SUFFIX, true);
		PreferenceConverter.setDefault(store, P_HEADER_ASSIGNMENT, HEADER_ASSIGNMENT);
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		try {
			Display display = PlatformUI.getWorkbench().getDisplay();
			Runnable runnable = () -> {
				if (!display.isDisposed() && display.getHighContrast()) {
					PreferenceConverter.setDefault(store, P_DEFAULT, DEFAULT_HIGH_CONTRAST);
					PreferenceConverter.setDefault(store, P_HEADER_VALUE, HEADER_VALUE_HIGH_CONTRAST);
					PreferenceConverter.setDefault(store, P_HEADER_ATTRIBUTES, HEADER_ASSIGNMENT_HIGH_CONTRAST);
				}
			};
			if (display == Display.getCurrent()) {
				runnable.run();
			} else {
				display.asyncExec(runnable);
			}
		} catch (SWTException | IllegalStateException e) {
			// keep non-high-contrast-mode defaults
		}
	}

	private void initialize() {
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		putColor(pstore, P_DEFAULT);
		putColor(pstore, P_PROC_INSTR);
		putColor(pstore, P_STRING);
		putColor(pstore, P_EXTERNALIZED_STRING);
		putColor(pstore, P_TAG);
		putColor(pstore, P_XML_COMMENT);
		putColor(pstore, P_HEADER_KEY);
		putColor(pstore, P_HEADER_OSGI);
		putColor(pstore, P_HEADER_VALUE);
		putColor(pstore, P_HEADER_ATTRIBUTES);
		putColor(pstore, P_HEADER_ASSIGNMENT);
		pstore = PreferenceConstants.getPreferenceStore();
		for (String color : IColorManager.PROPERTIES_COLORS) {
			putColor(pstore, color);
		}
	}

	public void disposeColors(boolean resetSingleton) {
		Iterator<Color> e = fColorTable.values().iterator();
		while (e.hasNext())
			e.next().dispose();
		if (resetSingleton)
			fColorManager = null;

	}

	@Override
	public void dispose() {
		counter--;
		if (counter == 0)
			disposeColors(true);
	}

	private void putColor(IPreferenceStore pstore, String property) {
		putColor(property, PreferenceConverter.getColor(pstore, property));
	}

	private void putColor(String property, RGB setting) {
		Color oldColor = fColorTable.get(property);
		if (oldColor != null) {
			if (oldColor.getRGB().equals(setting))
				return;
			oldColor.dispose();
		}
		fColorTable.put(property, new Color(Display.getCurrent(), setting));
	}

	@Override
	public Color getColor(String key) {
		Color color = fColorTable.get(key);
		if (color == null)
			color = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		return color;
	}

	@Override
	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		Object color = event.getNewValue();
		if (color instanceof RGB) {
			putColor(event.getProperty(), (RGB) color);
		} else {
			putColor(event.getProperty(), StringConverter.asRGB(color.toString()));
		}
	}
}
