/*******************************************************************************
 *  Copyright (c) 2010, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Provides the UI for the License Agreement section of the Information page in the Feature Editor.
 * There are two radio buttons which allow the user to choose between setting a license in text
 * or to point at a feature which specifies the licensing.
 *
 * @since 3.7
 * @see InfoSection
 * @see FeatureEditor
 */
public class LicenseFeatureSection extends PDESection {

	private Text fLicenseFeatureIDText;
	private Button fLicenseButton;
	private Text fLicenseFeatureVersionText;
	private Text fUrlText;
	private SourceViewer fSourceViewer;
	private final SourceViewerConfiguration fSourceConfiguration;
	private final IDocument fDocument;
	private boolean fIgnoreChange;
	private Button fSharedLicenseButton;
	private Button fLocalLicenseButton;

	public LicenseFeatureSection(PDEFormPage page, Composite parent, XMLConfiguration fSourceConfiguration) {
		super(page, parent, ExpandableComposite.NO_TITLE, false);
		this.fSourceConfiguration = fSourceConfiguration;
		fDocument = new Document();
		new XMLDocumentSetupParticpant().setup(fDocument);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	/**
	 * Creates the UI in the given section using the provided toolkit
	 */
	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite page = toolkit.createComposite(section);
		final StackLayout stackLayout = new StackLayout();

		GridLayout layout = FormLayoutFactory.createClearGridLayout(false, 2);
		layout.horizontalSpacing = 8;
		page.setLayout(layout);
		fSharedLicenseButton = toolkit.createButton(page, PDEUIMessages.FeatureEditor_licenseFeatureSection_sharedButton, SWT.RADIO);
		fLocalLicenseButton = toolkit.createButton(page, PDEUIMessages.FeatureEditor_licenseFeatureSection_localButton, SWT.RADIO);

		GridData gd = new GridData();
		gd.horizontalIndent = 5;
		fLocalLicenseButton.setLayoutData(gd);

		final Composite sectionsComposite = toolkit.createComposite(page);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		sectionsComposite.setLayoutData(gd);
		sectionsComposite.setLayout(stackLayout);

		// Shared Section

		final Composite licenseFeatureComposite = toolkit.createComposite(sectionsComposite);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		licenseFeatureComposite.setLayoutData(gd);

		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 2;
		layout.marginHeight = 5;
		layout.verticalSpacing = 8;
		licenseFeatureComposite.setLayout(layout);

		Label label = toolkit.createLabel(licenseFeatureComposite, PDEUIMessages.FeatureEditor_licenseFeatureSection_featureID);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fLicenseFeatureIDText = toolkit.createText(licenseFeatureComposite, null, SWT.SINGLE);
		fLicenseFeatureIDText.addModifyListener(e -> infoModified());

		fLicenseFeatureIDText.setEditable(true);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fLicenseFeatureIDText.setLayoutData(gd);

		fLicenseButton = toolkit.createButton(licenseFeatureComposite, PDEUIMessages.FeatureEditor_licenseFeatureSection_browse, SWT.PUSH);
		fLicenseButton.addSelectionListener(widgetSelectedAdapter(e -> handleSelect()));

		label = toolkit.createLabel(licenseFeatureComposite, PDEUIMessages.FeatureEditor_licenseFeatureSection_featureVersion);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fLicenseFeatureVersionText = toolkit.createText(licenseFeatureComposite, null, SWT.SINGLE);
		fLicenseFeatureVersionText.addModifyListener(e -> infoModified());

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fLicenseFeatureVersionText.setLayoutData(gd);

		// Local Section

		final Composite localLicenseComposite = toolkit.createComposite(sectionsComposite);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		localLicenseComposite.setLayoutData(gd);

		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 5;
		layout.verticalSpacing = 8;
		localLicenseComposite.setLayout(layout);

		label = toolkit.createLabel(localLicenseComposite, PDEUIMessages.FeatureEditor_InfoSection_url);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fUrlText = toolkit.createText(localLicenseComposite, null, SWT.SINGLE);
		fUrlText.addModifyListener(e -> infoModified());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUrlText.setLayoutData(gd);

		label = toolkit.createLabel(localLicenseComposite, PDEUIMessages.FeatureEditor_InfoSection_text);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);

