/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.junit.runtime;

import junit.framework.Assert;

import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.*;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */ 
public class UITestApplication implements IPlatformRunnable {
	
	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench";
	private static final String DEFAULT_APP_PRE_3_0 = "org.eclipse.ui.workbench";
	
	private boolean fInDeprecatedMode = false;
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.boot.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(final Object args) throws Exception {
		// Get the application to test
		IPlatformRunnable application = getApplication((String[])args);
		
		Assert.assertNotNull(application);
		
		if (fInDeprecatedMode) {
			runDeprecatedApplication(application, args);
		} else {
			runApplication(application, args);
		}
		
		return null;
	}
	

	/*
	 * return the application to run, or null if not even the default application
	 * is found.
	 */
	private IPlatformRunnable getApplication(String[] args) throws CoreException {
		// Assume we are in 3.0 mode.
		// Find the name of the application as specified by the PDE JUnit launcher.
		// If no application is specified, the 3.0 default workbench application
		// is returned.
		IExtension extension =
			Platform.getPluginRegistry().getExtension(
				Platform.PI_RUNTIME,
				Platform.PT_APPLICATIONS,
				getApplicationToRun(args));
		
		// If no 3.0 extension can be found, search the registry
		// for the pre-3.0 default workbench application, i.e. org.eclipse ui.workbench
		// Set the deprecated flag to true
		if (extension == null) {
			extension = Platform.getPluginRegistry().getExtension(
					Platform.PI_RUNTIME,
					Platform.PT_APPLICATIONS,
					DEFAULT_APP_PRE_3_0);
			fInDeprecatedMode = true;
		}
		
		Assert.assertNotNull(extension);
		
		// If the extension does not have the correct grammar, return null.
		// Otherwise, return the application object.
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run");
			if (runs.length > 0) {
				Object runnable = runs[0].createExecutableExtension("class");
				if (runnable instanceof IPlatformRunnable)
					return (IPlatformRunnable) runnable;
			}
		}
		return null;
	}
	
	/*
	 * The -testApplication argument specifies the application to be run.
	 * If the PDE JUnit launcher did not set this argument, then return
	 * the name of the default application.
	 * In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
	 * 
	 */
	private String getApplicationToRun(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-testApplication") && i < args.length -1)
				return args[i+1];
		}
		return DEFAULT_APP_3_0;
	}
	
	/**
	 * In 3.0 mode
	 * 
	 */
	private void runApplication(IPlatformRunnable application, Object args) {
		
	}
	/*
	 * If we are in pre-3.0 mode, then the application to run is "org.eclipse.ui.workbench"
	 * Therefore, we safely cast the runnable object to IWorkbenchWindow.
	 * We add a listener to it, so that we know when the window opens so that we 
	 * can start running the tests.
	 * When the tests are done, we explicitly call close() on the workbench.
	 */
	private void runDeprecatedApplication(
			IPlatformRunnable object,
			final Object args)
			throws Exception {
			final IWorkbench workbench = (IWorkbench) object;
			// this flag is used so that we only run tests when the window is opened
			// for the first time only.
			final boolean[] started = { false };
			workbench.addWindowListener(new IWindowListener() {
				public void windowOpened(IWorkbenchWindow w) {
					if (started[0])
						return;
					w.getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							started[0] = true;
							RemotePluginTestRunner.main((String[]) args);
							workbench.close();
						}
					});
				}
				public void windowActivated(IWorkbenchWindow window) {
				}
				public void windowDeactivated(IWorkbenchWindow window) {
				}
				public void windowClosed(IWorkbenchWindow window) {
				}
			});
			((IPlatformRunnable) workbench).run(args);
		}
	
		
}