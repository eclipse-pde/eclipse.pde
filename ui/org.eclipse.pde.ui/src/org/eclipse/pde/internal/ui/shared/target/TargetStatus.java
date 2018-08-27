/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferencePage;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
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

		TargetDefinition fRunningHost;

		public TargetStatusLineContributionItem() {
			super(TARGET_STATUS_ID, true, 22);
			PDEPlugin.getDefault().getLabelProvider().connect(this); // Needed to avoid disposing the image early
			setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION));
			fRunningHost = (TargetDefinition) TargetPlatformService.getDefault().newDefaultTarget();
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

		@Override
		public void dispose() {
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
			super.dispose();
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PDECore.PLUGIN_ID);
			node.removePreferenceChangeListener(prefListener);
		}

		@Override
		public void update() {
			int flag = 0;
			String result = Messages.TargetStatus_TargetStatusDefaultString;
			String statusMessage = null;
			final Image newImage;
			try {
				ITargetHandle handle = TargetPlatformService.getDefault().getWorkspaceTargetHandle();
				if (handle != null) {
					ITargetDefinition target = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
					String name = target.getName();
					if (name != null && name.length() > 0) {
						result = name;
					}
					if (target.isResolved()) {
						IStatus status = target.getStatus();
						if (status.getSeverity() == IStatus.WARNING) {
							flag = SharedLabelProvider.F_WARNING;
							statusMessage = getStatusMessage(status).toString();
						} else if (status.getSeverity() == IStatus.ERROR) {
							flag = SharedLabelProvider.F_ERROR;
							statusMessage = getStatusMessage(status).toString();
						}
					} else {
						flag = SharedLabelProvider.F_WARNING;
						result = NLS.bind(Messages.TargetStatus_UnresolvedTarget, name);
						statusMessage = NLS.bind('\n' + Messages.TargetStatus_UnresolvedTargetStatus, name);
					}
					if (fRunningHost != null && fRunningHost.isContentEquivalent(target)) {
						newImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_BRANDING, flag);
					} else {
						newImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION, flag);
					}
				} else {
					result = Messages.TargetStatus_NoActiveTargetPlatformStatus;
					newImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION);
				}

				final String newValue = result;
				final String newTooltip = statusMessage == null ? newValue : newValue + statusMessage;
				UIJob job = new UIJob("") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						targetStatus.setText(newValue);
						setImage(newImage);
						setToolTipText(newTooltip);
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();

			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}

		private final IPreferenceChangeListener prefListener = event -> {
			if (ICoreConstants.WORKSPACE_TARGET_HANDLE.equals(event.getKey())) {
				update();
			}

		};

		private StringBuilder getStatusMessage(IStatus status) {
			StringBuilder result = new StringBuilder();
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				for (IStatus child : children) {
					result.append(getStatusMessage(child));
				}
			} else {
				result.append('\n').append(status.getMessage());
			}
			return result;
		}
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
					for (IWorkbenchWindow window : windows) {
						IStatusLineManager slManager = getStatusLineManager(window);
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
		for (IWorkbenchWindow window : windows) {
			IStatusLineManager manager = getStatusLineManager(window);
			if (manager != null) {
				if (showStatus) {
					manager.remove(TARGET_STATUS_ID);
					manager.appendToGroup(StatusLineManager.BEGIN_GROUP, getContributionItem());
				} else {
					manager.remove(TARGET_STATUS_ID);
				}
				manager.update(false);
				break;
			}
		}
	}

	/**
	 * Updates the content of the status line based on the current target platform
	 * if the status line item is visible
	 */
	public static void refreshTargetStatusContent() {
		PDEPreferencesManager prefs = PDEPlugin.getDefault().getPreferenceManager();
		boolean showStatus = prefs.getBoolean(IPreferenceConstants.SHOW_TARGET_STATUS);
		if (showStatus) {
			getContributionItem().update();
		}
	}
}
