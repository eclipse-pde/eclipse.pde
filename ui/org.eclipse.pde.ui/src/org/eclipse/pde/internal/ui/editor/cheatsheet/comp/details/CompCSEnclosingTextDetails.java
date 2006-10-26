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

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetailsSurrogate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSEnclosingTextDetails
 *
 */
public class CompCSEnclosingTextDetails implements ICSDetails {

	private ICompCSTaskObject fDataTaskObject;
	
	private ICSDetailsSurrogate fDetails;

	private Section fEnclosingTextSection;		
	
	private Text fIntroductionText;
	
	private Text fConclusionText;
	
	private CTabFolder fTabFolder;
	
	private final static int F_INTRODUCTION_TAB = 0;

	private final static int F_CONCLUSION_TAB = 1;
	
	private Composite fNotebookComposite;
	
	private StackLayout fNotebookLayout;
	
	private Composite fIntroductionComposite;
	
	private Composite fCompositeConclusion;		
	
	private String fTaskObjectLabelName;	
	
	/**
	 * 
	 */
	public CompCSEnclosingTextDetails(ICompCSTaskObject taskObject, 
			ICSDetailsSurrogate details) {

		fDataTaskObject = taskObject;
		fDetails = details;		
		
		fEnclosingTextSection = null;
		fIntroductionText = null;
		fConclusionText = null;
		fTabFolder = null;
		fNotebookComposite = null;
		fNotebookLayout = null;
		fIntroductionComposite = null;
		fCompositeConclusion = null;
		
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
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		String description = NLS.bind(
				PDEUIMessages.CompCSEnclosingTextDetails_SectionDescription,
				fTaskObjectLabelName);		
		fEnclosingTextSection = fDetails.createUISection(parent,
				PDEUIMessages.CompCSEnclosingTextDetails_EnclosingText,
				description, style);
		// Create the container for the main section
		Composite sectionClient = fDetails.createUISectionContainer(
				fEnclosingTextSection, 1);		
		// Create the tab folder
		createUITabFolder(sectionClient);
		// Create the introduction folder tab
		createUIIntroductionTab();
		// Create the conclusion folder tab
		createUIConclusionTab();
		// Create the notebook composite
		createUINotebookComposite(sectionClient);
		// Create the introduction text
		createUIIntroductionText();
		// Create the conclusion text
		createUIConclusionText();	
		// Bind widgets
		fDetails.getToolkit().paintBordersFor(sectionClient);
		fEnclosingTextSection.setClient(sectionClient);		
		
	}

