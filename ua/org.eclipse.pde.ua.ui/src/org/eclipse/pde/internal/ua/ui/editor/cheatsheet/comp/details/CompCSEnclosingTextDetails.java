/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractSubDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.CompCSInputContext;
import org.eclipse.pde.internal.ui.parts.PDESourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSEnclosingTextDetails
 *
 */
public class CompCSEnclosingTextDetails extends CSAbstractSubDetails {

	private ICompCSTaskObject fDataTaskObject;

	private Section fEnclosingTextSection;

	private PDESourceViewer fIntroductionViewer;

	private PDESourceViewer fConclusionViewer;

	private CTabFolder fTabFolder;

	private final static int F_INTRODUCTION_TAB = 0;

	private final static int F_CONCLUSION_TAB = 1;

	private final static int F_NO_TAB = -1;

	private Composite fNotebookComposite;

	private StackLayout fNotebookLayout;

	private Composite fIntroductionComposite;

	private Composite fConclusionComposite;

	private String fTaskObjectLabelName;

	private CompCSIntroductionTextListener fIntroductionListener;

	private CompCSConclusionTextListener fConclusionListener;

	/**
	 * 
	 */
	public CompCSEnclosingTextDetails(int type, ICSMaster section) {
		super(section, CompCSInputContext.CONTEXT_ID);

		fDataTaskObject = null;

		fEnclosingTextSection = null;

		fIntroductionViewer = null;
		fConclusionViewer = null;

		fTabFolder = null;
		fNotebookComposite = null;
		fNotebookLayout = null;

		fIntroductionComposite = null;
		fConclusionComposite = null;

		fIntroductionListener = new CompCSIntroductionTextListener();
		fConclusionListener = new CompCSConclusionTextListener();

		defineTaskObjectLabelName(type);
	}

	/**
	 * @param object
	 */
	public void setData(ICompCSTaskObject object) {
		// Set data
		fDataTaskObject = object;
		// Set data on introduction text listener
		fIntroductionListener.setData(object);
		// Set data on conclusion text listener
		fConclusionListener.setData(object);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
	}

	/**
	 * 
	 */
	private void defineTaskObjectLabelName(int type) {
		if (type == ICompCSConstants.TYPE_TASK) {
			fTaskObjectLabelName = DetailsMessages.CompCSEnclosingTextDetails_task;
		} else {
			fTaskObjectLabelName = DetailsMessages.CompCSEnclosingTextDetails_group;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {
		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		String description = NLS.bind(DetailsMessages.CompCSEnclosingTextDetails_description, fTaskObjectLabelName);
		fEnclosingTextSection = getPage().createUISection(parent, DetailsMessages.CompCSEnclosingTextDetails_enclosing, description, style);
		// Configure the section
		// The source viewers get clipped when the label above it wraps.
		// Prevent this by making the section fill vertically in addition to
		// horizontally
		GridData data = new GridData(GridData.FILL_BOTH);
		fEnclosingTextSection.setLayoutData(data);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(fEnclosingTextSection, 1);
		// Create the tab folder
		createUITabFolder(sectionClient);
		// Create the introduction folder tab
		createUIIntroductionTab();
		// Create the conclusion folder tab
		createUIConclusionTab();
		// Create the notebook composite
		createUINotebookComposite(sectionClient);
		// Create the introduction text
		createUIIntroductionViewer();
		// Create the conclusion text
		createUIConclusionViewer();
		// Bind widgets
		getToolkit().paintBordersFor(sectionClient);
		fEnclosingTextSection.setClient(sectionClient);
		// Mark as a details part to enable cut, copy, paste, etc.		
		markDetailsPart(fEnclosingTextSection);
	}

	/**
	 * @param parent
	 */
	private void createUITabFolder(Composite parent) {
		fTabFolder = new CTabFolder(parent, SWT.FLAT | SWT.TOP);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.heightHint = 2;
		fTabFolder.setLayoutData(data);

		getToolkit().adapt(fTabFolder, true, true);

		FormColors colors = getToolkit().getColors();
		colors.initializeSectionToolBarColors();
		Color selectedColor = colors.getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, colors.getBackground()}, new int[] {100}, true);
	}

	/**
	 * 
	 */
	private void createUIIntroductionTab() {
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(DetailsMessages.CompCSEnclosingTextDetails_introduction);
		item.setImage(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().get(PDEUserAssistanceUIPluginImages.DESC_CSINTRO_OBJ));
	}

	/**
	 * 
	 */
	private void createUIConclusionTab() {
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(DetailsMessages.CompCSEnclosingTextDetails_conclusion);
		item.setImage(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().get(PDEUserAssistanceUIPluginImages.DESC_CSCONCLUSION_OBJ));
	}

	/**
	 * @param parent
	 */
	private void createUINotebookComposite(Composite parent) {
		fNotebookComposite = getToolkit().createComposite(parent);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fNotebookComposite.setLayoutData(data);
		fNotebookLayout = new StackLayout();
		fNotebookComposite.setLayout(fNotebookLayout);
	}

