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

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.MultiStatus;
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
import org.eclipse.swt.widgets.Text;
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
				return (Object[]) fInput.get(parentElement);
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

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (fShowLink) {
			createLink(parent);
		}
		createButton(parent, IDialogConstants.OK_ID, PDEUIMessages.PluginStatusDialog_continueButtonLabel, true);
		if (fShowCancelButton) {
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
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
		GridData gridData = new GridData(GridData.FILL_BOTH);

		Entry<?, ?> onlyEntry = fInput.size() == 1 ? fInput.entrySet().iterator().next() : null;
		if (onlyEntry != null && onlyEntry.getKey() instanceof String errorMessage && errorMessage.contains("\n") //$NON-NLS-1$
				&& onlyEntry.getValue() == null) {
			Text message = new Text(container, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
			message.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			message.setText(errorMessage);
			message.setLayoutData(gridData);
		} else {
			treeViewer = new TreeViewer(container);
			treeViewer.setContentProvider(new ContentProvider());
			treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
			treeViewer.setComparator(new ViewerComparator());
			treeViewer.setInput(fInput);
			treeViewer.getControl().setLayoutData(gridData);
		}
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
		fInput = input;
		treeViewer.setInput(input);
		treeViewer.refresh();
	}

}
