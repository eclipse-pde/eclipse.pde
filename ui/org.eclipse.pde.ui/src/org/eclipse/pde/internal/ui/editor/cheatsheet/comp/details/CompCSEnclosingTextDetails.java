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

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractSubDetails;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.comp.CompCSInputContext;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSEnclosingTextDetails
 *
 */
public class CompCSEnclosingTextDetails extends CSAbstractSubDetails {

	private ICompCSTaskObject fDataTaskObject;
	
	private Section fEnclosingTextSection;		
	
	private SourceViewer fIntroductionViewer;

	private SourceViewer fConclusionViewer;

	private XMLConfiguration fSourceConfiguration;
	
	private IColorManager fColorManager;
	
	private IDocument fIntroductionDocument;

	private IDocument fConclusionDocument;
	
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
	public CompCSEnclosingTextDetails(int type, 
			ICSMaster section) {
		super(section, CompCSInputContext.CONTEXT_ID);
		
		fDataTaskObject = null;
		
		fEnclosingTextSection = null;
		
		fIntroductionViewer = null;
		fIntroductionDocument = null;
		fConclusionViewer = null;
		fConclusionDocument = null;
		
		fTabFolder = null;
		fNotebookComposite = null;
		fNotebookLayout = null;
		
		fIntroductionComposite = null;
		fConclusionComposite = null;
		
		fIntroductionListener = new CompCSIntroductionTextListener();
		fConclusionListener = new CompCSConclusionTextListener();
		
		defineTaskObjectLabelName(type);
		setupSourceViewerConfiguration();
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
	private void setupSourceViewerConfiguration() {
		// Get the color manager
		fColorManager = ColorManager.getDefault();
		// Create the source configuration
		fSourceConfiguration = new XMLConfiguration(fColorManager);		
	}
	
	/**
	 * 
	 */
	private void defineTaskObjectLabelName(int type) {
		if (type == ICompCSConstants.TYPE_TASK) {
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
		fEnclosingTextSection = getPage().createUISection(parent,
				PDEUIMessages.CompCSEnclosingTextDetails_EnclosingText,
				description, style);
		// Configure the section
		// The source viewers get clipped when the label above it wraps.
		// Prevent this by making the section fill vertically in addition to
		// horizontally
		GridData data = new GridData(GridData.FILL_BOTH);
		fEnclosingTextSection.setLayoutData(data);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(
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
		fTabFolder.setSelectionBackground(new Color[] { selectedColor,
				colors.getBackground() },
				new int[] { 100 }, true);		
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
		item.setImage(PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_CSCONCLUSION_OBJ));	
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
		String description = NLS.bind(
				PDEUIMessages.CompCSEnclosingTextDetails_IntroductionDescription,
				fTaskObjectLabelName);			
		final Label label = getToolkit().createLabel(
				fIntroductionComposite, description, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
		// Create the underlying document
		fIntroductionDocument = new Document();
		// Create the source viewer
		fIntroductionViewer = createUISourceViewer(fIntroductionComposite, fIntroductionDocument);
		// Note: Must paint border for parent composite; otherwise, the border
		// goes missing on the text widget when using the Windows XP Classic
		// theme
		getToolkit().paintBordersFor(fIntroductionComposite);
	}

	/**
	 * 
	 */
	private SourceViewer createUISourceViewer(Composite parent, IDocument document) {
		// Create the source viewer
		int style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
		SourceViewer viewer = 
			new SourceViewer(parent, null, style);
		// Configure the source viewer
		viewer.configure(fSourceConfiguration);
		// Setup the underlying document
		IDocumentSetupParticipant participant = 
			new XMLDocumentSetupParticpant();
		participant.setup(document);
		// Set the document on the source viewer
		viewer.setDocument(document);
		// Configure the underlying styled text widget
		StyledText styledText = viewer.getTextWidget();
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 60;
		data.widthHint = 60;
		styledText.setLayoutData(data);
		
		return viewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		// Determine which tab is selected
		int index = fTabFolder.getSelectionIndex();
		// Do the global action on the source viewer on that tab
		SourceViewer viewer = null;
		
		if (index == F_INTRODUCTION_TAB) {
			viewer = fIntroductionViewer;
		} else if (index == F_CONCLUSION_TAB) {
			viewer = fConclusionViewer;
		}
		
		return doGlobalActionViewer(actionId, viewer);
	}
	
	/**
	 * @param actionId
	 * @param viewer
	 * @return
	 */
	private boolean doGlobalActionViewer(String actionId, SourceViewer viewer) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			viewer.doOperation(ITextOperationTarget.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			viewer.doOperation(ITextOperationTarget.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			viewer.doOperation(ITextOperationTarget.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			viewer.doOperation(ITextOperationTarget.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			viewer.doOperation(ITextOperationTarget.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			viewer.doOperation(ITextOperationTarget.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			viewer.doOperation(ITextOperationTarget.REDO);
			return true;
		}
		return false;		
	}
	
	/**
	 * 
	 */
	private void createUIConclusionViewer() {
		// Create composite
		fConclusionComposite = createUIContainer(fNotebookComposite, 1);
		// Create label
		String description = NLS.bind(
				PDEUIMessages.CompCSEnclosingTextDetails_ConclusionDescription,
				fTaskObjectLabelName);		
		final Label label = getToolkit().createLabel(
				fConclusionComposite, description, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);		
		// Create the underlying document
		fConclusionDocument = new Document();
		// Create the source viewer
		fConclusionViewer = createUISourceViewer(fConclusionComposite, fConclusionDocument);		
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
		// Create selection listener
		fIntroductionViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				getPage().getPDEEditor().setSelection(event.getSelection());
			}
		});
		// Create focus listener
		fIntroductionViewer.getTextWidget().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				getPage().getPDEEditor().getContributor().updateSelectableActions(null);
			}
		});
		// Create document listener
		fIntroductionDocument.addDocumentListener(fIntroductionListener);		
	}
	
	/**
	 * 
	 */
	private void createListenersConclusionViewer() {
		// Create selection listener
		fConclusionViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				getPage().getPDEEditor().setSelection(event.getSelection());
			}
		});
		// Create focus listener
		fConclusionViewer.getTextWidget().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				getPage().getPDEEditor().getContributor().updateSelectableActions(null);
			}
		});
		// Create document listener
		fConclusionDocument.addDocumentListener(fConclusionListener);				
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
		if ((intro != null) &&
				PDETextHelper.isDefined(intro.getFieldContent())) {
			fIntroductionDocument.set(intro.getFieldContent());
		} else {
			fIntroductionDocument.set(""); //$NON-NLS-1$
		}
		// Unblock for user updates
		fIntroductionListener.setBlockEvents(false);
		
		fIntroductionViewer.setEditable(editable);
	}

	/**
	 * @param editable
	 */
	private void updateConclusionViewer(boolean editable) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();
		
		// Block listener from handling this update
		fConclusionListener.setBlockEvents(true);
		if ((conclusion != null) &&
				PDETextHelper.isDefined(conclusion.getFieldContent())) {
			fConclusionDocument.set(conclusion.getFieldContent());
		} else {
			fConclusionDocument.set(""); //$NON-NLS-1$
		}
		// Unblock for user updates
		fConclusionListener.setBlockEvents(false);		
		
		fConclusionViewer.setEditable(editable);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// TODO: MP: CompCS: Profile Sleek when making static to ensure no leaks
		// Set the context menu to null to prevent the editor context menu
		// from being disposed along with the source viewer
		unsetSourceViewerMenu(fIntroductionViewer);
		unsetSourceViewerMenu(fConclusionViewer);
		// Dispose of the color manager
		if (fColorManager != null) {
			fColorManager.dispose();
			fColorManager = null;
		}
		// Dispose of the source configuration
		if (fSourceConfiguration != null) {
			fSourceConfiguration.dispose();
			fSourceConfiguration = null;
		}
		super.dispose();
	}

	/**
	 * @param viewer
	 */
	private void unsetSourceViewerMenu(SourceViewer viewer) {
		if (viewer == null) {
			return;
		}
		StyledText styledText = viewer.getTextWidget();
		if (styledText == null) {
			return;
		} else if (styledText.isDisposed()) {
			return;
		}
		styledText.setMenu(null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#canPaste(org.eclipse.swt.dnd.Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		// Determine which tab is selected
		int index = fTabFolder.getSelectionIndex();
		// Check if the source viewer on that tab can paste
		SourceViewer viewer = null;
		
		if (index == F_INTRODUCTION_TAB) {
			viewer = fIntroductionViewer;
		} else if (index == F_CONCLUSION_TAB) {
			viewer = fConclusionViewer;
		}		
		
		return viewer.canDoOperation(ITextOperationTarget.PASTE);
	}
}
