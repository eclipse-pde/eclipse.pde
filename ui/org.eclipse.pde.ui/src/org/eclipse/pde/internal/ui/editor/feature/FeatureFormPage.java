/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Feature page.
 */
public class FeatureFormPage extends PDEFormPage implements IHyperlinkListener {
	public static final String PAGE_ID = "feature"; //$NON-NLS-1$

	private static final String fContentText = PDEPlugin
			.getResourceString("FeatureEditor.InfoPage.ContentSection.text"); //$NON-NLS-1$

	private static final String fPackagingText = PDEPlugin
			.getResourceString("FeatureEditor.InfoPage.PackagingSection.text"); //$NON-NLS-1$

	private static final String fPublishingText = PDEPlugin
			.getResourceString("FeatureEditor.InfoPage.PublishingSection.text"); //$NON-NLS-1$

	private FeatureSpecSection fSpecSection;

	private PortabilitySection fPortabilitySection;

	public FeatureFormPage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		TableWrapLayout layout = new TableWrapLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.topMargin = 5;
		layout.bottomMargin = 5;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 15;
		TableWrapData twd;

		Composite left = toolkit.createComposite(form.getBody());
		TableWrapLayout tableLayout = new TableWrapLayout();
		tableLayout.bottomMargin = 0;
		tableLayout.topMargin = 0;
		tableLayout.leftMargin = 0;
		tableLayout.rightMargin = 0;
		tableLayout.verticalSpacing = 15;
		tableLayout.horizontalSpacing = 15;
		left.setLayout(tableLayout);

		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		left.setLayoutData(twd);

		Composite right = toolkit.createComposite(form.getBody());
		tableLayout = new TableWrapLayout();
		tableLayout.bottomMargin = 0;
		tableLayout.topMargin = 0;
		tableLayout.leftMargin = 0;
		tableLayout.rightMargin = 0;
		tableLayout.verticalSpacing = 15;
		tableLayout.horizontalSpacing = 15;
		right.setLayout(tableLayout);

		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		right.setLayoutData(twd);

		fSpecSection = new FeatureSpecSection(this, left);
		twd = new TableWrapData();
		twd.grabHorizontal = true;
		fSpecSection.getSection().setLayoutData(twd);

		fPortabilitySection = new PortabilitySection(this, left);
		twd = new TableWrapData();
		twd.grabHorizontal = true;
		fPortabilitySection.getSection().setLayoutData(twd);

		createContentSection(managedForm, right, toolkit);
		createPackagingSection(managedForm, right, toolkit);
		createPublishingSection(managedForm, right, toolkit);

		managedForm.addPart(fSpecSection);
		managedForm.addPart(fPortabilitySection);

		WorkbenchHelp.setHelp(form.getBody(),
				IHelpContextIds.MANIFEST_FEATURE_OVERVIEW);
		initialize();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getModel();
		IFeature feature = model.getFeature();
		getManagedForm().getForm().setText(
				model.getResourceString(feature.getLabel()));
	}

	private Section createContentSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		FormText text;
		section
				.setText(PDEPlugin
						.getResourceString("FeatureEditor.InfoPage.ContentSection.title")); //$NON-NLS-1$
		text = createClient(section, fContentText, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, //$NON-NLS-1$
				SharedLabelProvider.F_EDIT));
		return section;
	}

	private Section createPackagingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section
				.setText(PDEPlugin
						.getResourceString("FeatureEditor.InfoPage.PackagingSection.title")); //$NON-NLS-1$
		// ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		// toolkit.adapt(info, true, true);
		// Image image =
		// PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP);
		// info.setImage(image);
		// info.addHyperlinkListener(new HyperlinkAdapter() {
		// public void linkActivated(HyperlinkEvent e) {
		// WorkbenchHelp
		// .displayHelpResource(PDEPlugin.getResourceString("OverviewPage.help.deploy"));
		// //$NON-NLS-1$
		// }
		// });
		// info.setBackground(section.getTitleBarGradientBackground());
		// section.setTextClient(info);
		createClient(section, fPackagingText, toolkit);
		return section;
	}

	private Section createPublishingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section
				.setText(PDEPlugin
						.getResourceString("FeatureEditor.InfoPage.PublishingSection.title")); //$NON-NLS-1$
		// ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		// toolkit.adapt(info, true, true);
		// Image image =
		// PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP);
		// info.setImage(image);
		// info.addHyperlinkListener(new HyperlinkAdapter() {
		// public void linkActivated(HyperlinkEvent e) {
		// WorkbenchHelp
		// .displayHelpResource(PDEPlugin.getResourceString("OverviewPage.help.deploy"));
		// //$NON-NLS-1$
		// }
		// });
		// info.setBackground(section.getTitleBarGradientBackground());
		// section.setTextClient(info);
		createClient(section, fPublishingText, toolkit);
		return section;
	}

	private FormText createClient(Section section, String content,
			FormToolkit toolkit) {
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
		Section section = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		// toolkit.createCompositeSeparator(section);
		return section;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		// try page references
		if (href.equals("info")) //$NON-NLS-1$
			getEditor().setActivePage(InfoFormPage.PAGE_ID);
		if (href.equals("plugins")) //$NON-NLS-1$
			getEditor().setActivePage(FeatureReferencePage.PAGE_ID);
		else if (href.equals("features")) //$NON-NLS-1$
			getEditor().setActivePage(FeatureIncludesPage.PAGE_ID);
		else if (href.equals("dependencies")) //$NON-NLS-1$
			getEditor().setActivePage(FeatureDependenciesPage.PAGE_ID);
		else if (href.equals("installHandler")) //$NON-NLS-1$
			getEditor().setActivePage(FeatureAdvancedPage.PAGE_ID);
		else if (href.equals("build")) //$NON-NLS-1$
			getEditor().setActivePage(BuildPage.PAGE_ID);
		else if (href.equals("synchronize")) { //$NON-NLS-1$ {
			getEditor().setActivePage(FeatureReferencePage.PAGE_ID);
			final FeatureEditorContributor contributor = (FeatureEditorContributor) getPDEEditor()
					.getContributor();
			BusyIndicator.showWhile(e.display, new Runnable() {
				public void run() {
					contributor.getSynchronizeAction().run();
				}
			});
		} else if (href.equals("export")) { //$NON-NLS-1$
			getEditor().doSave(null);
			final FeatureEditorContributor contributor = (FeatureEditorContributor) getPDEEditor()
					.getContributor();
			BusyIndicator.showWhile(e.display, new Runnable() {
				public void run() {
					contributor.getBuildAction().run();
				}
			});
		} else if (href.equals("siteProject")) { //$NON-NLS-1$
			getEditor().doSave(null);
			final FeatureEditorContributor contributor = (FeatureEditorContributor) getPDEEditor()
					.getContributor();
			BusyIndicator.showWhile(e.display, new Runnable() {
				public void run() {
					contributor.getNewSiteAction().run();
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars()
				.getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars()
				.getStatusLineManager();
		mng.setMessage(null);
	}
}
