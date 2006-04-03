/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.runtime.logview.LogView;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;


public class LaunchListener implements ILaunchListener, IDebugEventSetListener {
    private ArrayList managedLaunches;
    // maximum log file size
    public static final long MAX_FILE_LENGTH = 1024 * 1024;
    // different ways to open the error log
    public static final int OPEN_IN_ERROR_LOG_VIEW = 0;
    public static final int OPEN_IN_SYSTEM_EDITOR = 1;

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
            if (source instanceof IProcess && event.getKind() == DebugEvent.TERMINATE) {
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
                        MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
                                PDEUIMessages.Launcher_error_title, 
                                PDEUIMessages.Launcher_error_code15); 
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
                            if (log != null && log.exists()) {
                        		MessageDialog dialog = new MessageDialog(
                            		PDEPlugin.getActiveWorkbenchShell(),
                            		PDEUIMessages.Launcher_error_title, 
                            		null, // accept the default window icon
                            		PDEUIMessages.Launcher_error_code13, 
                            		MessageDialog.ERROR,
                            		new String[] {
                            			PDEUIMessages.Launcher_error_displayInLogView,
                            			PDEUIMessages.Launcher_error_displayInSystemEditor,
                            			IDialogConstants.NO_LABEL}, 
                            		OPEN_IN_ERROR_LOG_VIEW);
                        		int dialog_value = dialog.open();
                            	if (dialog_value == OPEN_IN_ERROR_LOG_VIEW) {
                            		LogView errlog = (LogView)PDEPlugin.getActivePage()
                            				.showView("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
                            		errlog.handleImportPath(log.getAbsolutePath());
                            		errlog.sortByDateDescending();
                            	} else if (dialog_value == OPEN_IN_SYSTEM_EDITOR) {
                            		openSystemEditor(log);
                            	} 
                            }
                        } catch (CoreException e) {
                        }
                    }
                });
            }
        }
    }
    
    private void openSystemEditor(File log) {
    	boolean canLaunch = false;
    	if (log.length() <= MAX_FILE_LENGTH) {
            canLaunch = Program.launch(log.getAbsolutePath());
            if (!canLaunch) {
                Program p = Program.findProgram(".txt"); //$NON-NLS-1$
                if (p != null)
                    canLaunch = p.execute(log.getAbsolutePath());
            }
    	}
    	if (!canLaunch) {
	        OpenLogDialog dialog = new OpenLogDialog(PDEPlugin.getActiveWorkbenchShell(), log);
	        dialog.create();
	        dialog.open();
    	}
    }

    private File getMostRecentLogFile(ILaunch launch) throws CoreException {
    	ILaunchConfiguration configuration = launch.getLaunchConfiguration();
        File latest = null;
        String workspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
        if (workspace.length() > 0) {
            latest = new File(workspace, ".metadata/.log"); //$NON-NLS-1$
            if (!latest.exists())
                latest = null;
        }
        File configDir = LaunchConfigurationHelper.getConfigurationLocation(configuration);
        File[] children = configDir.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                if (!children[i].isDirectory()
                        && children[i].getName().endsWith(".log")) { //$NON-NLS-1$
                    if (latest == null
                            || latest.lastModified() < children[i].lastModified())
                        latest = children[i];
                }
            }
        }
        return latest;
    }
    
}
