/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.StatusDialog;
import org.eclipse.pde.internal.ui.parts.StatusInfo;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.help.WorkbenchHelp;

public class HistoryListDialog extends StatusDialog {
	class ContentProvider extends DefaultContentProvider implements
			IStructuredContentProvider {
		public Object[] getElements(Object element) {
			return fHistoryList.toArray();
		}
	}

	private List fHistoryList = new ArrayList();

	private IStatus fHistoryStatus;

	private TableViewer fHistoryViewer;

	private Button fRemoveButton;

	private String fResult;

	public HistoryListDialog(Shell shell, String[] elements) {
		super(shell);
		setTitle(PDEPlugin.getResourceString("HistoryListDialog.title")); //$NON-NLS-1$
		fHistoryList.addAll(Arrays.asList(elements));
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IHelpContextIds.HISTORY_LIST_DIALOG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		setShellStyle(getShellStyle() | SWT.RESIZE);
		super.create();
	}

	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
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
		label.setText(PDEPlugin.getResourceString("HistoryListDialog.label")); //$NON-NLS-1$

		Composite container = createListArea(inner);

		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		applyDialogFont(composite);
		return composite;
	}

	/**
	 * @param parent
	 * @return
	 */
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

	/**
	 * @param container
	 */
	private void createListButtons(Composite parent) {
		fRemoveButton = new Button(parent, SWT.PUSH);
		fRemoveButton.setText(PDEPlugin
				.getResourceString("HistoryListDialog.remove.button"));
		fRemoveButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false,
				false));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = fHistoryViewer.getSelection();
				if (!selection.isEmpty()
						&& selection instanceof IStructuredSelection) {
					Object removalCandiate = ((IStructuredSelection) selection)
							.getFirstElement();
					fHistoryList.remove(removalCandiate);
					fHistoryViewer.remove(removalCandiate);
				}
			}
		});
	}

	/**
	 * @param parent
	 * @return
	 */
	private Control createTableArea(Composite parent) {
		Table table = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		gd.heightHint = 200;
		table.setLayoutData(gd);

		table.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				if (fHistoryStatus.isOK()) {
					okPressed();
				}
			}
		});

		fHistoryViewer = new TableViewer(table);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(
				false);
		fHistoryViewer.setLabelProvider(labelProvider);
		fHistoryViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				labelProvider.dispose();
			}
		});
		fHistoryViewer.setContentProvider(new ContentProvider());
		fHistoryViewer.setInput(PDECore.getDefault().getExternalModelManager());
		fHistoryViewer.setSorter(ListUtil.PLUGIN_SORTER);

		ISelection sel;
		if (fHistoryList.size() > 0) {
			sel = new StructuredSelection(fHistoryList.get(0));
		} else {
			sel = new StructuredSelection();
		}
		fHistoryViewer.setSelection(sel);

		fHistoryViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
					 */
					public void selectionChanged(SelectionChangedEvent event) {
						StatusInfo status = new StatusInfo();
						ISelection selection = fHistoryViewer.getSelection();
						if (selection instanceof IStructuredSelection) {
							List selected = ((IStructuredSelection) selection)
									.toList();
							if (selected.size() != 1) {
								status.setError(""); //$NON-NLS-1$
								fResult = null;
							} else {
								fResult = (String) selected.get(0);
							}
							fRemoveButton
									.setEnabled(fHistoryList.size() > selected
											.size()
											&& selected.size() != 0);
							fHistoryStatus = status;
							updateStatus(status);
						}
					}
				});
		return fHistoryViewer.getControl();
	}

	public String[] getRemaining() {
		return (String[]) fHistoryList.toArray(new String[fHistoryList.size()]);
	}

	public String getResult() {
		return fResult;
	}

}