	/**
	 * @param parent
	 */
	private void createUITabFolder(Composite parent) {
		fTabFolder = new CTabFolder(parent, SWT.FLAT | SWT.TOP);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.heightHint = 2;
		fTabFolder.setLayoutData(data);
		
		fDetails.getToolkit().adapt(fTabFolder, true, true);
		
		FormColors colors = fDetails.getToolkit().getColors();
		colors.initializeSectionToolBarColors();
		Color selectedColor1 = colors.getColor(FormColors.TB_BG);
		Color selectedColor2 = colors.getColor(FormColors.TB_GBG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor1,
				selectedColor2, colors.getBackground() },
				new int[] { 50, 100 }, true);		
	}	

	/**
	 * 
	 */
	private void createUIIntroductionTab() {
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(PDEUIMessages.CompCSEnclosingTextDetails_Introduction);
		item.setImage(PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_CSINTRO_OBJ));	
	}

	/**
	 * 
	 */
	private void createUIConclusionTab() {
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(PDEUIMessages.CompCSEnclosingTextDetails_Conclusion);
		// TODO: MP: LOW: CompCS:  Update image
		item.setImage(PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_CSINTRO_OBJ));	
	}		
	
	/**
	 * @param parent
	 */
	private void createUINotebookComposite(Composite parent) {
		fNotebookComposite = fDetails.getToolkit().createComposite(parent);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fNotebookComposite.setLayoutData(data);
		fNotebookLayout = new StackLayout();
		fNotebookComposite.setLayout(fNotebookLayout);		
	}
	
	/**
	 * 
	 */
	private void createUIIntroductionText() {
		GridData data = null;
		int columns = 1;
		// Create composite
		fIntroductionComposite = createUIContainer(fNotebookComposite, columns);
		// Create label
		String description = NLS.bind(
				PDEUIMessages.CompCSEnclosingTextDetails_IntroductionDescription,
				fTaskObjectLabelName);			
		final Label label = fDetails.getToolkit().createLabel(
				fIntroductionComposite, description, SWT.WRAP);
		data = new GridData();
		data.horizontalSpan = columns;
		label.setLayoutData(data);		
		// Create text
		int style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
		fIntroductionText = fDetails.getToolkit().createText(
				fIntroductionComposite, "", style); //$NON-NLS-1$
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 60;
		fIntroductionText.setLayoutData(data);

	}
	
	/**
	 * 
	 */
	private void createUIConclusionText() {
		GridData data = null;
		int columns = 1;
		// Create composite
		fCompositeConclusion = createUIContainer(fNotebookComposite, columns);
		// Create label
		String description = NLS.bind(
				PDEUIMessages.CompCSEnclosingTextDetails_ConclusionDescription,
				fTaskObjectLabelName);		
		final Label label = fDetails.getToolkit().createLabel(
				fCompositeConclusion, description, SWT.WRAP);
		data = new GridData();
		data.horizontalSpan = columns;
		label.setLayoutData(data);		
		// Create text
		int style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;		
		fConclusionText = fDetails.getToolkit().createText(
				fCompositeConclusion, "", style); //$NON-NLS-1$
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 60;
		fConclusionText.setLayoutData(data);
	}	

	/**
	 * @param parent
	 * @param columns
	 * @return
	 */
	private Composite createUIContainer(Composite parent, int columns) {
		Composite container = fDetails.getToolkit().createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2; 
		layout.marginHeight = 2;
		layout.numColumns = columns;
		container.setLayout(layout);
		return container;		
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#hookListeners()
	 */
	public void hookListeners() {
		// Create the listeners for the introduction text
		createListenersIntroductionText();
		// Create the listeners for the conclusion text
		createListenersConclusionText();
		// Create the listeners for the tab folder
		createListenersTabFolder();
	}

	/**
	 * 
	 */
	private void createListenersIntroductionText() {
		fIntroductionText.addModifyListener(
				new CompCSIntroductionTextListener(fDataTaskObject));
	}

	/**
	 * 
	 */
	private void createListenersConclusionText() {
		fConclusionText.addModifyListener(
				new CompCSConclusionTextListener(fDataTaskObject));
	}

	/**
	 * 
	 */
	private void createListenersTabFolder() {
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabFolder();
			}
		});
	}	

	/**
	 * 
	 */
	private void updateTabFolder() {

		int index = fTabFolder.getSelectionIndex();
		Control oldControl = fNotebookLayout.topControl;
		
		if (index == F_INTRODUCTION_TAB) {
			fNotebookLayout.topControl = fIntroductionComposite;
		} else if (index == F_CONCLUSION_TAB) {
			fNotebookLayout.topControl = fCompositeConclusion;
		}

		if (oldControl != fNotebookLayout.topControl) {
			fNotebookComposite.layout();		
		}
		
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = fDetails.isEditableElement();
		// Select the introduction tab
		fTabFolder.setSelection(F_INTRODUCTION_TAB);
		// Update tab folder
		updateTabFolder();
		// Update introduction text
		updateIntroductionText(editable);
		// Update conclusion text
		updateConclusionText(editable);
		
		// TODO: MP: LOW: CompCS: Visually indicate which tab has contents
		// specified (perhaps using different image?
	}

	/**
	 * @param editable
	 */
	private void updateIntroductionText(boolean editable) {
		ICompCSIntro intro = fDataTaskObject.getFieldIntro();
		if ((intro != null) &&
				PDETextHelper.isDefined(intro.getFieldContent())) {
			fIntroductionText.setText(intro.getFieldContent());
		}
		fIntroductionText.setEditable(editable);
	}

	/**
	 * @param editable
	 */
	private void updateConclusionText(boolean editable) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();
		if ((conclusion != null) &&
				PDETextHelper.isDefined(conclusion.getFieldContent())) {
			fConclusionText.setText(conclusion.getFieldContent());
		}
		fConclusionText.setEditable(editable);		
	}

}