		int styles = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL;
		fSourceViewer = new SourceViewer(localLicenseComposite, null, styles);
		fSourceViewer.configure(fSourceConfiguration);
		fSourceViewer.setDocument(fDocument);
		fSourceViewer.addSelectionChangedListener(event -> updateSelection(event.getSelection()));
		StyledText styledText = fSourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		fDocument.addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent e) {
				infoModified();
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});

		if (SWT.getPlatform().equals("motif") == false) { //$NON-NLS-1$
			toolkit.paintBordersFor(localLicenseComposite);
		}

		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.widthHint = 50;
		gd.heightHint = 50;
		styledText.setLayoutData(gd);

		fSharedLicenseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (((Button) e.widget).getSelection()) {
				stackLayout.topControl = licenseFeatureComposite;
				sectionsComposite.layout();
			}
		}));
		fLocalLicenseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (((Button) e.widget).getSelection()) {
				stackLayout.topControl = localLicenseComposite;
				sectionsComposite.layout();
			}
		}));

		section.setClient(page);

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		if (feature.getLicenseFeatureID() == null || feature.getLicenseFeatureID().length() == 0) {
			fLocalLicenseButton.setSelection(true);
			fSharedLicenseButton.setSelection(false);
			stackLayout.topControl = localLicenseComposite;
		} else {
			fLocalLicenseButton.setSelection(false);
			fSharedLicenseButton.setSelection(true);
			stackLayout.topControl = licenseFeatureComposite;
		}
		model.addModelChangedListener(this);

		toolkit.paintBordersFor(licenseFeatureComposite);

	}

	private void handleSelect() {
		BusyIndicator.showWhile(fLicenseFeatureIDText.getDisplay(), new Runnable() {
			@Override
			public void run() {
				IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
				ArrayList<IFeatureModel> newModels = new ArrayList<>();
				for (IFeatureModel model : allModels) {
					if (canAdd(model))
						newModels.add(model);
				}
				IFeatureModel[] candidateModels = newModels.toArray(new IFeatureModel[newModels.size()]);
				FeatureSelectionDialog dialog = new FeatureSelectionDialog(fLicenseFeatureIDText.getShell(), candidateModels, false);
				if (dialog.open() == Window.OK) {
					Object[] models = dialog.getResult();
					doSelect((IFeatureModel) models[0]);
				}
			}

			private void doSelect(IFeatureModel licenseFeatureModel) {
				IFeature licenseFeature = licenseFeatureModel.getFeature();
				fLicenseFeatureIDText.setText(licenseFeature.getId());
				fLicenseFeatureVersionText.setText(licenseFeature.getVersion());
			}

			private boolean canAdd(IFeatureModel candidate) {
				IFeatureModel model = (IFeatureModel) getPage().getModel();
				IFeature feature = model.getFeature();
				String id = feature.getId();
				String candidateID = candidate.getFeature().getId();
				return !candidateID.equals(id) && !candidateID.equals(fLicenseFeatureIDText.getText());
			}
		});
	}

	private void infoModified() {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();

		if (fLicenseFeatureIDText.getText().length() == 0 && fLicenseFeatureVersionText.getText().length() > 0) {
			fIgnoreChange = true;
			fLicenseFeatureVersionText.setText(""); //$NON-NLS-1$
			fIgnoreChange = false;
		}
		if (!fIgnoreChange && featureModel instanceof IEditable) {
			((IEditable) featureModel).setDirty(true);
			markDirty();
		}
	}

	@Override
	public void refresh() {
		fIgnoreChange = true;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fLicenseFeatureIDText.setText(feature.getLicenseFeatureID());
		fLicenseFeatureVersionText.setText(feature.getLicenseFeatureVersion());
		IFeatureInfo info = feature.getFeatureInfo(IFeature.INFO_LICENSE);
		String url = null;
		String description = null;
		if (info != null) {
			url = info.getURL();
			description = info.getDescription();
		}
		fUrlText.setText(url != null ? url : ""); //$NON-NLS-1$
		fDocument.set(description != null ? description : ""); //$NON-NLS-1$
		super.refresh();
		fIgnoreChange = false;
	}

	@Override
	public void commit(boolean onSave) {
		IFeatureModel featureModel = (IFeatureModel) getPage().getModel();
		IFeature feature = featureModel.getFeature();

		if (fSharedLicenseButton.getSelection()) {
			feature.setLicenseFeatureID(fLicenseFeatureIDText.getText());
			feature.setLicenseFeatureVersion(fLicenseFeatureVersionText.getText());
		} else {
			feature.setLicenseFeatureID(""); //$NON-NLS-1$
			feature.setLicenseFeatureVersion(""); //$NON-NLS-1$
			fIgnoreChange = true;
			fLicenseFeatureIDText.setText(""); //$NON-NLS-1$
			fLicenseFeatureVersionText.setText(""); //$NON-NLS-1$
			fIgnoreChange = false;
		}

		String url = fUrlText.getText();
		String description = fDocument.get();

		try {
			IFeatureInfo targetInfo = feature.getFeatureInfo(2);
			if (targetInfo == null) {
				targetInfo = featureModel.getFactory().createInfo(2);
				feature.setFeatureInfo(targetInfo, 2);
			}
			targetInfo.setURL(url);
			targetInfo.setDescription(description);
		} catch (CoreException e) {
		}
		super.commit(onSave);
	}

	@Override
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
		} else if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fSourceViewer.doOperation(ITextOperationTarget.SELECT_ALL);
		}
		return false;
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		return fSourceViewer.canDoOperation(ITextOperationTarget.PASTE);
	}

	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
}
