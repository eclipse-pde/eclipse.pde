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

import java.io.*;
import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.tests.macro.*;
import org.eclipse.ui.*;
import org.eclipse.ui.intro.*;

import junit.framework.*;

public abstract class PDETestCase extends TestCase {
	
	private static boolean FIRST_TEST = true;
	
	private static IWorkbench fWorkbench;

	protected void setUp() throws Exception {
		if (FIRST_TEST) {
			fWorkbench = PlatformUI.getWorkbench();
			
				// close intro
			IIntroManager intro = fWorkbench.getIntroManager();
			intro.closeIntro(intro.getIntro());
		
			// open PDE perspective
			fWorkbench.showPerspective(PDEPlugin.PERSPECTIVE_ID, fWorkbench.getActiveWorkbenchWindow());
			
			// set to false
			FIRST_TEST = false;
		}	
	}
	
	protected void playScript(String scriptName) {
		InputStream stream = null;
		try {
			MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
			URL url = Platform.getBundle("org.eclipse.pde.ui.tests").getEntry("scripts/" + scriptName);
			if (url == null)
				fail("Script \"" + scriptName + "\" could not be found");
			stream = url.openStream();			
			recorder.play(fWorkbench.getDisplay(), fWorkbench.getActiveWorkbenchWindow(), stream);
			recorder.shutdown();
		} catch (CoreException e) {
			fail("Error playing the script: \"" + scriptName + "\"");
		} catch (IOException e) {			
		} finally {
			try {
				if (stream != null) 
					stream.close();
			} catch (IOException e) {
			}
		}
	}
	
	protected void tearDown() {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		try {
			for (int i = 0; i < projects.length; i++) {
				projects[i].delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			System.out.print(e.getStackTrace());
		}
	}
	
}
