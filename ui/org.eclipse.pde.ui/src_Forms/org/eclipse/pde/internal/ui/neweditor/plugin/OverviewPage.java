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
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.build.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
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
	public static final String PAGE_ID = "overview";
	private static final String contentText = "<form><p>The content of the plug-in is made up of four sections:</p>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"dependencies\">Dependencies</a>: lists all the plug-ins required on this plug-in's classpath to compile and run.</li>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"runtime\">Runtime</a>: lists the libraries that make up this plug-in's runtime.</li>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"extensions\">Extensions</a>: declares contributions this plug-in makes to the platform.</li>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"ex-points\">Extension Points</a>: declares new function points this plug-in adds to the platform.</li>"
			+ "</form>";
	private static final String testingText = "<form>"
			+ "<p>You can test the plug-in by launching a second (runtime) instance of the workbench:</p>"
			+ "<li style=\"image\" value=\"run\" bindent=\"5\"><a href=\"action.run\">Launch a runtime workbench</a></li>"
			+ "<li style=\"image\" value=\"debug\" bindent=\"5\"><a href=\"action.debug\">Launch a runtime workbench in Debug mode</a></li>"
			+ "</form>";
	private static final String deployingText = "<form><p>To deploy the plug-in:</p>"
			+ "<li style=\"text\" value=\"1.\" bindent=\"5\">Specify what needs to be packaged in the deployable plug-in on the <a href=\"build\">Build Configuration</a> page</li>"
			+ "<li style=\"text\" value=\"2.\" bindent=\"5\">Export the plug-in in a format suitable for deployment using the <a href=\"export\">Export Wizard</a></li>"
			+ "</form>";
	private static final String fcontentText = "<form><p>The content of the fragment is made up of four sections:</p>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"dependencies\">Dependencies</a>: lists all the plug-ins required on this fragment's classpath to compile and run.</li>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"runtime\">Runtime</a>: lists the libraries that make up this fragment's runtime.</li>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"extensions\">Extensions</a>: declares contributions this fragment makes to the platform.</li>"
			+ "<li style=\"image\" value=\"page\" bindent=\"5\"><a href=\"ex-points\">Extension Points</a>: declares new function points this fragment adds to the platform.</li>"
			+ "</form>";
	private static final String ftestingText = "<form>"
			+ "<p>You can test the fragment by launching a second (runtime) instance of the workbench:</p>"
			+ "<li style=\"image\" value=\"run\" bindent=\"5\"><a href=\"action.run\">Launch a runtime workbench</a></li>"
			+ "<li style=\"image\" value=\"debug\" bindent=\"5\"><a href=\"action.debug\">Launch a runtime workbench in Debug mode</a></li>"
			+ "</form>";
	private static final String fdeployingText = "<form><p>To deploy the fragment:</p>"
			+ "<li style=\"text\" value=\"1.\" bindent=\"5\">Specify what needs to be packaged in the deployable fragment on the <a href=\"build\">Build Configuration</a> page</li>"
			+ "<li style=\"text\" value=\"2.\" bindent=\"5\">Export the fragment in a format suitable for deployment using the <a href=\"export\">Export Wizard</a></li>"
			+ "</form>";
	private RuntimeWorkbenchShortcut fLaunchShortcut;
	private PluginExportAction fExportAction;
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, "Overview");
	}
	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/pde_manifest_overview.htm";
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEPlugin
				.getResourceString("ManifestEditor.OverviewPage.title"));
		fillBody(managedForm, toolkit);
		managedForm.refresh();
	}
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 10;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		layout.numColumns = 2;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		// alerts
		createAlertSection(managedForm, body, toolkit);
		// sections
		createGeneralInfoSection(managedForm, body, toolkit);
		createContentSection(managedForm, body, toolkit);
		createTestingSection(managedForm, body, toolkit);
		createDeployingSection(managedForm, body, toolkit);
	}
	private void createAlertSection(IManagedForm managedForm, Composite parent,
			FormToolkit toolkit) {
		AlertSection section = new AlertSection(this, parent);
		managedForm.addPart(section);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 2;
		section.getSection().setLayoutData(td);
	}
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
					.getResourceString("ManifestEditor.ContentSection.ftitle"));
			text = createClient(section, fcontentText, toolkit);
		} else {
			section.setText(PDEPlugin
					.getResourceString("ManifestEditor.ContentSection.title"));
			text = createClient(section, contentText, toolkit);
		}
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ,
				PDELabelProvider.F_EDIT));
	}
	private void createTestingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.TestingSection.title"));
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		info.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJS_INFO_TSK));
		info.setBackground(section.getTitleBarGradientBackground());
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				WorkbenchHelp
						.displayHelpResource("/org.eclipse.pde.doc.user/guide/pde_running.htm");
			}
		});
		section.setTextClient(info);
		FormText text;
		if (isFragment())
			text = createClient(section, ftestingText, toolkit);
		else
			text = createClient(section, testingText, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC));
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC));
		text.setImage("workbench", lp
				.get(PDEPluginImages.DESC_WORKBENCH_LAUNCHER_WIZ));
	}
	private void createDeployingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.DeployingSection.title"));
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		info.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJS_INFO_TSK));
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				WorkbenchHelp
						.displayHelpResource("/org.eclipse.pde.doc.user/guide/pde_deploy.htm");
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
		if (href.equals("dependencies"))
			getEditor().setActivePage(DependenciesPage.PAGE_ID);
		else if (href.equals("runtime"))
			getEditor().setActivePage(RuntimePage.PAGE_ID);
		else if (href.equals("extensions"))
			getEditor().setActivePage(ExtensionsPage.PAGE_ID);
		else if (href.equals("ex-points"))
			getEditor().setActivePage(ExtensionPointsPage.PAGE_ID);
		else if (href.equals("build"))
			getEditor().setActivePage(BuildPage.PAGE_ID);
		else if (href.equals("action.run"))
			getLaunchShortcut().run();
		else if (href.equals("action.debug"))
			getLaunchShortcut().debug();
		else if (href.equals("export"))
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