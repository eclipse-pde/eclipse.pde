/*******************************************************************************
 * Copyright (c) 2009, 2011 eXXcellent solutions gmbh, EclipseSource Corporation,
 * IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *     EclipseSource - initial API and implementation, ongoing enhancements
 *     IBM Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.ui.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsProcessor;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.MainTab;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

public class LauncherUtilsStatusHandler implements IStatusHandler {

	public Object handleStatus(IStatus status, Object source) throws CoreException {
		int code = status.getCode();
		switch (code) {
			case LauncherUtils.WORKSPACE_LOCKED :
				Object[] args = (Object[]) source;
				handleWorkspaceLocked((String) args[0], (ILaunchConfiguration) args[1], (String) args[2]);
				break;

			case LauncherUtils.CLEAR_LOG :
				return clearLog();

			case LauncherUtils.DELETE_WORKSPACE :
				return deleteWorkspace((String) source);

			case LauncherUtils.GENERATE_CONFIG_INI :
				return generateConfigIni();

			case LauncherUtils.ORGANIZE_MANIFESTS :
				Object[] args2 = (Object[]) source;
				organizeManifests((ArrayList) args2[0], (IProgressMonitor) args2[1], (Properties) args2[2]);
				break;

			case LauncherUtils.SELECT_WORKSPACE_FIELD :
				ILaunchConfigurationDialog dialog = (ILaunchConfigurationDialog) source;
				selectWorkspaceField(dialog);
				break;
		}
		return null;
	}

	private void selectWorkspaceField(ILaunchConfigurationDialog dialog) {
		ILaunchConfigurationTab[] tabs = dialog.getTabs();
		for (int i = 0; i < tabs.length; i++) {
			ILaunchConfigurationTab tab = tabs[i];
			if (tab instanceof MainTab) {
				MainTab mainTab = (MainTab) tab;
				dialog.setActiveTab(mainTab);
				mainTab.applyData(IPDELauncherConstants.LOCATION);
			}
		}
	}

	private Boolean generateConfigIni() {
		String message = PDEUIMessages.LauncherUtils_generateConfigIni;
		return Boolean.valueOf(generateDialog(message).intValue() == 0);
	}

	private Integer deleteWorkspace(String path) {
		return generateDialog(NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_confirmDeleteWorkspace, path));
	}

	private Integer clearLog() {
		return generateDialog(PDEUIMessages.LauncherUtils_clearLogFile);
	}

	private void handleWorkspaceLocked(String workspace, ILaunchConfiguration launchConfig, String mode) {
		String message = NLS.bind(PDEMessages.LauncherUtils_cannotLaunchApplication, workspace);
		generateErrorDialog(PDEUIMessages.LauncherUtils_workspaceLocked, message, launchConfig, mode);
	}

	private void organizeManifests(final ArrayList projects, final IProgressMonitor monitor, final Properties lastRun) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				OrganizeManifestsProcessor processor = new OrganizeManifestsProcessor(projects);
				initializeProcessor(processor);
				try {
					Change change = processor.createChange(monitor);
					change.perform(monitor);
					// update table for each project with current time stamp
					Properties table = lastRun;
					String ts = Long.toString(System.currentTimeMillis());
					Iterator it = projects.iterator();
					while (it.hasNext())
						table.put(((IProject) it.next()).getName(), ts);
				} catch (OperationCanceledException e) {
				} catch (CoreException e) {
				}
			}
		});
	}

	public static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	public final static Shell getActiveShell() {
		IWorkbenchWindow window = PDEPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PDEPlugin.getDefault().getWorkbench().getWorkbenchWindows();
			if (windows.length > 0)
				return windows[0].getShell();
		} else
			return window.getShell();
		return getDisplay().getActiveShell();
	}

	private static void initializeProcessor(OrganizeManifestsProcessor processor) {
		processor.setAddMissing(false);
		processor.setRemoveUnresolved(false);
		processor.setModifyDep(false);
		processor.setRemoveLazy(false);
		processor.setRemoveUselessFiles(false);
		processor.setAddDependencies(true);
		processor.setCalculateUses(false);
		processor.setMarkInternal(false);
		processor.setPrefixIconNL(false);
		processor.setUnusedDependencies(false);
		processor.setUnusedKeys(false);
	}

	/**
	 * Creates a message dialog using a syncExec in case we are launching in the background.
	 * Dialog will be a question dialog with Yes, No and Cancel buttons.
	 * @param message Message to use in the dialog
	 * @return int representing the button clicked (-1 or 2 for cancel, 0 for yes, 1 for no).
	 */
	private static Integer generateDialog(final String message) {
		final int[] result = new int[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEUIMessages.LauncherUtils_title;
				MessageDialog dialog = new MessageDialog(getActiveShell(), title, null, message, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
				result[0] = dialog.open();
			}
		});
		return new Integer(result[0]);
	}

	private static void generateErrorDialog(final String title, final String message, final ILaunchConfiguration launchConfig, final String mode) {
		getDisplay().syncExec(new Runnable() {
			public void run() {
				Shell parentShell = getActiveShell();
				MessageDialog dialog = new MessageDialog(parentShell, title, null, message, MessageDialog.ERROR, new String[] {PDEUIMessages.LauncherUtils_edit, IDialogConstants.OK_LABEL}, 1);
				int res = dialog.open();
				if (res == 0) {
					IStructuredSelection selection = new StructuredSelection(launchConfig);
					ILaunchGroup group = DebugUITools.getLaunchGroup(launchConfig, mode);
					String groupIdentifier = group == null ? IDebugUIConstants.ID_RUN_LAUNCH_GROUP : group.getIdentifier();
					IStatus status = new Status(IStatus.OK, PDELaunchingPlugin.getPluginId(), LauncherUtils.SELECT_WORKSPACE_FIELD, "", null); //$NON-NLS-1$
					DebugUITools.openLaunchConfigurationDialogOnGroup(parentShell, selection, groupIdentifier, status);
				}
			}
		});
	}

}
