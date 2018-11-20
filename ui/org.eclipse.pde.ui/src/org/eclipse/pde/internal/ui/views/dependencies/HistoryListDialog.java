/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import static org.eclipse.swt.events.SelectionListener.widgetDefaultSelectedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.StatusInfo;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class HistoryListDialog extends StatusDialog {
	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object element) {
			return fHistoryList.toArray();
		}
	}

	private List<String> fHistoryList = new ArrayList<>();

	private IStatus fHistoryStatus;

	private TableViewer fHistoryViewer;

	private Button fRemoveButton;

	private String fResult;

	public HistoryListDialog(Shell shell, String[] elements) {
		super(shell);
		setTitle(PDEUIMessages.HistoryListDialog_title);
		fHistoryList.addAll(Arrays.asList(elements));
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.HISTORY_LIST_DIALOG);
	}

	@Override
	public void create() {
		setShellStyle(getShellStyle() | SWT.RESIZE);
		super.create();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = (Composite) super.createDialogArea(parent);

		Composite inner = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(inner, SWT.NONE);
		label.setText(PDEUIMessages.HistoryListDialog_label);

		Composite container = createListArea(inner);

		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		applyDialogFont(composite);
		return composite;
	}

	private Composite createListArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout1 = new GridLayout();
		layout1.marginWidth = 0;
		layout1.marginHeight = 0;
		layout1.numColumns = 2;
		container.setLayout(layout1);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTableArea(container);
		createListButtons(container);
		return container;
	}

	private void createListButtons(Composite parent) {
		fRemoveButton = new Button(parent, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.HistoryListDialog_remove_button);
		fRemoveButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(widgetSelectedAdapter(e -> {
			IStructuredSelection selection = fHistoryViewer.getStructuredSelection();
			if (!selection.isEmpty()) {
				Object removalCandidate = selection.getFirstElement();
				fHistoryList.remove(removalCandidate);
				fHistoryViewer.remove(removalCandidate);
			}
		}));
	}

	private Control createTableArea(Composite parent) {
		Table table = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		gd.heightHint = 200;
		table.setLayoutData(gd);

		table.addSelectionListener(widgetDefaultSelectedAdapter(e -> {
			if (fHistoryStatus.isOK()) {
				okPressed();
			}
		}));

		fHistoryViewer = new TableViewer(table);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(false);
		fHistoryViewer.setLabelProvider(labelProvider);
		fHistoryViewer.getControl().addDisposeListener(e -> labelProvider.dispose());
		fHistoryViewer.setContentProvider(new ContentProvider());
		fHistoryViewer.setInput(PDECore.getDefault().getModelManager());
		fHistoryViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);

		ISelection sel;
		if (!fHistoryList.isEmpty()) {
			sel = new StructuredSelection(fHistoryList.get(0));
		} else {
			sel = new StructuredSelection();
		}
		fHistoryViewer.setSelection(sel);

		fHistoryViewer.addSelectionChangedListener(event -> {
			StatusInfo status = new StatusInfo();
			IStructuredSelection selection = fHistoryViewer.getStructuredSelection();
			List<?> selected = selection.toList();
			if (selected.size() != 1) {
				status.setError(""); //$NON-NLS-1$
				fResult = null;
			} else {
				fResult = (String) selected.get(0);
			}
			fRemoveButton.setEnabled(fHistoryList.size() > selected.size() && !selected.isEmpty());
			fHistoryStatus = status;
			updateStatus(status);
		});
		return fHistoryViewer.getControl();
	}

	public String[] getRemaining() {
		return fHistoryList.toArray(new String[fHistoryList.size()]);
	}

	public String getResult() {
		return fResult;
	}

}
