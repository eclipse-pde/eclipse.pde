/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.launcher.LaunchAction;
import org.eclipse.pde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.progress.IProgressService;


public class OverviewPage extends PDEFormPage implements IHyperlinkListener {
	
	public static final String PAGE_ID = "overview"; //$NON-NLS-1$

	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.OverviewPage_title); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.OverviewPage_title);  
		fillBody(managedForm, toolkit);
		managedForm.refresh();
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.OVERVIEW_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 5;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth =true;
		layout.verticalSpacing = 30;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);

		// sections
		managedForm.addPart(new ProductInfoSection(this, body));
		if (getModel().isEditable()) {
			createTestingSection(body, toolkit);
			createExportingSection(body, toolkit);
		}
	}
	
	private void createTestingSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.setText(PDEUIMessages.Product_OverviewPage_testing); 
		FormText text = createClient(section, PDEUIMessages.Product_overview_testing, toolkit); 
		text.setImage("run", getImage(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", getImage(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		text.addHyperlinkListener(this);
		section.setClient(text);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
	}
	
	private void createExportingSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.setText(PDEUIMessages.OverviewPage_exportingTitle); 
		FormText text = createClient(section, PDEUIMessages.Product_overview_exporting, toolkit); 
		text.addHyperlinkListener(this);
		section.setClient(text);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));		
	}
	
	private FormText createClient(Section section, String content,
			FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText("", false, false); //$NON-NLS-1$
		}
		section.setClient(text);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		return text;
	}
	
	private Image getImage(ImageDescriptor desc) {
		return getImage(desc, 0);
	}
	
	private Image getImage(ImageDescriptor desc, int overlay) {
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		return lp.get(desc, overlay);
	}

	public void linkEntered(HyperlinkEvent e) {
		getStatusLineManager().setMessage(e.getLabel());
	}

	public void linkExited(HyperlinkEvent e) {
		getStatusLineManager().setMessage(null);
	}

	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if (href.equals("action.debug")) { //$NON-NLS-1$
			handleSynchronize(false);
			new LaunchAction(getProduct(), getFilePath(), ILaunchManager.DEBUG_MODE).run();
		} else if (href.equals("action.run")) { //$NON-NLS-1$
			handleSynchronize(false);
			new LaunchAction(getProduct(), getFilePath(), ILaunchManager.RUN_MODE).run();
		} else if (href.equals("action.synchronize")) { //$NON-NLS-1$
			handleSynchronize(true);
		} else if (href.equals("action.export")) { //$NON-NLS-1$
			if (getPDEEditor().isDirty())
				getPDEEditor().doSave(null);
			new ProductExportAction(getPDEEditor()).run();
		} else if (href.equals("configuration")) { //$NON-NLS-1$
			String pageId = getProduct().useFeatures() ? ConfigurationPage.FEATURE_ID : ConfigurationPage.PLUGIN_ID;
			getEditor().setActivePage(pageId);
			
		}
	}
	
	private String getFilePath() {
		Object file = getEditorInput().getAdapter(IFile.class);
		if (file != null)
			return ((IFile)file).getFullPath().toString();
		file = getEditorInput().getAdapter(File.class);
		if (file != null)
			return ((File)file).getAbsolutePath();
		return getProduct().getId();
	}
	
	private void handleSynchronize(boolean alert) {
		try {
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			SynchronizationOperation op = new SynchronizationOperation(getProduct(), getSite().getShell());
			service.runInUI(service, op, PDEPlugin.getWorkspace().getRoot());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {		
			if (alert) MessageDialog.openError(getSite().getShell(), "Synchronize", e.getTargetException().getMessage()); //$NON-NLS-1$
		}
	}
	
	private IProduct getProduct() {
		IBaseModel model = getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
	}

	private IStatusLineManager getStatusLineManager() {
		IEditorSite site = getEditor().getEditorSite();
		return site.getActionBars().getStatusLineManager();
	}
	
}
