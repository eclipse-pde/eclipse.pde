/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.actions.ActionMenu;

public abstract class PDELauncherFormEditor extends MultiSourceEditor {

	protected static final int RUN_LAUNCHER_INDEX = 0;
	protected static final int DEBUG_LAUNCHER_INDEX = 1;
	protected static final int PROFILE_LAUNCHER_INDEX = 2;

	/**
	 * Stores the toolbar contributions that contain all the actions.  Entries may
	 * be null if the toolbar hasn't been created or if there are no actions for that
	 * index.  Uses {@link #RUN_LAUNCHER_INDEX}, {@link #DEBUG_LAUNCHER_INDEX} and
	 * {@link #PROFILE_LAUNCHER_INDEX}.
	 */
	ActionMenu[] fToolbarActions = new ActionMenu[3];

	LauncherAction[][] fActions = null;

	protected abstract ILauncherFormPageHelper getLauncherHelper();

	protected void contributeLaunchersToToolbar(IToolBarManager manager) {

		// this should never be null (no point in using this class if you don't provide an ILauncherFormPageHelper)
		// but we'll guard against it anyway
		if (getLauncherHelper() != null) {
			List<String> recentLaunches = PDEEditorLaunchManager.getDefault().getRecentLaunches();
			LauncherAction[][] actions = getActions();
			if (actions[RUN_LAUNCHER_INDEX].length > 0) {
				fToolbarActions[RUN_LAUNCHER_INDEX] = new ActionMenu(actions[RUN_LAUNCHER_INDEX]);
				fToolbarActions[RUN_LAUNCHER_INDEX].updateActionOrder(recentLaunches);
				manager.add(fToolbarActions[RUN_LAUNCHER_INDEX]);
			}

			if (actions[DEBUG_LAUNCHER_INDEX].length > 0) {
				fToolbarActions[DEBUG_LAUNCHER_INDEX] = new ActionMenu(actions[DEBUG_LAUNCHER_INDEX]);
				fToolbarActions[DEBUG_LAUNCHER_INDEX].updateActionOrder(recentLaunches);
				manager.add(fToolbarActions[DEBUG_LAUNCHER_INDEX]);
			}

			if (actions[PROFILE_LAUNCHER_INDEX].length > 0) {
				fToolbarActions[PROFILE_LAUNCHER_INDEX] = new ActionMenu(actions[PROFILE_LAUNCHER_INDEX]);
				fToolbarActions[PROFILE_LAUNCHER_INDEX].updateActionOrder(recentLaunches);
				manager.add(fToolbarActions[PROFILE_LAUNCHER_INDEX]);
			}
		}
	}

	private LauncherAction[][] getActions() {
		if (fActions == null) {
			fActions = new LauncherAction[3][];
			IConfigurationElement[][] elements = getLaunchers(getLauncherHelper().isOSGi());
			fActions[RUN_LAUNCHER_INDEX] = getLauncherActions(elements[RUN_LAUNCHER_INDEX], RUN_LAUNCHER_INDEX);
			fActions[DEBUG_LAUNCHER_INDEX] = getLauncherActions(elements[DEBUG_LAUNCHER_INDEX], DEBUG_LAUNCHER_INDEX);
			fActions[PROFILE_LAUNCHER_INDEX] = getLauncherActions(elements[PROFILE_LAUNCHER_INDEX], PROFILE_LAUNCHER_INDEX);
		}
		return fActions;
	}

	private LauncherAction[] getLauncherActions(IConfigurationElement[] elements, final int toolbarIndex) {
		LauncherAction[] result = new LauncherAction[elements.length];
		for (int i = 0; i < elements.length; i++) {
			LauncherAction thisAction = new LauncherAction(elements[i]) {
				@Override
				public void run() {
					doSave(null);
					String id = getConfigurationElement().getAttribute("id"); //$NON-NLS-1$
					String mode = getConfigurationElement().getAttribute("mode"); //$NON-NLS-1$
					launch(id, mode, getPreLaunchRunnable(), getLauncherHelper().getLaunchObject());
					// Have all toolbar items update their order
					PDEEditorLaunchManager.getDefault().setRecentLaunch(getConfigurationElement().getAttribute("id")); //$NON-NLS-1$
					List<String> updatedActionOrder = PDEEditorLaunchManager.getDefault().getRecentLaunches();
					for (ActionMenu action : fToolbarActions) {
						if (action != null) {
							action.updateActionOrder(updatedActionOrder);
						}
					}
				}
			};
			result[i] = thisAction;
		}
		return result;
	}

