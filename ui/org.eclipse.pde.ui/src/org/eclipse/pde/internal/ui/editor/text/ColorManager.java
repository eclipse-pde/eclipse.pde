/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;

import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
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
}
