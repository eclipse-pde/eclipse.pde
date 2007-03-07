/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.service.prefs.BackingStoreException;


public class OverviewPage extends PDEFormPage implements IHyperlinkListener {
	public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	private PluginExportAction fExportAction;
	private GeneralInfoSection fInfoSection;
	private boolean fDisposed = false;

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.OverviewPage_tabName);  
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IPDEUIConstants.PLUGIN_DOC_ROOT + "guide/tools/editors/manifest_editor/overview.htm"; //$NON-NLS-1$
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		if (isFragment()) {
			form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_MF_OBJ));
		} else {
			form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_MF_OBJ));
		}
		form.setText(PDEUIMessages.ManifestEditor_OverviewPage_title); 
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_OVERVIEW);
	}
	
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormTableWrapLayout(true, 2));

		Composite left = toolkit.createComposite(body);
		left.setLayout(FormLayoutFactory.createFormPaneTableWrapLayout(false, 1));
		left.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		if (isFragment())
			fInfoSection = new FragmentGeneralInfoSection(this, left);
		else
			fInfoSection = new PluginGeneralInfoSection(this, left);
		managedForm.addPart(fInfoSection);		
		if (isBundle())
			managedForm.addPart(new ExecutionEnvironmentSection(this, left));
			
		Composite right = toolkit.createComposite(body);			
		right.setLayout(FormLayoutFactory.createFormPaneTableWrapLayout(false, 1));
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

		Composite container = createStaticSectionClient(toolkit, section);
		
		FormText text = createClient(container, isFragment() ? PDEUIMessages.OverviewPage_fContent : PDEUIMessages.OverviewPage_content, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, SharedLabelProvider.F_EDIT)); //$NON-NLS-1$
		
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
	}
	
	private void createExtensionSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		String sectionTitle = PDEUIMessages.ManifestEditor_ExtensionSection_title;
		Section section = createStaticSection(
							toolkit, 
							parent, 
							sectionTitle);

		Composite container = createStaticSectionClient(toolkit, section);
		
		FormText text  = createClient(container, isFragment() ? PDEUIMessages.OverviewPage_fExtensionContent : PDEUIMessages.OverviewPage_extensionContent, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, SharedLabelProvider.F_EDIT)); //$NON-NLS-1$
		
		section.setClient(container);
	}
	
	private void createTestingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEUIMessages.ManifestEditor_TestingSection_title); 
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		
		Composite container = createStaticSectionClient(toolkit, section);
		
		FormText text = createClient(container, getLauncherText(!((ManifestEditor)getEditor()).showExtensionTabs()), toolkit);
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		text.setImage("profile", lp.get(PDEPluginImages.DESC_PROFILE_EXC)); //$NON-NLS-1$
		section.setClient(container);
	}
	
	private void createExportingSection(IManagedForm managedForm,
			Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEUIMessages.ManifestEditor_DeployingSection_title); 
		Composite container = createStaticSectionClient(toolkit, section);
		createClient(container, isFragment() ? PDEUIMessages.OverviewPage_fDeploying : PDEUIMessages.OverviewPage_deploying, toolkit);
		section.setClient(container);
	}
	
	private Section createStaticSection(FormToolkit toolkit, Composite parent, String text) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(text);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);
		return section;
	}
	
	/**
	 * @param toolkit
	 * @param parent
	 * @return
	 */
	private Composite createStaticSectionClient(FormToolkit toolkit, 
			Composite parent) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		container.setLayoutData(data);
		return container;
	}
	
	private FormText createClient(Composite section, String content, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
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
		} else if (href.equals("export")) { //$NON-NLS-1$
			getExportAction().run();
		} else if (href.equals("action.convert")) { //$NON-NLS-1$
			handleConvert();
		} else if (href.equals("organize")) { //$NON-NLS-1$
			getEditor().doSave(null);
			OrganizeManifestsAction organizeAction = new OrganizeManifestsAction();
			organizeAction.selectionChanged(null, new StructuredSelection(getPDEEditor().getCommonProject()));
			organizeAction.run(null);
		} else if (href.startsWith("launchShortcut.")) { //$NON-NLS-1$
			href = href.substring(15);
			int index = href.indexOf('.');
			if (index < 0)
				return;  // error.  Format of href should be launchShortcut.<mode>.<launchShortcutId>
			String mode = href.substring(0, index);
			String id = href.substring(index + 1); 
			getEditor().doSave(null);
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.debug.ui.launchShortcuts"); //$NON-NLS-1$
			for (int i = 0; i < elements.length; i++) {
				if (id.equals(elements[i].getAttribute("id"))) //$NON-NLS-1$
					try {
						ILaunchShortcut shortcut = (ILaunchShortcut)elements[i].createExecutableExtension("class"); //$NON-NLS-1$
						shortcut.launch(new StructuredSelection(getPDEEditor().getCommonProject()), mode);
					} catch (CoreException e1) {
					}
			}
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
	
	private String getLauncherText(boolean osgi) {
		IConfigurationElement[] elements = getLaunchers(osgi);
		
		StringBuffer buffer = new StringBuffer(osgi ? PDEUIMessages.OverviewPage_OSGiTesting : 
			isFragment() ? PDEUIMessages.OverviewPage_fTesting : PDEUIMessages.OverviewPage_testing);
		
		for (int i = 0; i < elements.length; i++) {
			String mode = elements[i].getAttribute("mode"); //$NON-NLS-1$
			buffer.append("<li style=\"image\" value=\""); //$NON-NLS-1$
			buffer.append(mode);
			buffer.append("\" bindent=\"5\"><a href=\"launchShortcut."); //$NON-NLS-1$
			buffer.append(mode);
			buffer.append('.');
			buffer.append(elements[i].getAttribute("id")); //$NON-NLS-1$
			buffer.append("\">"); //$NON-NLS-1$
			buffer.append(elements[i].getAttribute("label")); //$NON-NLS-1$
			buffer.append("</a></li>"); //$NON-NLS-1$
		}
		buffer.append("</form>"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	private IConfigurationElement[] getLaunchers(boolean osgi) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.launchShortcuts"); //$NON-NLS-1$
		// validate elements
		ArrayList list = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			String mode = elements[i].getAttribute("mode"); //$NON-NLS-1$
			if (mode != null && (mode.equals(ILaunchManager.RUN_MODE) || mode.equals(ILaunchManager.DEBUG_MODE) || mode.equals(ILaunchManager.PROFILE_MODE)) 
					&& elements[i].getAttribute("label") != null && elements[i].getAttribute("id") != null &&  //$NON-NLS-1$ //$NON-NLS-2$
					osgi == "true".equals(elements[i].getAttribute("osgi"))) //$NON-NLS-1$ //$NON-NLS-2$
				list.add(elements[i]);
		}
		
		// sort elements based on criteria specified in bug 172703
		elements = (IConfigurationElement[])list.toArray(new IConfigurationElement[list.size()]);
		Arrays.sort(elements, new Comparator() {

			public int compare(Object arg0, Object arg1) {
				int mode1 = getModeValue(((IConfigurationElement)arg0).getAttribute("mode")); //$NON-NLS-1$
				int mode2 = getModeValue(((IConfigurationElement)arg1).getAttribute("mode")); //$NON-NLS-1$
				if (mode1 != mode2)
					return mode1 - mode2;
				String label1 = ((IConfigurationElement)arg0).getAttribute("label"); //$NON-NLS-1$
				String label2 = ((IConfigurationElement)arg1).getAttribute("label"); //$NON-NLS-1$
				return label1.compareTo(label2);
			}
			
			private int getModeValue(String value) {
				if (value.equals(ILaunchManager.RUN_MODE))
					return 0;
				else if (value.equals(ILaunchManager.DEBUG_MODE))
					return 1;
				return 2; // has to be ILaunchManager.PROFILE_MODE
			}
			
		});
		return elements;
	}
	
	private PluginExportAction getExportAction() {
		if (fExportAction == null)
			fExportAction = new PluginExportAction((PDEFormEditor) getEditor());
		return fExportAction;
	}
	
	private void handleConvert() {
		try {
			// remove listeners of Info section before we convert.  If we don't 
			// we may get a model changed event while disposing the page.  Bug 156414
			fInfoSection.removeListeners();
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
			// if convert failed and this OverviewPage hasn't been removed from the editor, reattach listeners
			if (!fDisposed)
				fInfoSection.addListeners();
		} catch (InterruptedException e) {
			// if convert failed and this OverviewPage hasn't been removed from the editor, reattach listeners
			if (!fDisposed)
				fInfoSection.addListeners();
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
        if (mdiag.open() != Window.OK)
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
    
    public void dispose() {
    	fDisposed = true;
    	super.dispose();
    }
}
