/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.iproduct.IIntroInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.internal.ui.wizards.product.ProductIntroWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;


public class IntroSection extends PDESection {

	private ComboPart fIntroCombo;
	private IFile fManifest;
	private String[] fAvailableIntroIds;
	private static final String INTRO_PLUGIN_ID = "org.eclipse.ui.intro"; //$NON-NLS-1$
	private static final double NEW_INTRO_SUPPORT_VERSION = 3.1;

	public IntroSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.IntroSection_sectionText); 
		section.setDescription(PDEUIMessages.IntroSection_sectionDescription); 
		
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		boolean canCreateNew = TargetPlatform.getTargetVersion() >= NEW_INTRO_SUPPORT_VERSION;
		layout.numColumns = canCreateNew ? 3 : 2;
		layout.marginHeight = 5;
		client.setLayout(layout);
		
		
		Label label = toolkit.createLabel(client, PDEUIMessages.IntroSection_introLabel, SWT.WRAP);
		GridData td = new GridData();
		td.horizontalSpan = canCreateNew ? 3 : 2;
		label.setLayoutData(td);
		
		Label introLabel = toolkit.createLabel(client, PDEUIMessages.IntroSection_introInput); 
		introLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fIntroCombo = new ComboPart();
		fIntroCombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new GridData(GridData.FILL_HORIZONTAL);
		fIntroCombo.getControl().setLayoutData(td);
		loadManifestAndIntroIds(false);
		if (fAvailableIntroIds != null ) fIntroCombo.setItems(fAvailableIntroIds);
		fIntroCombo.add(""); //$NON-NLS-1$
		fIntroCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelection();
			}
		});
		
		if (canCreateNew) {
			Button button = toolkit.createButton(client, PDEUIMessages.IntroSection_new, SWT.PUSH); 
			button.setEnabled(isEditable());
			button.setLayoutData(new GridData(GridData.FILL));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleNewIntro();
				}
			});	
		}
		
		fIntroCombo.getControl().setEnabled(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	private void handleSelection() {
		if (!productDefined()) {
			fIntroCombo.setText(""); //$NON-NLS-1$
			return;
		}
		getIntroInfo().setId(fIntroCombo.getSelection());
		try { addDependenciesAndPlugins(); } catch (CoreException e) {}
	}

	private void loadManifestAndIntroIds(boolean onlyLoadManifest) {
		TreeSet result = new TreeSet();
		String introId;
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null && point.equals("org.eclipse.ui.intro")) {//$NON-NLS-1$
					IPluginObject[] children = extensions[j].getChildren();
					for (int k = 0; k < children.length; k++) {
						IPluginElement element = (IPluginElement)children[k];
						if ("introProductBinding".equals(element.getName())) {//$NON-NLS-1$
							if (element.getAttribute("productId").getValue().equals(getProduct().getId())) { //$NON-NLS-1$
								if (fManifest == null)
									fManifest = (IFile)element.getPluginModel().getUnderlyingResource();
								if (onlyLoadManifest)
									return;
								introId = element.getAttribute("introId").getValue(); //$NON-NLS-1$
								if (introId != null)
									result.add(introId);
							}
						}
					}
				}
			}
		}
		fAvailableIntroIds = (String[])result.toArray(new String[result.size()]);
	}
	
	private void handleNewIntro() {
		boolean needNewProduct = false;
		if (!productDefined()) {
			needNewProduct = true;
			MessageDialog mdiag = new MessageDialog(PDEPlugin.getActiveWorkbenchShell(),
					PDEUIMessages.IntroSection_undefinedProductId, null, 
					PDEUIMessages.IntroSection_undefinedProductIdMessage,
					MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
	        if (mdiag.open() != Window.OK)
	        	return;
		}
		ProductIntroWizard wizard = new ProductIntroWizard(getProduct(), needNewProduct);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		if (dialog.open() == Window.OK) {
			String id = wizard.getIntroId();
			fIntroCombo.add(id, 0);
			fIntroCombo.setText(id);
			getIntroInfo().setId(id);
			try { addDependenciesAndPlugins(); } catch (CoreException e) {}
		}
	}

	public void refresh() {
		String introId = getIntroInfo().getId();
		if (introId != null) fIntroCombo.setText(introId);
		super.refresh();
	}
	
	private IIntroInfo getIntroInfo() {
		IIntroInfo info = getProduct().getIntroInfo();
		if (info == null) {
			info = getModel().getFactory().createIntroInfo();
			getProduct().setIntroInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	private boolean productDefined() {
		String id = getProduct().getId();
		return id != null && !id.equals(""); //$NON-NLS-1$
	}
	
	private void addDependenciesAndPlugins() throws CoreException {
		IProduct product = getProduct();
		if (!product.useFeatures()) {
			IProductModelFactory factory = product.getModel().getFactory();
			IProductPlugin plugin = factory.createPlugin();
			plugin.setId(INTRO_PLUGIN_ID, false);
			product.addPlugins(new IProductPlugin[] {plugin});
			PluginSection.handleAddRequired(new IProductPlugin[] {plugin});
		}
		if (fManifest == null) loadManifestAndIntroIds(true);
		if (fManifest != null) addRequiredBundle();
	}
	
	private void addRequiredBundle() throws CoreException {
		ModelModification mod = new ModelModification(fManifest) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IBundlePluginModelBase))
					return;
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase)model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				IManifestHeader header = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
				if (header instanceof RequireBundleHeader) {
					RequireBundleObject[] requires = ((RequireBundleHeader)header).getRequiredBundles();
					for (int i = 0; i < requires.length; i++)
						if (requires[i].getId().equals(INTRO_PLUGIN_ID))
							return;
					((RequireBundleHeader)header).addBundle(INTRO_PLUGIN_ID);
				} else
					bundle.setHeader(Constants.REQUIRE_BUNDLE, INTRO_PLUGIN_ID);
			}
		};
		PDEModelUtility.modifyModel(mod, null);
	}
	
}
