package org.eclipse.pde.internal.ui.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.Color;

public interface IColorManager {

	void dispose();
	Color getColor(String key);
}
