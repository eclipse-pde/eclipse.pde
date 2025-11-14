package org.eclipse.pde.genericeditor.extension.tests;

import org.eclipse.swt.widgets.Display;

public class TestUtils {

	public static void processUIEvents() {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			while (display.readAndDispatch()) {
				// Keep pumping events until the queue is empty
			}
		}
	}
}
