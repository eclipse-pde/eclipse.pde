/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;

/**
 * Feature page.
 */
public class FeatureFormPage extends PDEFormPage implements IHyperlinkListener {
	public static final String PAGE_ID = "feature"; //$NON-NLS-1$

	private FeatureSpecSection fSpecSection;

	private PortabilitySection fPortabilitySection;

	public FeatureFormPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_OVERVIEW;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.getBody().setLayout(FormLayoutFactory.createFormTableWrapLayout(true, 2));
		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FTR_XML_OBJ));

		TableWrapData twd;

		Composite left = toolkit.createComposite(form.getBody());
		left.setLayout(FormLayoutFactory.createFormPaneTableWrapLayout(false, 1));

		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		left.setLayoutData(twd);

		Composite right = toolkit.createComposite(form.getBody());
		right.setLayout(FormLayoutFactory.createFormPaneTableWrapLayout(false, 1));

		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		right.setLayoutData(twd);

		fSpecSection = new FeatureSpecSection(this, left);
		fPortabilitySection = new PortabilitySection(this, left);
		twd = new TableWrapData();
		twd.grabHorizontal = true;
		fPortabilitySection.getSection().setLayoutData(twd);

		createContentSection(managedForm, right, toolkit);
		createPackagingSection(managedForm, right, toolkit);

		managedForm.addPart(fSpecSection);
		managedForm.addPart(fPortabilitySection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_OVERVIEW);
		initialize();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getModel();
		IFeature feature = model.getFeature();
		getManagedForm().getForm().setText(model.getResourceString(feature.getLabel()));
	}

	private Section createContentSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		FormText text;
		section.setText(PDEUIMessages.FeatureEditor_InfoPage_ContentSection_title);
		text = createClient(section, PDEUIMessages.FeatureEditor_InfoPage_ContentSection_text, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, //$NON-NLS-1$
				SharedLabelProvider.F_EDIT));
		return section;
	}

	private Section createPackagingSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section.setText(PDEUIMessages.FeatureEditor_InfoPage_PackagingSection_title);
		// ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		// toolkit.adapt(info, true, true);
		// Image image =
		// PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP);
		// info.setImage(image);
		// info.addHyperlinkListener(new HyperlinkAdapter() {
		// public void linkActivated(HyperlinkEvent e) {
		// }
		// });
		// info.setBackground(section.getTitleBarGradientBackground());
		// section.setTextClient(info);
		createClient(section, PDEUIMessages.FeatureEditor_InfoPage_PackagingSection_text, toolkit);
		return section;
	}


	private FormText createClient(Section section, String content, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		section.setClient(text);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		text.addHyperlinkListener(this);
		return text;
	}

	private Section createStaticSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);
		// toolkit.createCompositeSeparator(section);
		return section;
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		// try page references
		switch (href) {
		case "info": //$NON-NLS-1$
			getEditor().setActivePage(InfoFormPage.PAGE_ID);
			break;
		case "plugins": //$NON-NLS-1$
			getEditor().setActivePage(FeatureReferencePage.PAGE_ID);
			break;
		case "features": //$NON-NLS-1$
			getEditor().setActivePage(FeatureIncludesPage.PAGE_ID);
			break;
		case "dependencies": //$NON-NLS-1$
			getEditor().setActivePage(FeatureDependenciesPage.PAGE_ID);
			break;
		case "build": //$NON-NLS-1$
			getEditor().setActivePage(BuildPage.PAGE_ID);
			break;
		case "synchronize": //$NON-NLS-1$
			getEditor().setActivePage(FeatureReferencePage.PAGE_ID);
			BusyIndicator.showWhile(e.display,
					() -> ((FeatureEditorContributor) getPDEEditor().getContributor()).getSynchronizeAction().run());
			break;
		case "export": //$NON-NLS-1$
			((FeatureEditor) getPDEEditor()).getFeatureExportAction().run();
			break;
		case "siteProject": //$NON-NLS-1$
			getEditor().doSave(null);
			BusyIndicator.showWhile(e.display,
					() -> ((FeatureEditorContributor) getPDEEditor().getContributor()).getNewSiteAction().run());
			break;
		default:
			break;
		}
	}

	@Override
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);
	}
}
