/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.StatusLineContributionItem;

/**
 * Contributes a status control displaying information on the current target platform,
 * similar to HeapStatus.
 * 
 * @since 4.4 Luna
 */
public class TargetStatus {

	private static final String TARGET_STATUS_ID = "targetStatus"; //$NON-NLS-1$
	private static StatusLineContributionItem targetStatus;

	/**
	 * The status line contribution that displays the current target platform. Uses the 
	 * text editor's contribution item to keep the same look and feel.
	 */
	private static class TargetStatusLineContributionItem extends StatusLineContributionItem {

		public TargetStatusLineContributionItem() {
			super(TARGET_STATUS_ID, true, 22);
			PDEPlugin.getDefault().getLabelProvider().connect(this); // Needed to avoid disposing the image early
			setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION));
			update();
			setActionHandler(new Action() {
				@Override
				public void run() {
					PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(PDEPlugin.getActiveWorkbenchShell(), TargetPlatformPreferencePage.PAGE_ID, null, null);
					dialog.open();
				}
			});
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PDECore.PLUGIN_ID);
			node.addPreferenceChangeListener(prefListener);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.ContributionItem#dispose()
		 */
		@Override
		public void dispose() {
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
			super.dispose();
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PDECore.PLUGIN_ID);
			node.removePreferenceChangeListener(prefListener);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.ContributionItem#update()
		 */
		@Override
		public void update() {
			String result = Messages.TargetStatus_TargetStatusDefaultString;
			try {
				ITargetHandle handle = TargetPlatformService.getDefault().getWorkspaceTargetHandle();
				if (handle != null) {
					ITargetDefinition target = handle.getTargetDefinition();
					String name = target.getName();
					if (name != null && name.length() > 0) {
						result = name;
					}
				} else {
					result = Messages.TargetStatus_NoActiveTargetPlatformStatus;
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
			final String newValue = result;
			UIJob job = new UIJob("") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					targetStatus.setText(newValue);
					setToolTipText(newValue);
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}

		private final IPreferenceChangeListener prefListener = new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				if (ICoreConstants.WORKSPACE_TARGET_HANDLE.equals(event.getKey())) {
					update();
				}

			}
		};
	}

	/**
	 * @return the existing target status contribution item or a new instance if one hasn't been created
	 */
	private static StatusLineContributionItem getContributionItem() {
		if (targetStatus == null) {
			targetStatus = new TargetStatusLineContributionItem();
		}
		return targetStatus;
	}

	@SuppressWarnings("restriction")
	// see https://bugs.eclipse.org/378395
	private static IStatusLineManager getStatusLineManager(IWorkbenchWindow window) {
		// We are not in a view or editor so this is the only practical way of getting the status line manager at this time -  see https://bugs.eclipse.org/378395
		return (window instanceof org.eclipse.ui.internal.WorkbenchWindow) ? ((org.eclipse.ui.internal.WorkbenchWindow) window).getStatusLineManager() : null;
	}

	/**
	 * Adds the target status contribution to the status line manager if the value of
	 * preference {@link IPreferenceConstants#SHOW_TARGET_STATUS} is true.  Will not remove
	 * an existing status contribution if the preference is false, to remove use
	 * {@link #refreshTargetStatus()}.
	 * <p>
	 * Does not have to be called from a UI thread.
	 * </p>
	 */
	public static void initializeTargetStatus() {
		PDEPreferencesManager prefs = PDEPlugin.getDefault().getPreferenceManager();
		boolean showStatus = prefs.getBoolean(IPreferenceConstants.SHOW_TARGET_STATUS);

		if (showStatus) {
			UIJob updateStatus = new UIJob("Refresh PDE Target Status") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
					for (int i = 0; i < windows.length; i++) {
						IStatusLineManager slManager = getStatusLineManager(windows[i]);
						if (slManager != null) {
							slManager.appendToGroup(StatusLineManager.BEGIN_GROUP, getContributionItem());
							slManager.update(false);
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
			updateStatus.setSystem(true);
			updateStatus.setPriority(Job.DECORATE);
			updateStatus.schedule();
		}
	}

	/**
	 * Adds or removes the target status contribution from the status line manager depending on the
	 * value of preference {@link IPreferenceConstants#SHOW_TARGET_STATUS}.
	 * <p>
	 * Must be called from the UI Thread.
	 * </p>
	 */
	public static void refreshTargetStatus() {
		PDEPreferencesManager prefs = PDEPlugin.getDefault().getPreferenceManager();
		boolean showStatus = prefs.getBoolean(IPreferenceConstants.SHOW_TARGET_STATUS);

		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IStatusLineManager manager = getStatusLineManager(windows[i]);
			if (manager != null) {
				if (showStatus) {
					manager.appendToGroup(StatusLineManager.BEGIN_GROUP, getContributionItem());
				} else {
					manager.remove(TARGET_STATUS_ID);
				}
				manager.update(false);
				break;
			}
		}
	}
}
