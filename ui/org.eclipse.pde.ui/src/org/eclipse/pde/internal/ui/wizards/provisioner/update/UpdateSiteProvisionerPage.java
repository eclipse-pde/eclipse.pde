/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.update;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class UpdateSiteProvisionerPage extends WizardPage {

	private List fElements = new ArrayList();
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fEditButton;
	private TableViewer fViewer;

	class UpdateSiteContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List)
				return ((List) inputElement).toArray();
			return null;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	protected UpdateSiteProvisionerPage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.UpdateSiteWizardPage_title);
		setDescription(PDEUIMessages.UpdateSiteWizardPage_description);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite client = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2;
		layout.numColumns = 2;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(client, SWT.None);
		label.setText(PDEUIMessages.UpdateSiteWizardPage_label);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		fViewer = new TableViewer(client, SWT.BORDER);
		fViewer.setContentProvider(new UpdateSiteContentProvider());
		fViewer.setLabelProvider(new LabelProvider() {

			public Image getImage(Object element) {
				PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
				return provider.get(PDEPluginImages.DESC_SITE_OBJ);
			}

			public String getText(Object element) {
				IUpdateSiteProvisionerEntry entry = (IUpdateSiteProvisionerEntry) element;
				return entry.getSiteLocation();
			}

		});
		fViewer.setInput(fElements);
		gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 3;
		fViewer.getControl().setLayoutData(gd);

		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}

		});

		fViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					if (fViewer.getSelection() != null) {
						handleRemove();
					}
				}
			}
		});

		PlatformUI.getWorkbench().getHelpSystem().setHelp(client, IHelpContextIds.UPDATE_SITE_PROVISIONING_PAGE);
		createButtons(client);
		setControl(client);
	}

	protected void createButtons(Composite parent) {
		fAddButton = new Button(parent, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.UpdateSiteWizardPage_add);
		fAddButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fEditButton = new Button(parent, SWT.PUSH);
		fEditButton.setText(PDEUIMessages.UpdateSiteWizardPage_edit);
		fEditButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fEditButton);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IUpdateSiteProvisionerEntry entry = (IUpdateSiteProvisionerEntry) ((IStructuredSelection) fViewer.getSelection()).getFirstElement();
				handleEdit(entry);
			}
		});

		fRemoveButton = new Button(parent, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.UpdateSiteWizardPage_remove);
		fRemoveButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		updateButtons();
	}

	protected void handleRemove() {
		Object[] elements = ((IStructuredSelection) fViewer.getSelection()).toArray();
		for (int i = 0; i < elements.length; i++)
			fElements.remove(elements[i]);

		fViewer.refresh();
		updateButtons();
		fViewer.getTable().select(fElements.size() - 1);
		setPageComplete(!fElements.isEmpty());
	}

	protected void handleAdd() {
		UpdateSiteProvisionerDialog dialog = new UpdateSiteProvisionerDialog(getShell(), null, null, PDEUIMessages.UpdateSiteProvisionerDialog_addTitle);
		int status = dialog.open();
		if (status == Window.OK) {
			fElements.add(dialog.getEntry());
			fViewer.refresh();
			fViewer.getTable().select(fElements.indexOf(dialog.getEntry()));
			setPageComplete(true);
			updateButtons();
		}
	}

	protected void handleEdit(IUpdateSiteProvisionerEntry entry) {
		UpdateSiteProvisionerDialog dialog = new UpdateSiteProvisionerDialog(getShell(), entry.getInstallLocation(), entry.getSiteLocation(), PDEUIMessages.UpdateSiteProvisionerDialog_editTitle);
		int status = dialog.open();
		if (status == Window.OK) {
			fElements.remove(entry);
			fElements.add(dialog.getEntry());
			fViewer.refresh();
			fViewer.getTable().select(fElements.indexOf(dialog.getEntry()));
			setPageComplete(true);
		}
		updateButtons();
	}

	protected void updateButtons() {
		int num = fViewer.getTable().getSelectionCount();
		fRemoveButton.setEnabled(num > 0);
		fEditButton.setEnabled(num == 1);
	}

	public IUpdateSiteProvisionerEntry[] getEntries() {
		return (IUpdateSiteProvisionerEntry[]) fElements.toArray(new UpdateSiteProvisionerEntry[fElements.size()]);
	}

}