	protected Runnable getPreLaunchRunnable() {
		return () -> getLauncherHelper().preLaunch();
	}

	public void launch(String launcherID, String mode, Runnable preLaunch, Object launchObject) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.debug.ui.launchShortcuts"); //$NON-NLS-1$
		for (IConfigurationElement element : elements) {
			if (launcherID.equals(element.getAttribute("id"))) { //$NON-NLS-1$
				try {
					ILaunchShortcut shortcut = (ILaunchShortcut) element.createExecutableExtension("class"); //$NON-NLS-1$
					preLaunch.run();
					StructuredSelection selection = launchObject != null ? new StructuredSelection(launchObject) : StructuredSelection.EMPTY;
					shortcut.launch(selection, mode);
				} catch (CoreException e1) {
				}
			}
		}
	}

	protected IConfigurationElement[][] getLaunchers(boolean osgi) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.launchShortcuts"); //$NON-NLS-1$
		// validate elements
		ArrayList<IConfigurationElement> runList = new ArrayList<>();
		ArrayList<IConfigurationElement> debugList = new ArrayList<>();
		ArrayList<IConfigurationElement> profileList = new ArrayList<>();
		// limit to specific shortcuts based on project settings (if specified)
		IResource resource = getEditorInput().getAdapter(IResource.class);
		Set<String> specificIds = null;
		if (resource != null) {
			IProject project = resource.getProject();
			if (project != null) {
				String[] values = PDEProject.getLaunchShortcuts(project);
				if (values != null) {
					specificIds = new HashSet<>();
					for (String value : values) {
						specificIds.add(value);
					}
				}
			}
		}
		for (IConfigurationElement element : elements) {
			String mode = element.getAttribute("mode"); //$NON-NLS-1$
			String id = element.getAttribute("id"); //$NON-NLS-1$
			String projectSpecific = element.getAttribute("projectSpecific"); //$NON-NLS-1$
			if (mode != null && element.getAttribute("label") != null && id != null) { //$NON-NLS-1$
				boolean include = false;
				if (specificIds != null) {
					include = specificIds.contains(id);
				} else {
					include = osgi == "true".equals(element.getAttribute("osgi")) && !"true".equals(projectSpecific); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				if (include) {
					if (mode.equals(ILaunchManager.RUN_MODE))
						runList.add(element);
					else if (mode.equals(ILaunchManager.DEBUG_MODE))
						debugList.add(element);
					else if (mode.equals(ILaunchManager.PROFILE_MODE))
						profileList.add(element);
				}
			}
		}

		// sort elements based on criteria specified in bug 172703
		IConfigurationElement[] runElements = runList.toArray(new IConfigurationElement[runList.size()]);
		IConfigurationElement[] debugElements = debugList.toArray(new IConfigurationElement[debugList.size()]);
		IConfigurationElement[] profileElements = profileList.toArray(new IConfigurationElement[profileList.size()]);
		return new IConfigurationElement[][] {runElements, debugElements, profileElements};
	}

	/**
	 * Represents an action that will launch a PDE launch shortcut extension
	 */
	public static abstract class LauncherAction extends Action {
		private IConfigurationElement configElement;

		public LauncherAction(IConfigurationElement configurationElement) {
			super();
			configElement = configurationElement;
			String label = configElement.getAttribute("label"); //$NON-NLS-1$
			setText(label);
			setToolTipText(label);
			setImageDescriptor(getImageDescriptor(configurationElement.getAttribute("mode"))); //$NON-NLS-1$
		}

		public IConfigurationElement getConfigurationElement() {
			return configElement;
		}

		private ImageDescriptor getImageDescriptor(String mode) {
			if (mode == null)
				return null;
			else if (mode.equals(ILaunchManager.RUN_MODE))
				return PDEPluginImages.DESC_RUN_EXC;
			else if (mode.equals(ILaunchManager.DEBUG_MODE))
				return PDEPluginImages.DESC_DEBUG_EXC;
			else if (mode.equals(ILaunchManager.PROFILE_MODE))
				return PDEPluginImages.DESC_PROFILE_EXC;
			return null;
		}
	}
}