	/**
	 * 
	 */
	private void createUIIntroductionViewer() {
		// Create composite
		fIntroductionComposite = createUIContainer(fNotebookComposite, 1);
		// Create label
		String description = NLS.bind(DetailsMessages.CompCSEnclosingTextDetails_label, fTaskObjectLabelName);
		final Label label = getToolkit().createLabel(fIntroductionComposite, description, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
		// Create the source viewer
		fIntroductionViewer = new PDESourceViewer(getPage());
		fIntroductionViewer.createUI(fIntroductionComposite, 60, 60);
		// Note: Must paint border for parent composite; otherwise, the border
		// goes missing on the text widget when using the Windows XP Classic
		// theme
		getToolkit().paintBordersFor(fIntroductionComposite);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		// Determine which tab is selected
		int index = fTabFolder.getSelectionIndex();
		// Do the global action on the source viewer on that tab
		PDESourceViewer viewer = null;

		if (index == F_INTRODUCTION_TAB) {
			viewer = fIntroductionViewer;
		} else if (index == F_CONCLUSION_TAB) {
			viewer = fConclusionViewer;
		}

		return viewer.doGlobalAction(actionId);
	}

	/**
	 * 
	 */
	private void createUIConclusionViewer() {
		// Create composite
		fConclusionComposite = createUIContainer(fNotebookComposite, 1);
		// Create label
		String description = NLS.bind(DetailsMessages.CompCSEnclosingTextDetails_labelDescription, fTaskObjectLabelName);
		final Label label = getToolkit().createLabel(fConclusionComposite, description, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
		// Create the source viewer
		fConclusionViewer = new PDESourceViewer(getPage());
		fConclusionViewer.createUI(fConclusionComposite, 60, 60);
		// Note: Must paint border for parent composite; otherwise, the border
		// goes missing on the text widget when using the Windows XP Classic
		// theme
		getToolkit().paintBordersFor(fConclusionComposite);
	}

	/**
	 * @param parent
	 * @param columns
	 * @return
	 */
	private Composite createUIContainer(Composite parent, int columns) {
		Composite container = getToolkit().createComposite(parent);
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
		createListenersIntroductionViewer();
		// Create the listeners for the conclusion text
		createListenersConclusionViewer();
		// Create the listeners for the tab folder
		createListenersTabFolder();
	}

	/**
	 * 
	 */
	private void createListenersIntroductionViewer() {
		fIntroductionViewer.createUIListeners();
		// Create document listener
		fIntroductionViewer.getDocument().addDocumentListener(fIntroductionListener);
	}

	/**
	 * 
	 */
	private void createListenersConclusionViewer() {
		fConclusionViewer.createUIListeners();
		// Create document listener
		fConclusionViewer.getDocument().addDocumentListener(fConclusionListener);
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

		if (index == F_NO_TAB) {
			// Select the introduction contents by default
			fNotebookLayout.topControl = fIntroductionComposite;
			// Select the introduction tab by default to match
			// Does not trigger selection adapter (only user UI selections do)
			fTabFolder.setSelection(F_INTRODUCTION_TAB);
		} else if (index == F_INTRODUCTION_TAB) {
			fNotebookLayout.topControl = fIntroductionComposite;
		} else if (index == F_CONCLUSION_TAB) {
			fNotebookLayout.topControl = fConclusionComposite;
		}

		if (oldControl != fNotebookLayout.topControl) {
			fNotebookComposite.layout();
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fDataTaskObject == null) {
			return;
		}
		boolean editable = isEditableElement();
		// Update tab folder
		updateTabFolder();
		// Update introduction text
		updateIntroductionViewer(editable);
		// Update conclusion text
		updateConclusionViewer(editable);
	}

	/**
	 * @param editable
	 */
	private void updateIntroductionViewer(boolean editable) {
		ICompCSIntro intro = fDataTaskObject.getFieldIntro();

		// Block listener from handling this update
		fIntroductionListener.setBlockEvents(true);
		if ((intro != null) && PDETextHelper.isDefined(intro.getFieldContent())) {
			fIntroductionViewer.getDocument().set(intro.getFieldContent());
		} else {
			fIntroductionViewer.getDocument().set(""); //$NON-NLS-1$
		}
		// Unblock for user updates
		fIntroductionListener.setBlockEvents(false);

		fIntroductionViewer.getViewer().setEditable(editable);
	}

	/**
	 * @param editable
	 */
	private void updateConclusionViewer(boolean editable) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();

		// Block listener from handling this update
		fConclusionListener.setBlockEvents(true);
		if ((conclusion != null) && PDETextHelper.isDefined(conclusion.getFieldContent())) {
			fConclusionViewer.getDocument().set(conclusion.getFieldContent());
		} else {
			fConclusionViewer.getDocument().set(""); //$NON-NLS-1$
		}
		// Unblock for user updates
		fConclusionListener.setBlockEvents(false);

		fConclusionViewer.getViewer().setEditable(editable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Set the context menu to null to prevent the editor context menu
		// from being disposed along with the source viewer
		if (fIntroductionViewer != null) {
			fIntroductionViewer.unsetMenu();
			fIntroductionViewer = null;
		}
		if (fConclusionViewer != null) {
			fConclusionViewer.unsetMenu();
			fConclusionViewer = null;
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#canPaste(org.eclipse.swt.dnd.Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		// Determine which tab is selected
		int index = fTabFolder.getSelectionIndex();
		// Check if the source viewer on that tab can paste
		PDESourceViewer viewer = null;

		if (index == F_INTRODUCTION_TAB) {
			viewer = fIntroductionViewer;
		} else if (index == F_CONCLUSION_TAB) {
			viewer = fConclusionViewer;
		}

		return viewer.canPaste();
	}
}
