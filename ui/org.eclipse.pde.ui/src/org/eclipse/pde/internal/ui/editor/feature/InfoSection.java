/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.XMLConfiguration;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class InfoSection extends PDESection {
	private static final String KEY_APPLY = "Actions.apply.flabel"; //$NON-NLS-1$
	private static final String KEY_RESET = "Actions.reset.flabel"; //$NON-NLS-1$
	private static final String SECTION_DESC = "FeatureEditor.InfoSection.desc"; //$NON-NLS-1$
	private static final String KEY_URL = "FeatureEditor.InfoSection.url"; //$NON-NLS-1$
	private static final String KEY_TEXT = "FeatureEditor.InfoSection.text"; //$NON-NLS-1$
	private static final String KEY_INFO_DESCRIPTION =
		"FeatureEditor.info.description"; //$NON-NLS-1$
	private static final String KEY_INFO_LICENSE = "FeatureEditor.info.license"; //$NON-NLS-1$
	private static final String KEY_INFO_COPYRIGHT =
		"FeatureEditor.info.copyright"; //$NON-NLS-1$
	private IDocument document;
	private IDocumentPartitioner partitioner;
	private SourceViewerConfiguration sourceConfiguration;
	private SourceViewer sourceViewer;
	private CTabFolder tabFolder;
	private Text urlText;
	private Button applyButton;
	private Button resetButton;
	private Object element;
	private int elementIndex;
	private boolean ignoreChange;

	public InfoSection(PDEFormPage page, Composite parent, IColorManager colorManager) {
		super(page, parent, Section.DESCRIPTION|Section.NO_TITLE, false);
		String description = PDEPlugin.getResourceString(SECTION_DESC);
		getSection().setDescription(description);
		sourceConfiguration = new XMLConfiguration(colorManager);
		document = new Document();
		partitioner =
			new DefaultPartitioner(
				new XMLPartitionScanner(),
				new String[] {
					XMLPartitionScanner.XML_TAG,
					XMLPartitionScanner.XML_COMMENT });
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		handleApply();

		if (onSave) {
			resetButton.setEnabled(false);
		}
		super.commit(onSave);
	}
	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 2;
		layout.marginHeight = 5;
		layout.verticalSpacing = 8;
		container.setLayout(layout);
		GridData gd;
		
		toolkit.createLabel(container, null);
		tabFolder = new CTabFolder(container, SWT.FLAT|SWT.TOP);
		toolkit.adapt(tabFolder, true, true);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		tabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		tabFolder.setSelectionBackground(new Color[] {selectedColor1, selectedColor2, toolkit.getColors().getBackground()}, new int[] {50, 100}, true);

		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection();
			}
		});

		Label label = toolkit.createLabel(container, PDEPlugin.getResourceString(KEY_URL));
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));		

		urlText = toolkit.createText(container, null, SWT.SINGLE);
		urlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				infoModified();
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL);
		urlText.setLayoutData(gd);

		toolkit.createLabel(container, null);

		label =
			toolkit.createLabel(
				container,
				PDEPlugin.getResourceString(KEY_TEXT));
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));		
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);

		int styles = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL;
		sourceViewer = new SourceViewer(container, null, styles);
		sourceViewer.configure(sourceConfiguration);
		sourceViewer.setDocument(document);
		sourceViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		StyledText styledText = sourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

		if (SWT.getPlatform().equals("motif") == false) //$NON-NLS-1$
			toolkit.paintBordersFor(container);
		Control[] children = container.getChildren();
		Control control = children[children.length - 1];
		gd =
			new GridData(
				GridData.FILL_BOTH
					| GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL);
		//gd.widthHint = 600;
		//gd.heightHint = 600;
		gd.widthHint = 50;
		gd.heightHint = 50;
		control.setLayoutData(gd);
		Composite buttonContainer = toolkit.createComposite(container);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);

		applyButton =
			toolkit.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_APPLY),
				SWT.PUSH);
		applyButton.setEnabled(false);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		applyButton.setLayoutData(gd);
		applyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleApply();
			}
		});

		resetButton =
			toolkit.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_RESET),
				SWT.PUSH);
		resetButton.setEnabled(false);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		resetButton.setLayoutData(gd);
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReset();
			}
		});
		createTabs();
		section.setClient(container);
		initialize();
		if (tabFolder.getItemCount()>0) {
			tabFolder.setSelection(0);		
			updateTabSelection();
		}
	}

	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			sourceViewer.doOperation(SourceViewer.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			sourceViewer.doOperation(SourceViewer.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			sourceViewer.doOperation(SourceViewer.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			sourceViewer.doOperation(SourceViewer.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			sourceViewer.doOperation(SourceViewer.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			sourceViewer.doOperation(SourceViewer.REDO);
			return true;
		}
		return false;
	}
	public boolean setFormInput(Object input) {
		if (input instanceof IFeatureInfo) {
			IFeatureInfo info = (IFeatureInfo) input;
			int index = info.getIndex();
			if (index!= -1)
				tabFolder.setSelection(index);
			updateEditorInput(input, false);
			return true;
		}
		return false;
	}

	private void handleApply() {
		handleApply(null, tabFolder.getSelectionIndex());
	}

	private void handleApply(IFeatureInfo info, int index) {
		String urlName = urlText.getText();
		String text = document.get();
		updateInfoText(info, urlName, text, index);
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
		updateTabImage(tabFolder.getSelection());
	}

	private void updateInfoText(
		IFeatureInfo targetInfo,
		String urlText,
		String text,
		int index) {
		String url = null;

		if (urlText.length() > 0) {
			url = urlText;
		}
		try {
			IFeatureModel model = (IFeatureModel) getPage().getModel();
			IFeature feature = model.getFeature();
			IFeatureInfo info = targetInfo;

			if (info == null) {
				info = feature.getFeatureInfo(index);
			}

			if (targetInfo == null && info == null) {
				info = model.getFactory().createInfo(index);
				feature.setFeatureInfo(info, index);
			}
			info.setURL(url);
			info.setDescription(text);
		} catch (CoreException e) {
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
	}


	private void handleReset() {
		updateEditorInput(element, false);
		updateTabImage(tabFolder.getSelection());
	}

	public void initialize() {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		document.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				infoModified();
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
		urlText.setEditable(featureModel.isEditable());
		sourceViewer.getTextWidget().setEditable(featureModel.isEditable());
		featureModel.addModelChangedListener(this);
		updateEditorInput(featureModel.getFeature().getFeatureInfo(0), false);
	}

	public void dispose() {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		featureModel.removeModelChangedListener(this);
		super.dispose();
	}

	private void infoModified() {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		if (!ignoreChange && featureModel instanceof IEditable) {
			markDirty();
		}
		applyButton.setEnabled(true);
		resetButton.setEnabled(true);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}
	
	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		int index = tabFolder.getSelectionIndex();
		IFeatureInfo info = model.getFeature().getFeatureInfo(index);
		element = null;
		elementIndex = -1;
		updateEditorInput(info, false);
		super.refresh();
	}

	private void createTabs() {
		IFeatureModel model = (IFeatureModel)getPage().getModel();
		IFeature feature = model.getFeature();
		addTab(PDEPlugin.getResourceString(KEY_INFO_DESCRIPTION), feature.getFeatureInfo(0));
		addTab(PDEPlugin.getResourceString(KEY_INFO_COPYRIGHT), feature.getFeatureInfo(1));
		addTab(PDEPlugin.getResourceString(KEY_INFO_LICENSE), feature.getFeatureInfo(2));
	}
	private void addTab(String label, IFeatureInfo info) {
		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setText(label);
		item.setData(info);
		updateTabImage(item);
	}
	private void updateTabImage(CTabItem item) {
		if (item==null) return;
		IFeatureInfo info = (IFeatureInfo)item.getData();
		if (info==null) return;
		item.setImage(PDEPlugin.getDefault().getLabelProvider().getImage(info));
	}

	private void updateTabSelection() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		int index = tabFolder.getSelectionIndex();
		IFeatureInfo info = feature.getFeatureInfo(index);
		updateEditorInput(info, true);
	}

	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
		updateSelection(sourceViewer.getSelection());
	}

	private void commitPrevious() {
		IFeatureInfo previous = (IFeatureInfo) element;
		handleApply(previous, elementIndex);
	}

	public void updateEditorInput(Object input, boolean commitPrevious) {
		if (isDirty()
			&& commitPrevious /*
			&& element != null
			&& element != input */) {
			commitPrevious();
		}
		ignoreChange = true;
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
		document.set(text);
		if (url == null)
			urlText.setText(""); //$NON-NLS-1$
		else
			urlText.setText(url.toString());
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
		element = input;
		elementIndex = tabFolder.getSelectionIndex();
		ignoreChange = false;
	}

	public boolean canPaste(Clipboard clipboard) {
		return sourceViewer.canDoOperation(SourceViewer.PASTE);
	}
}
