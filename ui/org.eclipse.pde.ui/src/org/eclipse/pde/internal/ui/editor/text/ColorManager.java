/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager implements IColorManager, IPDEColorConstants {

	private static ColorManager fColorManager;
	private Map fColorTable = new HashMap(5);
	private static int counter = 0;

	public ColorManager() {
		initialize();
	}
	
	public static ColorManager getDefault(){
		if (fColorManager == null){
			fColorManager = new ColorManager();
		}
		counter++;
		return fColorManager;
	}

	public static void initializeDefaults(IPreferenceStore store) {
		PreferenceConverter.setDefault(store, P_DEFAULT, DEFAULT);
		PreferenceConverter.setDefault(store, P_PROC_INSTR, PROC_INSTR);
		PreferenceConverter.setDefault(store, P_STRING, STRING);
		PreferenceConverter.setDefault(store, P_TAG, TAG);
		PreferenceConverter.setDefault(store, P_XML_COMMENT, XML_COMMENT);
	}

	private void initialize() {
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		putColor(pstore, P_DEFAULT);
		putColor(pstore, P_PROC_INSTR);
		putColor(pstore, P_STRING);
		putColor(pstore, P_TAG);
		putColor(pstore, P_XML_COMMENT);
		pstore = JavaPlugin.getDefault().getCombinedPreferenceStore();
		for (int i = 0; i < IColorManager.PROPERTIES_COLORS.length; i++) {
			String property = IColorManager.PROPERTIES_COLORS[i];
			RGB setting = PreferenceConverter.getColor(pstore, property);
			fColorTable.put(property, new Color(Display.getCurrent(), setting));
		}
	}

	public void dispose() {
		counter--;
		if (counter == 0) {
			Iterator e = fColorTable.values().iterator();
			while (e.hasNext())
				 ((Color) e.next()).dispose();
			fColorManager = null;
		}
	}
	
	private void putColor(IPreferenceStore pstore, String property) {
		RGB setting = PreferenceConverter.getColor(pstore, property);
		Color oldColor = (Color) fColorTable.get(property);
		if (oldColor != null){
			if (oldColor.getRGB().equals(setting))
				return;
			oldColor.dispose();
		}
		fColorTable.put(property, new Color(Display.getCurrent(), setting));
	}

	public void updateProperty(String property){
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		putColor(pstore, property);
	}

	public Color getColor(String key) {
		Color color = (Color) fColorTable.get(key);
		if (color == null) {
			color =
				Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		}
		return color;
	}

	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		Object color = event.getNewValue();
		if (color instanceof String) {
			RGB setting = StringConverter.asRGB((String)color, null);
			if (setting != null)
				fColorTable.put(event.getProperty(), new Color(Display.getCurrent(), setting));
		}
	}
}
