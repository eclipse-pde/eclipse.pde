package org.eclipse.pde.internal.editor.text;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public interface IColorManager {

	void dispose();
	Color getColor(RGB rgb);
}
