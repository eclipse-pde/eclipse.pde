/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.jface.window.Window;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */ 
public class UITestApplication extends Workbench {
	/**
	 * Run an event loop for the workbench.
	 */
	
	protected void runEventLoop(Window.IExceptionHandler handler) {
		// Dispatch all events.
		Display display = Display.getCurrent();
		while (true) {
			try {
				if (!display.readAndDispatch())
					break;
			} catch (Throwable e) {
				break;
			}
		}

		// Run all tests.
		String[] arguments= getCommandLineArgs();
		
		RemotePluginTestRunner.main(arguments);		
		// Close the workbench.
		close();		
	}
}