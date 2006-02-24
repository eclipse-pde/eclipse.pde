/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.ui.tests.macro.MacroManager;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.eclipse.ui.IWorkbench;

public class ScriptRunner {
	
	public static void run(String scriptName, IWorkbench workbench) {
		InputStream stream = null;
		try {
			MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
			URL url = Platform.getBundle("org.eclipse.pde.ui.tests").getEntry("scripts/" + scriptName);
			if (url == null)
				Assert.fail("Script \"" + scriptName + "\" could not be found");
			stream = url.openStream();			
			recorder.play(workbench.getDisplay(), workbench.getActiveWorkbenchWindow(), scriptName, stream);
			recorder.shutdown();
		} catch (CoreException e) {
			Assert.fail("Error playing the script: \"" + scriptName + "\"");
		} catch (IOException e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}
		}
	}
	
}
