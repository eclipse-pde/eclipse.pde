/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class IndexPage extends WizardPage {
	private String indexId;
	private Text text;
	private TableViewer tableViewer;
	private String [] existingIndices;
	
	class ExistingProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return existingIndices;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	public IndexPage(String [] existingIndices) {
		super("index");
		setTitle("Script index");
		setDescription("Enter a unique id for the script index. The index will be processed by index handled during execution of the script.");
		this.existingIndices = existingIndices;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		Label label = new Label(container, SWT.NULL);
		label.setText("&Index identifier:");
		text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label = new Label(container, SWT.NULL);
		label.setText("&Existing indices:");
		tableViewer = new TableViewer(container, SWT.BORDER);
		tableViewer.setContentProvider(new ExistingProvider());
		tableViewer.setInput(this);
		tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				Object obj = sel.getFirstElement();
				if (obj!=null)
					text.setText(obj.toString());
			}
		});
		setPageComplete(false);
		setControl(container);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			text.setFocus();
	}
	
	private void updateStatus() {
		String id = text.getText();
		String errorMessage=null;
		if (id.length()==0) {
			errorMessage = "Index id cannot be empty.";
		}
		else {
			boolean exists=false;
			for (int i=0; i<existingIndices.length; i++) {
				if (id.equals(existingIndices[i])) {
					exists=true;
					break;
				}
			}
			if (exists)
				errorMessage="Index id already exists.";
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage==null);
		if (errorMessage==null)
			this.indexId = id;
	}
	
	public String getIndexId() {
		return indexId;
	}
}