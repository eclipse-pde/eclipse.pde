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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.launcher.EquinoxLaunchShortcut;
import org.eclipse.pde.internal.ui.launcher.RuntimeWorkbenchShortcut;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.service.prefs.BackingStoreException;


public class OverviewPage extends PDEFormPage implements IHyperlinkListener {
	public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	private RuntimeWorkbenchShortcut fLaunchShortcut;
	private EquinoxLaunchShortcut fEquinoxShortcut;
	private PluginExportAction fExportAction;

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.OverviewPage_tabName);  
	}
	
	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/pde_manifest_overview.htm"; //$NON-NLS-1$
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.ManifestEditor_OverviewPage_title); 
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_OVERVIEW);		
	}
	
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 5;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.numColumns = 2;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);

		Composite left = toolkit.createComposite(body);
		layout = new TableWrapLayout();
		layout.verticalSpacing = 20;
		left.setLayout(layout);
		left.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		GeneralInfoSection general = null;
		if (isFragment())
			general = new FragmentGeneralInfoSection(this, left);
		else
			general = new PluginGeneralInfoSection(this, left);
		managedForm.addPart(general);		
		if (isBundle())
			managedForm.addPart(new ExecutionEnvironmentSection(this, left));
			
		Composite right = toolkit.createComposite(body);			
		layout = new TableWrapLayout();
		layout.verticalSpacing = 20;
		right.setLayout(layout);
		right.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		createContentSection(managedForm, right, toolkit);
		if (isEditable() || getPDEEditor().hasInputContext(PluginInputContext.CONTEXT_ID))
			createExtensionSection(managedForm, right, toolkit);
		if (isEditable() )
    		createTestingSection(managedForm, isBundle() ? right : left, toolkit);
        if (isEditable())
    		createExportingSection(managedForm, right, toolkit);
	}
	
	private void createContentSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		String sectionTitle;
		if(isFragment()){
			sectionTitle = PDEUIMessages.ManifestEditor_ContentSection_ftitle;
		}else{
			sectionTitle = PDEUIMessages.ManifestEditor_ContentSection_title;
		}
		Section section = createStaticSection(
							toolkit, 
							parent, 
							sectionTitle);

		Composite container = toolkit.createComposite(section, SWT.NONE);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = layout.topMargin = layout.bottomMargin = 0;
		container.setLayout(layout);
		container.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		FormText text = createClient(container, isFragment() ? PDEUIMessages.OverviewPage_fContent : PDEUIMessages.OverviewPage_content, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, PDELabelProvider.F_EDIT)); //$NON-NLS-1$
		
		if (!isBundle() && isEditable()){
			String content;
			if(isFragment()){
				content = PDEUIMessages.OverviewPage_fOsgi;
			}else{
				content = PDEUIMessages.OverviewPage_osgi;
			}
			text = createClient(container, content, toolkit);
		}
		section.setClient(container);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}
	
	private void createExtensionSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		String sectionTitle = PDEUIMessages.ManifestEditor_ExtensionSection_title;
		Section section = createStaticSection(
							toolkit, 
							parent, 
							sectionTitle);

		Composite container = toolkit.createComposite(section, SWT.NONE);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = layout.topMargin = layout.bottomMargin = 0;
		container.setLayout(layout);
		container.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		FormText text  = createClient(container, isFragment() ? PDEUIMessages.OverviewPage_fExtensionContent : PDEUIMessages.OverviewPage_extensionContent, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, PDELabelProvider.F_EDIT)); //$NON-NLS-1$
		
		section.setClient(container);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}
	
	private void createTestingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEUIMessages.ManifestEditor_TestingSection_title); 
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		info.setImage(lp.get(PDEPluginImages.DESC_HELP));
		//no need for the background - transparency will take care of it
		info.setBackground(null);
		//info.setBackground(section.getTitleBarGradientBackground());
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				displayHelpResource("/org.eclipse.pde.doc.user/guide/pde_running.htm"); //$NON-NLS-1$
			}
		});
		section.setTextClient(info);
		
		FormText text;
		if (!((ManifestEditor)getEditor()).showExtensionTabs())
			text = createClient(section, PDEUIMessages.OverviewPage_OSGiTesting, toolkit);
		else
			text = createClient(section, isFragment() ? PDEUIMessages.OverviewPage_fTesting : PDEUIMessages.OverviewPage_testing, toolkit);
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		section.setClient(text);
	}
	
	private void createExportingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEUIMessages.ManifestEditor_DeployingSection_title); 
		ImageHyperlink info = new ImageHyperlink(section, SWT.NULL);
		toolkit.adapt(info, true, true);
		info.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_HELP));
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				displayHelpResource("/org.eclipse.pde.doc.user/guide/pde_deploy.htm"); //$NON-NLS-1$
			}
		});
		//info.setBackground(section.getTitleBarGradientBackground());
		//no need for the background - transparency will take care of it
		info.setBackground(null);
		section.setTextClient(info);
		section.setClient(createClient(section, isFragment() ? PDEUIMessages.OverviewPage_fDeploying : PDEUIMessages.OverviewPage_deploying, toolkit));
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
		else if (href.equals("extensions")) { //$NON-NLS-1$
			if (getEditor().setActivePage(ExtensionsPage.PAGE_ID) == null)
				activateExtensionPages(ExtensionsPage.PAGE_ID);
		} else if (href.equals("ex-points")) { //$NON-NLS-1$
			if (getEditor().setActivePage(ExtensionPointsPage.PAGE_ID) == null)
				activateExtensionPages(ExtensionPointsPage.PAGE_ID);
		} else if (href.equals("build")) { //$NON-NLS-1$
			if (!getPDEEditor().hasInputContext(BuildInputContext.CONTEXT_ID)) {
				if (!MessageDialog.openQuestion(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OverviewPage_buildTitle, PDEUIMessages.OverviewPage_buildQuestion)) 
					return;
				IFile file = getPDEEditor().getCommonProject().getFile("build.properties"); //$NON-NLS-1$
				WorkspaceBuildModel model = new WorkspaceBuildModel(file);
				model.save();
				IEditorInput in = new FileEditorInput(file);
				getPDEEditor().getContextManager().putContext(in, new BuildInputContext(getPDEEditor(), in, false));
			} 
			getEditor().setActivePage(BuildPage.PAGE_ID);	
		} else if (href.equals("action.run")) { //$NON-NLS-1$ {
			getEditor().doSave(null);
			getLaunchShortcut().run(getPDEEditor().getCommonProject());
		} else if (href.equals("action.debug")) { //$NON-NLS-1$
			getEditor().doSave(null);
			getLaunchShortcut().debug(getPDEEditor().getCommonProject());
		} else if (href.equals("export")) { //$NON-NLS-1$
			getExportAction().run();
		} else if (href.equals("action.convert")) { //$NON-NLS-1$
			handleConvert();
		} else if (href.equals("action.runEquinox")) { //$NON-NLS-1$ {
			getEditor().doSave(null);
			getEquinoxShortcut().run(getPDEEditor().getCommonProject());
		} else if (href.equals("action.debugEquinox")) { //$NON-NLS-1$
			getEditor().doSave(null);
			getEquinoxShortcut().debug(getPDEEditor().getCommonProject());
		} else if (href.equals("organize")) { //$NON-NLS-1$
			getEditor().doSave(null);
			OrganizeManifestsAction organizeAction = new OrganizeManifestsAction();
			organizeAction.selectionChanged(null, 
					new StructuredSelection(getPDEEditor().getCommonProject()));
			organizeAction.run(null);
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
	
	private EquinoxLaunchShortcut getEquinoxShortcut() {
		if (fEquinoxShortcut == null)
			fEquinoxShortcut = new EquinoxLaunchShortcut();
		return fEquinoxShortcut;
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
			service.runInUI(service, op, PDEPlugin.getWorkspace().getRoot());
            updateBuildProperties();
            editor.doSave(null);
		} catch (InvocationTargetException e) {
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OverviewPage_error, e.getCause().getMessage()); 
		} catch (InterruptedException e) {
		}
	}
    
    private void updateBuildProperties() throws InvocationTargetException {
       try {
        InputContext context = getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
        if (context != null) {
               IBuildModel buildModel = (IBuildModel)context.getModel();
               IBuild build = buildModel.getBuild();
               IBuildEntry entry = build.getEntry("bin.includes"); //$NON-NLS-1$
               if (entry == null) {
                   entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
                   build.add(entry);
               } 
               if (!entry.contains("META-INF")) //$NON-NLS-1$
                   entry.addToken("META-INF/");           //$NON-NLS-1$
           }
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        }
    }
    
    private void activateExtensionPages(String activePageId) {
    	MessageDialog mdiag = new MessageDialog(PDEPlugin.getActiveWorkbenchShell(),
				PDEUIMessages.OverviewPage_extensionPageMessageTitle, null, 
				PDEUIMessages.OverviewPage_extensionPageMessageBody,
				MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
        if (mdiag.open() != MessageDialog.OK)
        	return;
        try {
        	ManifestEditor manifestEditor = (ManifestEditor)getEditor();
        	manifestEditor.addExtensionTabs();
        	manifestEditor.setShowExtensions(true);
        	manifestEditor.setActivePage(activePageId);
		} catch (PartInitException e) {
		} catch (BackingStoreException e) {
		}
    }
}
