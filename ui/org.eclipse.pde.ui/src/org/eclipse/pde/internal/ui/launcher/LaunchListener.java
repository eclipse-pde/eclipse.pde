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
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.program.*;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LaunchListener
	implements ILaunchListener, IDebugEventSetListener {
	private ArrayList managedLaunches;

	public LaunchListener() {
		managedLaunches = new ArrayList();
	}

	public void manage(ILaunch launch) {
		if (managedLaunches.size() == 0)
			hookListener(true);

		if (!managedLaunches.contains(launch))
			managedLaunches.add(launch);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
		update(launch, true);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}

	private void update(ILaunch launch, boolean remove) {
		if (managedLaunches.contains(launch)) {
			if (remove || launch.isTerminated()) {
				managedLaunches.remove(launch);
				if (managedLaunches.size() == 0) {
					hookListener(false);
				}
			}
		}
	}

	private void hookListener(boolean add) {
		DebugPlugin debugPlugin = DebugPlugin.getDefault();
		ILaunchManager launchManager = debugPlugin.getLaunchManager();
		if (add) {
			launchManager.addLaunchListener(this);
			debugPlugin.addDebugEventListener(this);
		} else {
			launchManager.removeLaunchListener(this);
			debugPlugin.removeDebugEventListener(this);
		}
	}

	private void doRestart(ILaunch launch) {
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		try {
			config.launch(launch.getLaunchMode(), new NullProgressMonitor());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void shutdown() {
		hookListener(false);
	}
	/**
	 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent)
	 */
	public void handleDebugEvents(DebugEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			DebugEvent event = events[i];
			Object source = event.getSource();
			if (source instanceof IProcess
				&& event.getKind() == DebugEvent.TERMINATE) {
				IProcess process = (IProcess) source;
				ILaunch launch = process.getLaunch();
				if (launch != null) {
					try {
						launchTerminated(launch, process.getExitValue());
					} catch (DebugException e) {
					}
				}
			}
		}
	}

	private void launchTerminated(final ILaunch launch, int returnValue) {
		if (managedLaunches.contains(launch)) {
			update(launch, true);
			if (returnValue == 23) {
				doRestart(launch);
				return;
			}
			
			// launch failed because the associated workspace is in use
			if (returnValue == 15) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(
							PDEPlugin.getActiveWorkbenchShell(),
							PDEPlugin.getResourceString("Launcher.error.title"), //$NON-NLS-1$
							PDEPlugin.getResourceString("Launcher.error.code15")); //$NON-NLS-1$
					}
				});
				return;
			}
			
			// launch failed for reasons printed to the log.
			if (returnValue == 13) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						try {
							File log = getMostRecentLogFile(launch);
							if (log != null && MessageDialog.openQuestion(
								PDEPlugin.getActiveWorkbenchShell(),
								PDEPlugin.getResourceString("Launcher.error.title"), //$NON-NLS-1$
								PDEPlugin.getResourceString("Launcher.error.code13"))) { //$NON-NLS-1$
								if (log.exists()){
									boolean canLaunch = Program.launch(log.getAbsolutePath());
									if (!canLaunch){
										Program p = Program.findProgram (".txt"); //$NON-NLS-1$
										if (p != null) 
											p.execute (log.getAbsolutePath());
										else {
											OpenLogDialog openDialog = new OpenLogDialog(PDEPlugin.getActiveWorkbenchShell(), log);
											openDialog.create();
											openDialog.open();
										}
									}
								}
							}
						} catch (CoreException e) {
						}
					}
				});
			}
		}
	}

	private File getMostRecentLogFile(ILaunch launch) throws CoreException {
		File latest = null;
		
		String workspace = launch.getLaunchConfiguration().getAttribute(ILauncherSettings.LOCATION + "0", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (workspace.length() > 0) {
			latest = new File(workspace, ".metadata/.log"); //$NON-NLS-1$
			if (!latest.exists())
				latest = null;
		}

		String dir = launch.getAttribute(ILauncherSettings.CONFIG_LOCATION);
		if (dir != null) {
			File configDir = new File(dir);
			File[] children = configDir.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					if (!children[i].isDirectory() && children[i].getName().endsWith(".log")) { //$NON-NLS-1$
						if (latest == null || latest.lastModified() < children[i].lastModified())
							latest = children[i];
					}
				}
			}
		}

		return latest;
	}
	
}
