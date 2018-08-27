/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * A dialog where the user can select arguments provided by installation (profile) containers in
 * the target.  At least one argument must be selected to enable the OK button.
 *
 */
public class ArgumentsFromContainerSelectionDialog extends TrayDialog {

	private CheckboxTreeViewer fTree;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private Map<ITargetLocation, Object[]> fAllArguments;
	private String[] fArguments;
	private ITargetDefinition fTarget;

	public ArgumentsFromContainerSelectionDialog(Shell shell, ITargetDefinition target) {
		super(shell);
		fTarget = target;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.ArgumentsFromContainerSelectionDialog_0);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		ITargetLocation[] containers = fTarget.getTargetLocations();
		boolean foundArguments = false;
		if (containers != null) {
			fAllArguments = new HashMap<>(containers.length);
			for (ITargetLocation container : containers) {
				String[] args = container.getVMArguments();
				if (args != null) {
					if (args.length > 0) {
						fAllArguments.put(container, args);
						foundArguments = true;
					} else {
						fAllArguments.put(container, new Object[] {new Status(IStatus.ERROR, PDEPlugin.getPluginId(), Messages.ArgumentsFromContainerSelectionDialog_1)});
					}
				}
			}
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.ARGS_FROM_CONTAINER_SELECTION_DIALOG);
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 10, 10);
		((GridLayout) comp.getLayout()).verticalSpacing = 10;

		Label infoLabel = SWTFactory.createLabel(comp, Messages.ArgumentsFromContainerSelectionDialog_2, 1);

		Composite treeComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH, 0, 0);

		fTree = new CheckboxTreeViewer(treeComp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 300;
		fTree.getControl().setLayoutData(gd);

		fTree.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fTree.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getChildren(Object element) {
				if (element instanceof ITargetLocation) {
					Object args = fAllArguments.get(element);
					if (args != null) {
						return (Object[]) args;
					}
				}
				return new Object[0];
			}

			@Override
			public boolean hasChildren(Object element) {
				return getChildren(element).length > 0;
			}

			@Override
			public Object[] getElements(Object element) {
				if (element instanceof Map) {
					return ((Map<?, ?>) element).keySet().toArray();
				}
				return new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

		});
		fTree.addCheckStateListener(event -> {
			updateCheckState(event.getElement());
			updateOKButton();
		});
		fTree.addDoubleClickListener(event -> {
			if (!event.getSelection().isEmpty()) {
				Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
				fTree.setChecked(selected, !fTree.getChecked(selected));
				updateCheckState(selected);
				updateOKButton();

			}
		});
		fTree.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		Composite buttonComp = SWTFactory.createComposite(treeComp, 1, 1, GridData.FILL_VERTICAL, 0, 0);

		fSelectAllButton = SWTFactory.createPushButton(buttonComp, Messages.ArgumentsFromContainerSelectionDialog_3, null);
		fSelectAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fTree.setAllChecked(true);
			// TODO These buttons don't update as the check state changes
//				fSelectAllButton.setEnabled(true);
//				fDeselectAllButton.setEnabled(false);
//				fTree.getTree().getItemCount();
			updateOKButton();
		}));

		fDeselectAllButton = SWTFactory.createPushButton(buttonComp, Messages.ArgumentsFromContainerSelectionDialog_4, null);
		fDeselectAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fTree.setAllChecked(false);
			updateOKButton();
		}));

		if (foundArguments) {
			fTree.setInput(fAllArguments);
		} else {
			fTree.getControl().setEnabled(false);
			fSelectAllButton.setEnabled(false);
			fDeselectAllButton.setEnabled(false);
			infoLabel.setText(Messages.ArgumentsFromContainerSelectionDialog_5);
		}

		return comp;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Updates the check state of the parent and any children of the element
	 */
	private void updateCheckState(Object element) {
		if (element instanceof ITargetLocation) {
			fTree.setSubtreeChecked(element, fTree.getChecked(element));
		} else {
			TreeItem[] containers = fTree.getTree().getItems();
			for (TreeItem container : containers) {
				TreeItem[] arguments = container.getItems();
				int checked = 0;
				for (TreeItem argument : arguments) {
					if (argument.getChecked()) {
						checked++;
					}
				}
				if (checked == 0) {
					container.setChecked(false);
					container.setGrayed(false);
				} else if (arguments.length > checked) {
					container.setChecked(true);
					container.setGrayed(true);
				} else {
					container.setChecked(true);
					container.setGrayed(false);
				}
			}
		}
	}

	/**
	 * Updates the enablement of the ok button based on whether one or more arguments are checked
	 */
	private void updateOKButton() {
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			boolean ok = false;
			Object[] checked = fTree.getCheckedElements();
			for (Object element : checked) {
				if (element instanceof String) {
					ok = true;
					break;
				}
			}
			okButton.setEnabled(ok);
		}

	}

	/**
	 * Returns the arguments the user selected in the dialog or <code>null</code>
	 * if the ok button has not been pressed.
	 * <p>
	 * This method may be called after the dialog has been disposed.
	 * </p>
	 * @return list of arguments or <code>null</code>
	 */
	public String[] getSelectedArguments() {
		return fArguments;
	}

	@Override
	protected void okPressed() {
		List<String> arguments = new ArrayList<>();
		Object[] checked = fTree.getCheckedElements();
		for (Object element : checked) {
			if (element instanceof String) {
				// If the argument contains a space, surround it in quotes so it is treated as a single argument
				String arg = ((String) element).trim();
				if (arg.indexOf(' ') > 0) {
					arg = "\"" + arg + "\""; //$NON-NLS-1$//$NON-NLS-2$
				}
				arguments.add(arg);
			}
		}
		fArguments = arguments.toArray(new String[arguments.size()]);
		super.okPressed();
	}

}
