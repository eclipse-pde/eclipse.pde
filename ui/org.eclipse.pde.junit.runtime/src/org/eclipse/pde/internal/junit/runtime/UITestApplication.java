/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.*;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */ 
public class UITestApplication implements IPlatformRunnable {
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.boot.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(final Object args) throws Exception {
		String className = getClassName((String[])args);
		if (className == null)
			return null;
		
		final IWorkbench workbench = (IWorkbench)Class.forName(className).newInstance();
		final boolean [] started = {false};
		workbench.addWindowListener(new IWindowListener() {
			public void windowOpened(IWorkbenchWindow w) {
				if (started[0]) return;
				w.getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						started[0]=true;
						RemotePluginTestRunner.main((String[])args);
						workbench.close();
					}
				});
			}
			public void windowActivated(IWorkbenchWindow window) {}
			public void windowDeactivated(IWorkbenchWindow window) {}
			public void windowClosed(IWorkbenchWindow window) {}
		});
		((IPlatformRunnable)workbench).run(args); 
		return null;
	}
	
	private String getClassName(String[] args) {
		IExtension extPoint =
			Platform.getPluginRegistry().getExtension(
				"org.eclipse.core.runtime.applications",
				getExtensionPointId(args));
		if (extPoint != null) {
			IConfigurationElement[] elements = extPoint.getConfigurationElements();
			for (int i = 0; i < elements.length; i++) {
				IConfigurationElement[] runs = elements[i].getChildren("run");
				for (int j = 0; j < runs.length; j++) {
					String className = runs[j].getAttribute("class");
					if (className != null && className.length() > 0)
						return className;
				}			
			}
		}
		return null;
	}
	
	private String getExtensionPointId(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-applicationExtension") && i < args.length -1)
				return args[i+1];
		}
		return "org.eclipse.ui.workbench";
	}
	
	
}