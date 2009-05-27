/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

public class AddLibraryDialog extends SelectionStatusDialog {
	private String newName;
	private String[] libraries;
	private IPluginModelBase model;
	private static String init = "library.jar"; //$NON-NLS-1$
	private Text text;
	private Image libImage;
	private TableViewer libraryViewer;
	private DuplicateStatusValidator validator;

	class DuplicateStatusValidator {
		public IStatus validate(String text) {
			if (text.length() == 0)
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, PDEUIMessages.AddLibraryDialog_emptyLibraries, null);

			if (text.indexOf(' ') != -1)
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, PDEUIMessages.AddLibraryDialog_nospaces, null);

			if (libraries == null || libraries.length == 0)
				return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$

			if (!text.endsWith(".jar") && !text.endsWith("/") && !text.equals(".")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				text += "/"; //$NON-NLS-1$

			for (int i = 0; i < libraries.length; i++) {
				if (libraries[i].equals(text))
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, PDEUIMessages.BuildEditor_RuntimeInfoSection_duplicateLibrary, null);
			}
			return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}

	class TableContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			if (input instanceof IPluginModelBase) {
				return ((IPluginModelBase) input).getPluginBase().getLibraries();
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return ((IPluginLibrary) obj).getName();
		}

		public Image getColumnImage(Object obj, int index) {
			return libImage;
		}
	}

	public AddLibraryDialog(Shell shell, String[] libraries, IPluginModelBase model) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setLibraryNames(libraries);
		setPluginModel(model);
		initializeImages();
		initializeValidator();
		setStatusLineAboveButtons(true);
	}

	public void setPluginModel(IPluginModelBase model) {
		this.model = model;
	}

	private void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		libImage = provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
	}

	public void setLibraryNames(String[] libraries) {
		this.libraries = libraries;
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		container.setLayout(layout);

		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.BuildEditor_AddLibraryDialog_label);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		text = new Text(container, SWT.SINGLE | SWT.BORDER);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus(validator.validate(text.getText()));
			}
		});
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Table table = new Table(container, SWT.FULL_SELECTION | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 125;
		table.setLayoutData(gd);

		libraryViewer = new TableViewer(table);
		libraryViewer.setContentProvider(new TableContentProvider());
		libraryViewer.setLabelProvider(new TableLabelProvider());
		libraryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				ISelection sel = e.getSelection();
				IPluginLibrary obj = (IPluginLibrary) ((IStructuredSelection) sel).getFirstElement();
				text.setText(obj != null ? obj.getName() : ""); //$NON-NLS-1$
			}
		});
		libraryViewer.setInput(model);
		applyDialogFont(container);
		return container;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.BUILD_ADD_LIBRARY_DIALOG);
	}

	public int open() {
		text.setText(init);
		text.selectAll();
		return super.open();
	}

	protected void computeResult() {

	}

	public String getNewName() {
		return newName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		newName = text.getText();
		super.okPressed();
	}

	private void initializeValidator() {
		this.validator = new DuplicateStatusValidator();
	}

}
