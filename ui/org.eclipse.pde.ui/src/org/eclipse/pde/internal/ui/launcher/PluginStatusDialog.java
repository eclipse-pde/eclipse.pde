/*******************************************************************************
 *  Copyright (c) 2005, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Dinesh Palanisamy (ETAS GmbH) - Issue 305
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog that opens when plug-in validation fails during launching. Displays a
 * list of problems discovered. Allows the user to continue the launch or cancel
 * if @link {@link #showCancelButton(boolean)} is set to true.
 */
public class PluginStatusDialog extends TrayDialog {

	class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (fInput.containsKey(parentElement)) {
				Object value = fInput.get(parentElement);
				if (value instanceof Object[] children) {
					return children;
				}
			}
			if (parentElement instanceof MultiStatus) {
				return ((MultiStatus) parentElement).getChildren();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (fInput.containsKey(element) && element instanceof BundleDescription) {
				return true;
			}
			if (element instanceof MultiStatus) {
				return ((MultiStatus) element).getChildren().length > 0;
			}
			return false;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((Map<?, ?>) inputElement).keySet().toArray();
		}

	}

	private boolean fShowCancelButton;
	private boolean fShowLink;
	private Map<?, ?> fInput;
	private TreeViewer treeViewer;
	private ILaunchConfiguration fLaunchConfiguration;
	private String launchMode;
	private PluginStatusDialogActions fActions;

	/**
	 * Callbacks supplied by the owner of the dialog (a launch-configuration
	 * block) so the dialog can offer in-place fixes for the reported problems.
	 * The dialog is also shown in contexts without a plug-in tree to modify
	 * (e.g. at launch time); those simply do not set any actions, so the
	 * buttons are not created.
	 */
	public interface PluginStatusDialogActions {
		/** Adds the required plug-ins/features to the owning block's selection. */
		void selectRequired();

		/** Removes the unresolved plug-ins from the owning block's selection. */
		void removeUnresolved();

		/** Whether the {@link #removeUnresolved()} action is offered. */
		boolean supportsRemoveUnresolved();

		/** Label for the "select required" button (plug-ins vs. features). */
		String getSelectRequiredLabel();

		/**
		 * Re-validates the owning block's current selection and returns the
		 * operation, or {@code null} if validation failed. Used to refresh the
		 * dialog after an action.
		 */
		LaunchValidationOperation validate();
	}

	public void setActions(PluginStatusDialogActions actions) {
		fActions = actions;
	}

	public PluginStatusDialog(Shell parentShell, int style) {
		super(parentShell);
		setShellStyle(style);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public PluginStatusDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void showCancelButton(boolean showCancel) {
		fShowCancelButton = showCancel;
	}

	public void showLink(boolean showLink) {
		fShowLink = showLink;
	}

	public void setInput(LaunchValidationOperation operation) {
		fInput = operation.getInput();
		fLaunchConfiguration = operation.fLaunchConfiguration;
		launchMode = operation.fLaunchMode;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSectionName());
		if (section == null) {
			section = settings.addNewSection(getDialogSectionName());
		}
		return section;
	}

	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTSIZE;
	}

	private static final int SELECT_REQUIRED_ID = IDialogConstants.CLIENT_ID + 1;
	private static final int REMOVE_UNRESOLVED_ID = IDialogConstants.CLIENT_ID + 2;

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (fShowLink) {
			createLink(parent);
		}
		if (fActions != null) {
			createButton(parent, SELECT_REQUIRED_ID, fActions.getSelectRequiredLabel(), false);
			if (fActions.supportsRemoveUnresolved()) {
				createButton(parent, REMOVE_UNRESOLVED_ID, PDEUIMessages.AdvancedLauncherTab_removeUnresolved, false);
			}
		}
		createButton(parent, IDialogConstants.OK_ID, PDEUIMessages.PluginStatusDialog_continueButtonLabel, true);
		if (fShowCancelButton) {
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == SELECT_REQUIRED_ID) {
			runAction(PluginStatusDialogActions::selectRequired);
		} else if (buttonId == REMOVE_UNRESOLVED_ID) {
			runAction(PluginStatusDialogActions::removeUnresolved);
		} else {
			super.buttonPressed(buttonId);
		}
	}

	private void runAction(Consumer<PluginStatusDialogActions> action) {
		if (fActions == null) {
			return;
		}
		action.accept(fActions);
		LaunchValidationOperation operation = fActions.validate();
		if (operation != null) {
			refresh(operation.getInput());
		}
	}

	private void createLink(Composite parent) {
		GridLayout layout = (GridLayout) parent.getLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		Link link = new Link(parent, SWT.NONE);
		link.setText(PDEUIMessages.PluginStatusDialog_validationLink);
		if (fLaunchConfiguration != null) {
			link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				// Closing the validation dialog to avoid cyclic dependency
				setReturnCode(CANCEL);
				close();
				ILaunchGroup launchGroup = DebugUITools.getLaunchGroup(fLaunchConfiguration, launchMode);
				String groupIdentifier = launchGroup != null //
						? launchGroup.getIdentifier()
						: IDebugUIConstants.ID_RUN_LAUNCH_GROUP;
				DebugUITools.openLaunchConfigurationDialog(Display.getCurrent().getActiveShell(), fLaunchConfiguration,
						groupIdentifier, null);
			}));
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.PLUGIN_STATUS_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 300;
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.PluginStatusDialog_label);

		treeViewer = new TreeViewer(container);
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		treeViewer.setComparator(new ViewerComparator());
		treeViewer.setInput(fInput);
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

		getShell().setText(PDEUIMessages.PluginStatusDialog_pluginValidation);
		Dialog.applyDialogFont(container);
		return container;
	}

	@Override
	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	protected String getDialogSectionName() {
		return PDEPlugin.getPluginId() + ".PLUGIN_STATUS_DIALOG"; //$NON-NLS-1$
	}

	public void refresh(Map<?, ?> input) {
		if (input.isEmpty()) {
			// Keep the field and the viewer input consistent: show a single
			// "no problems" node once all reported issues have been fixed.
			Map<String, IStatus> noProblems = new HashMap<>(1);
			noProblems.put(PDEUIMessages.AbstractLauncherToolbar_noProblems, Status.OK_STATUS);
			input = noProblems;
		}
		fInput = input;
		treeViewer.setInput(input);
		treeViewer.refresh();
	}

}
