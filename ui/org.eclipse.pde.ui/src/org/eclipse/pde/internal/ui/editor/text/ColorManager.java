package org.eclipse.pde.internal.ui.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;

import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

public class ColorManager implements IColorManager, IPDEColorConstants {

	private static Map fColorTable = new HashMap(10);
	private static int counter = 0;

	public ColorManager() {
		initialize();
		counter++;
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
		}
	}

	private void putColor(IPreferenceStore pstore, String property) {
		RGB setting = PreferenceConverter.getColor(pstore, property);
		Color color = new Color(Display.getCurrent(), setting);
		fColorTable.put(property, color);
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
