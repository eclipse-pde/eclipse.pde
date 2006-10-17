/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp.details;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetailsSurrogate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSDependencies
 *
 */
public class CompCSDependenciesDetails implements ICSDetails {

	private ICompCSTaskObject fDataTaskObject;
	
	private ICSDetailsSurrogate fDetails;

	private Section fDependenciesSection;	
	
	private TableViewer fDependenciesTable;	

	private Button fAddDependencyButton;
	
	private Button fRemoveDependencyButton;	
	
	private String fTaskObjectLabelName;
	
	/**
	 * 
	 */
	public CompCSDependenciesDetails(ICompCSTaskObject taskObject, 
			ICSDetailsSurrogate details) {
		
		fDataTaskObject = taskObject;
		fDetails = details;
		
		fDependenciesSection = null;
		fDependenciesTable = null;
		fAddDependencyButton = null;
		fRemoveDependencyButton = null;
		
		defineTaskObjectLabelName();
	}

	/**
	 * 
	 */
	private void defineTaskObjectLabelName() {
		if (fDataTaskObject.getType() == ICompCSConstants.TYPE_TASK) {
			fTaskObjectLabelName = PDEUIMessages.CompCSDependenciesDetails_task;
		} else {
			fTaskObjectLabelName = PDEUIMessages.CompCSDependenciesDetails_group;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR
				| ExpandableComposite.TWISTIE; 

		String description = NLS.bind(
				PDEUIMessages.CompCSDependenciesDetails_SectionDescription,
				fTaskObjectLabelName);
		fDependenciesSection = fDetails.createUISection(parent,
				PDEUIMessages.CompCSDependenciesDetails_Dependencies,
				description, style);
		// Create the container for the main section
		Composite sectionClient = fDetails.createUISectionContainer(fDependenciesSection, 2);		
		// Create the dependencies widget
		createUIDependenciesTablePart(sectionClient);
		// Bind widgets
		fDetails.getToolkit().paintBordersFor(sectionClient);
		fDependenciesSection.setClient(sectionClient);		

	}
	
	/**
	 * @param parent
	 */
	private void createUIDependenciesTablePart(Composite parent) {
		
		createUIDependenciesTable(parent);
		Composite tableButtonComposite = createUITableButtonComposite(parent);
		createUIAddDependencyButton(tableButtonComposite);
		createUIRemoveDependencyButton(tableButtonComposite);
	}
	
	/**
	 * @param parent
	 */
	private void createUIDependenciesTable(Composite parent) {
		Table table = fDetails.getToolkit().createTable(parent,
				SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 40;
		table.setLayoutData(data);
		
		fDependenciesTable = new TableViewer(table);
		// TODO: MP: MED: CompCS:  Set content provider on dependencies table
		//fWidgetDependenciesTable.setContentProvider(new SchemaAttributeContentProvider());
		fDependenciesTable.setLabelProvider(new LabelProvider());		
	}

	/**
	 * @param parent
	 * @return
	 */
	private Composite createUITableButtonComposite(Composite parent) {
		Composite tableButtonComposite = fDetails.getToolkit().createComposite(
				parent);
		GridLayout layout = new GridLayout(); 
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		tableButtonComposite.setLayout(layout);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		tableButtonComposite.setLayoutData(data);
		return tableButtonComposite;
	}
	
	/**
	 * @param parent
	 */
	private void createUIAddDependencyButton(Composite parent) {
		fAddDependencyButton = fDetails.getToolkit().createButton(parent,
				PDEUIMessages.SchemaAttributeDetails_addRestButton, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fAddDependencyButton.setLayoutData(data);
	}

	/**
	 * @param parent
	 */
	private void createUIRemoveDependencyButton(Composite parent) {
		fRemoveDependencyButton = fDetails.getToolkit()
				.createButton(parent,
						PDEUIMessages.SchemaAttributeDetails_removeRestButton,
						SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fRemoveDependencyButton.setLayoutData(data);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#hookListeners()
	 */
	public void hookListeners() {
		// TODO: MP: MED: Current: Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#updateFields()
	 */
	public void updateFields() {
		// TODO: MP: MED: Current: Auto-generated method stub
		// TODO: MP: HIGH: CompCS: Remove
		if (fDataTaskObject == null) {}
	}

}
