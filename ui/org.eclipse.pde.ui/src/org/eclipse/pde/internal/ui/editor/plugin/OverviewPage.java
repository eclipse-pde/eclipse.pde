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
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class OverviewPage extends PDEFormPage implements IHyperlinkListener {
	public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	private static final String contentText = PDEPlugin.getResourceString("OverviewPage.content"); //$NON-NLS-1$
	private static final String testingText = PDEPlugin.getResourceString("OverviewPage.testing"); //$NON-NLS-1$
	private static final String deployingText = PDEPlugin.getResourceString("OverviewPage.deploying"); //$NON-NLS-1$
	private static final String fcontentText = PDEPlugin.getResourceString("OverviewPage.fContent"); //$NON-NLS-1$
	private static final String ftestingText = PDEPlugin.getResourceString("OverviewPage.fTesting"); //$NON-NLS-1$
	private static final String fdeployingText = PDEPlugin.getResourceString("OverviewPage.fDeploying"); //$NON-NLS-1$

	private RuntimeWorkbenchShortcut fLaunchShortcut;
	private PluginExportAction fExportAction;
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, "Overview"); //$NON-NLS-1$
	}
	protected String getHelpResource() {
		return PDEPlugin.getResourceString("OverviewPage.help.manifest"); //$NON-NLS-1$
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEPlugin
				.getResourceString("ManifestEditor.OverviewPage.title")); //$NON-NLS-1$
		fillBody(managedForm, toolkit);
		managedForm.refresh();
	}
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 5;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.numColumns = 2;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		// alerts
		//createAlertSection(managedForm, body, toolkit);
		// sections
		createGeneralInfoSection(managedForm, body, toolkit);
		createContentSection(managedForm, body, toolkit);
		createTestingSection(managedForm, body, toolkit);
		createDeployingSection(managedForm, body, toolkit);
	}
	/*private void createAlertSection(IManagedForm managedForm, Composite parent,
			FormToolkit toolkit) {
		AlertSection section = new AlertSection(this, parent);
		managedForm.addPart(section);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 2;
		section.getSection().setLayoutData(td);
	}*/
	private void createGeneralInfoSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		managedForm.addPart(new GeneralInfoSection(this, parent));
	}
	private void createContentSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		FormText text;
		if (isFragment()) {
			section.setText(PDEPlugin
					.getResourceString("ManifestEditor.ContentSection.ftitle")); //$NON-NLS-1$
			text = createClient(section, fcontentText, toolkit);
		} else {
			section.setText(PDEPlugin
					.getResourceString("ManifestEditor.ContentSection.title")); //$NON-NLS-1$
			text = createClient(section, contentText, toolkit);
		}
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, //$NON-NLS-1$
				PDELabelProvider.F_EDIT));
	}
	private void createTestingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.TestingSection.title")); //$NON-NLS-1$
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		Image image = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP);
		info.setImage(image);
		info.setBackground(section.getTitleBarGradientBackground());
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				WorkbenchHelp
						.displayHelpResource(PDEPlugin.getResourceString("OverviewPage.help.pdeRunning")); //$NON-NLS-1$
			}
		});
		section.setTextClient(info);
		FormText text;
		if (isFragment())
			text = createClient(section, ftestingText, toolkit);
		else
			text = createClient(section, testingText, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		text.setImage("workbench", lp //$NON-NLS-1$
				.get(PDEPluginImages.DESC_WORKBENCH_LAUNCHER_WIZ));
	}
	private void createDeployingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.DeployingSection.title")); //$NON-NLS-1$
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		Image image = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP);
		info.setImage(image);
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				WorkbenchHelp
						.displayHelpResource(PDEPlugin.getResourceString("OverviewPage.help.deploy")); //$NON-NLS-1$
			}
		});
		info.setBackground(section.getTitleBarGradientBackground());
		section.setTextClient(info);
		if (isFragment())
			createClient(section, fdeployingText, toolkit);
		else
			createClient(section, deployingText, toolkit);
	}
	private Section createStaticSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		//toolkit.createCompositeSeparator(section);
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
	private boolean isFragment() {
		IPluginModelBase model = (IPluginModelBase) getPDEEditor()
				.getContextManager().getAggregateModel();
		return model.isFragmentModel();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		// try page references
		if (href.equals("dependencies")) //$NON-NLS-1$
			getEditor().setActivePage(DependenciesPage.PAGE_ID);
		else if (href.equals("runtime")) //$NON-NLS-1$
			getEditor().setActivePage(RuntimePage.PAGE_ID);
		else if (href.equals("extensions")) //$NON-NLS-1$
			getEditor().setActivePage(ExtensionsPage.PAGE_ID);
		else if (href.equals("ex-points")) //$NON-NLS-1$
			getEditor().setActivePage(ExtensionPointsPage.PAGE_ID);
		else if (href.equals("build")) //$NON-NLS-1$
			getEditor().setActivePage(BuildPage.PAGE_ID);
		else if (href.equals("action.run")) //$NON-NLS-1$
			getLaunchShortcut().run();
		else if (href.equals("action.debug")) //$NON-NLS-1$
			getLaunchShortcut().debug();
		else if (href.equals("export")) //$NON-NLS-1$
			getExportAction().run();
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
	private RuntimeWorkbenchShortcut getLaunchShortcut() {
		if (fLaunchShortcut == null)
			fLaunchShortcut = new RuntimeWorkbenchShortcut();
		return fLaunchShortcut;
	}
	private PluginExportAction getExportAction() {
		if (fExportAction == null)
			fExportAction = new PluginExportAction((PDEFormEditor) getEditor());
		return fExportAction;
	}
}
