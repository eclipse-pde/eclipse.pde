/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.*;

public class InfoSection extends PDESection {
	private IDocument fDocument;

	private XMLConfiguration fSourceConfiguration;

	private SourceViewer fSourceViewer;

	private CTabFolder fTabFolder;

	private Text fUrlText;

	private Object fElement;

	private int fElementIndex;

	private boolean fIgnoreChange;

	private Composite fNotebook;

	private StackLayout fNotebookLayout;

	private Control fInfoPage;

	private Control fUrlsPage;

	private Control fLicensePage;

	public InfoSection(PDEFormPage page, Composite parent, IColorManager colorManager) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.NO_TITLE, false);
		String description = PDEUIMessages.FeatureEditor_InfoSection_desc;
		getSection().setDescription(description);
		fSourceConfiguration = new XMLConfiguration(colorManager);
		fDocument = new Document();
		new XMLDocumentSetupParticpant().setup(fDocument);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		handleApply();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData gd;

		toolkit.createLabel(container, null);
		fTabFolder = new CTabFolder(container, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, new int[] {100}, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection();
			}
		});

		fNotebook = toolkit.createComposite(container);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fNotebook.setLayoutData(gd);
		fNotebookLayout = new StackLayout();
		fNotebook.setLayout(fNotebookLayout);

		fInfoPage = createInfoPage(toolkit, fNotebook);
		fLicensePage = createLicensePage(toolkit, fNotebook);
		fUrlsPage = createUrlsPage(toolkit, fNotebook);
		fNotebookLayout.topControl = fInfoPage;

		createTabs();
		section.setClient(container);
		initialize();
		if (fTabFolder.getItemCount() > 0) {
			fTabFolder.setSelection(0);
			updateTabSelection();
		}
	}

	private Control createLicensePage(FormToolkit toolkit, Composite parent) {
		Composite page = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 0;
		layout.verticalSpacing = 8;
		layout.marginTop = 0;
		page.setLayout(layout);

		LicenseFeatureSection licenseSection = new LicenseFeatureSection(getPage(), page, fSourceConfiguration);
		licenseSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

		getManagedForm().addPart(licenseSection);
		return page;
	}

	/**
	 * @param toolkit
	 * @param parent
	 */
	private Control createInfoPage(FormToolkit toolkit, Composite parent) {
		Composite page = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 5;
		layout.verticalSpacing = 8;
		page.setLayout(layout);

		GridData gd;
		Label label = toolkit.createLabel(page, PDEUIMessages.FeatureEditor_InfoSection_url);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		fUrlText = toolkit.createText(page, null, SWT.SINGLE);
		fUrlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				infoModified();
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUrlText.setLayoutData(gd);
		label = toolkit.createLabel(page, PDEUIMessages.FeatureEditor_InfoSection_text);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		int styles = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL;
		fSourceViewer = new SourceViewer(page, null, styles);
		fSourceViewer.configure(fSourceConfiguration);
		fSourceViewer.setDocument(fDocument);
		fSourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		StyledText styledText = fSourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		//
		if (SWT.getPlatform().equals("motif") == false) //$NON-NLS-1$
			toolkit.paintBordersFor(page);
		Control[] children = page.getChildren();
		Control control = children[children.length - 1];
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.widthHint = 50;
		gd.heightHint = 50;
		control.setLayoutData(gd);

		return page;
	}

	/**
	 * @param toolkit
	 * @param parent
	 */
	private Control createUrlsPage(FormToolkit toolkit, Composite parent) {
		Composite page = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 2;
		layout.marginHeight = 5;
		layout.verticalSpacing = 8;
		page.setLayout(layout);

		URLSection urlSection = new URLSection(getPage(), page);
		urlSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

		URLDetailsSection urlDetailsSection = new URLDetailsSection(getPage(), page);
		urlDetailsSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

		getManagedForm().addPart(urlSection);
		getManagedForm().addPart(urlDetailsSection);
		return page;
	}

	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.CUT);
			return true;
		} else if (actionId.equals(ActionFactory.COPY.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.COPY);
			return true;
		} else if (actionId.equals(ActionFactory.PASTE.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.PASTE);
			return true;
		} else if (actionId.equals(ActionFactory.DELETE.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.DELETE);
			return true;
		} else if (actionId.equals(ActionFactory.UNDO.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.UNDO);
			return true;
		} else if (actionId.equals(ActionFactory.REDO.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.REDO);
			return true;
		}
		return false;
	}

	public boolean setFormInput(Object input) {
		if (input instanceof IFeatureInfo) {
			IFeatureInfo info = (IFeatureInfo) input;
			int index = info.getIndex();
			if (index != -1)
				fTabFolder.setSelection(index);
			updateEditorInput(input, false);
			return true;
		}
		if (input instanceof IFeatureURLElement || input instanceof NamedElement) {
			fTabFolder.setSelection(3);
			updateEditorInput(input, false);
			return true;
		}
		return false;
	}

	private void handleApply() {
		if (0 <= fElementIndex && fElementIndex < 2 && fElement != null) {
			handleApply((IFeatureInfo) fElement, fTabFolder.getSelectionIndex());
		} else {
			handleApply(null, fTabFolder.getSelectionIndex());
		}
	}

	private void handleApply(IFeatureInfo info, int index) {
		if (index >= 2)
			return;
		String urlName = fUrlText.getText();
		String text = fDocument.get();
		applyInfoText(info, urlName, text, index);
		updateTabImage(fTabFolder.getSelection());
	}

	private void applyInfoText(IFeatureInfo targetInfo, String urlText, String text, int index) {
		String url = null;

		if (urlText.length() > 0) {
			url = urlText;
		}
		try {
			IFeatureModel model = (IFeatureModel) getPage().getModel();
			IFeature feature = model.getFeature();

			if (targetInfo == null)
				targetInfo = feature.getFeatureInfo(index);

			if (targetInfo == null) {
				targetInfo = model.getFactory().createInfo(index);
				feature.setFeatureInfo(targetInfo, index);
			}
			targetInfo.setURL(url);
			targetInfo.setDescription(text);
		} catch (CoreException e) {
			// TODO: This should probably at least be logged
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	public void initialize() {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		fDocument.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				infoModified();
			}

			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
		fUrlText.setEditable(featureModel.isEditable());
		fSourceViewer.getTextWidget().setEditable(featureModel.isEditable());
		featureModel.addModelChangedListener(this);
		updateEditorInput(featureModel.getFeature().getFeatureInfo(0), false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Dispose of the source configuration
		if (fSourceConfiguration != null) {
			fSourceConfiguration.dispose();
		}
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		if (featureModel != null)
			featureModel.removeModelChangedListener(this);
		super.dispose();
	}

	private void infoModified() {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		if (!fIgnoreChange && featureModel instanceof IEditable) {
			markDirty();
		}
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		int index = fTabFolder.getSelectionIndex();
		if (index < 3) {
			IFeatureInfo info = model.getFeature().getFeatureInfo(index);
			fElement = null;
			fElementIndex = -1;
			updateEditorInput(info, false);
		}
		super.refresh();
	}

	private void createTabs() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		addTab(PDEUIMessages.FeatureEditor_info_description, feature.getFeatureInfo(0));
		addTab(PDEUIMessages.FeatureEditor_info_copyright, feature.getFeatureInfo(1));
		addTab(PDEUIMessages.FeatureEditor_info_license, feature.getFeatureInfo(2));
		addTab(PDEUIMessages.FeatureEditor_info_discoveryUrls, null);
	}

	private void addTab(String label, IFeatureInfo info) {
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(label);
		item.setData(info);
		updateTabImage(item);
	}

	private void updateTabImage(CTabItem item) {
		if (item == null)
			return;
		Object info = item.getData();
		if (info != null) {
			item.setImage(PDEPlugin.getDefault().getLabelProvider().getImage(info));
		} else {
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DOC_SECTION_OBJ));
		}
	}

	private void updateTabSelection() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		int index = fTabFolder.getSelectionIndex();
		if (index < 3) {
			IFeatureInfo info = feature.getFeatureInfo(index);
			updateEditorInput(info, true);
		}

		// TODO: Why is this code in both updateEditorInput and updateTabSelection?
		Control oldPage = fNotebookLayout.topControl;
		switch (index) {
			case 0 :
			case 1 :
				fNotebookLayout.topControl = fInfoPage;
				break;
			case 2 :
				fNotebookLayout.topControl = fLicensePage;
				break;
			case 3 :
				fNotebookLayout.topControl = fUrlsPage;
				break;
		}
		if (oldPage != fNotebookLayout.topControl)
			fNotebook.layout();
	}

	public void setFocus() {
		fSourceViewer.getTextWidget().setFocus();
		updateSelection(fSourceViewer.getSelection());
	}

	private void commitPrevious() {
		IFeatureInfo previous = (IFeatureInfo) fElement;
		handleApply(previous, fElementIndex);
	}

	public void updateEditorInput(Object input, boolean commitPrevious) {
		if (isDirty() && commitPrevious /*
											 * && element != null && element !=
											 * input
											 */) {
			commitPrevious();
		}
		fIgnoreChange = true;
		String text = ""; //$NON-NLS-1$
		String url = null;
		if (input instanceof IFeatureInfo) {
			IFeatureInfo info = (IFeatureInfo) input;
			text = info.getDescription();
			url = info.getURL();
		}
		if (text == null)
			text = ""; //$NON-NLS-1$
		else
			text = TextUtil.createMultiLine(text, 60, false);
		fDocument.set(text);
		if (url == null)
			fUrlText.setText(""); //$NON-NLS-1$
		else
			fUrlText.setText(url.toString());
		fElement = input;
		fElementIndex = fTabFolder.getSelectionIndex();

		Control oldPage = fNotebookLayout.topControl;
		switch (fElementIndex) {
			case 0 :
			case 1 :
				fNotebookLayout.topControl = fInfoPage;
				break;
			case 2 :
				fNotebookLayout.topControl = fLicensePage;
				break;
			case 3 :
				fNotebookLayout.topControl = fUrlsPage;
				break;
		}
		if (oldPage != fNotebookLayout.topControl)
			fNotebook.layout();

		fIgnoreChange = false;
	}

	public boolean canPaste(Clipboard clipboard) {
		return fSourceViewer.canDoOperation(ITextOperationTarget.PASTE);
	}
}
