/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.progress.IProgressService;


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

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString("OverviewPage.tabName"));  //$NON-NLS-1$
	}
	
	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/pde_manifest_overview.htm"; //$NON-NLS-1$
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEPlugin.getResourceString("ManifestEditor.OverviewPage.title")); //$NON-NLS-1$
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

		// sections
		GeneralInfoSection general = null;
		if (isFragment())
			general = new FragmentGeneralInfoSection(this, body);
		else
			general = new PluginGeneralInfoSection(this, body);
		managedForm.addPart(general);
		createContentSection(managedForm, body, toolkit);
		createTestingSection(managedForm, body, toolkit);
		createExportingSection(managedForm, body, toolkit);
	}
	
	private void createContentSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(
							toolkit, 
							parent, 
							PDEPlugin.getResourceString("ManifestEditor.ContentSection." + (isFragment() ? "ftitle" : "title")));

		Composite container = toolkit.createComposite(section, SWT.NONE);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = layout.topMargin = layout.bottomMargin = 0;
		container.setLayout(layout);
		container.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		FormText text = createClient(container, isFragment() ? fcontentText : contentText, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, PDELabelProvider.F_EDIT));
		
		if (!isBundle() && isEditable())
			text = createClient(container, PDEPlugin.getResourceString("OverviewPage." + (isFragment() ? "fOsgi" : "osgi")), toolkit);
		section.setClient(container);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}
	
	private void createTestingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEPlugin.getResourceString("ManifestEditor.TestingSection.title"));
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		info.setImage(lp.get(PDEPluginImages.DESC_HELP));
		info.setBackground(section.getTitleBarGradientBackground());
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				displayHelpResource("/org.eclipse.pde.doc.user/guide/pde_running.htm"); //$NON-NLS-1$
			}
		});
		section.setTextClient(info);
		
		FormText text = createClient(section, isFragment() ? ftestingText : testingText, toolkit);
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		section.setClient(text);
	}
	
	private void createExportingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEPlugin.getResourceString("ManifestEditor.DeployingSection.title"));
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		info.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP));
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				displayHelpResource("/org.eclipse.pde.doc.user/guide/pde_deploy.htm"); //$NON-NLS-1$
			}
		});
		info.setBackground(section.getTitleBarGradientBackground());
		section.setTextClient(info);
		section.setClient(createClient(section, isFragment() ? fdeployingText : deployingText, toolkit));
	}
	
	private Section createStaticSection(FormToolkit toolkit, Composite parent, String text) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.setText(text);
		return section;
	}
	
	private FormText createClient(Composite section, String content, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		text.addHyperlinkListener(this);
		return text;
	}
	
	private boolean isFragment() {
		IPluginModelBase model = (IPluginModelBase) getPDEEditor().getContextManager().getAggregateModel();
		return model.isFragmentModel();
	}
	
	private boolean isBundle() {
		return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

	private boolean isEditable() {
		IPluginModelBase model = (IPluginModelBase) getPDEEditor().getContextManager().getAggregateModel();
		return model.isEditable();
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
		else if (href.equals("action.run")) { //$NON-NLS-1$ {
			getEditor().doSave(null);
			getLaunchShortcut().run((IPluginModelBase)getModel());
		} else if (href.equals("action.debug")) { //$NON-NLS-1$
			getEditor().doSave(null);
			getLaunchShortcut().debug((IPluginModelBase)getModel());
		} else if (href.equals("export")) { //$NON-NLS-1$
			getExportAction().run();
		} else if (href.equals("action.convert")) {
			handleConvert();
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
	
	private void displayHelpResource(String resource) {
		PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(resource);
	}
	
	private void handleConvert() {
		try {
			PDEFormEditor editor = getPDEEditor();
			IPluginModelBase model = (IPluginModelBase)editor.getAggregateModel();
			IRunnableWithProgress op = new CreateManifestOperation(model);
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			editor.doSave(null);
			service.runInUI(service, op, model.getUnderlyingResource().getProject());
		} catch (InvocationTargetException e) {
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), "Error", e.getCause().getMessage());
		} catch (InterruptedException e) {
		}
	}
	
}
