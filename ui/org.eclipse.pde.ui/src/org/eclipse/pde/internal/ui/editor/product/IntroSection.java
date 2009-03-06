/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.TreeSet;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.internal.ui.wizards.product.ProductIntroWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.editor.IFormPage;
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

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);

		section.setText(PDEUIMessages.IntroSection_sectionText);
		section.setDescription(PDEUIMessages.IntroSection_sectionDescription);

		boolean canCreateNew = TargetPlatformHelper.getTargetVersion() >= NEW_INTRO_SUPPORT_VERSION;

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, canCreateNew ? 3 : 2));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = toolkit.createLabel(client, PDEUIMessages.IntroSection_introLabel, SWT.WRAP);
		GridData td = new GridData();
		td.horizontalSpan = canCreateNew ? 3 : 2;
		label.setLayoutData(td);

		Label introLabel = toolkit.createLabel(client, PDEUIMessages.IntroSection_introInput);
		introLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fIntroCombo = new ComboPart();
		fIntroCombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new GridData(GridData.FILL_HORIZONTAL);
		fIntroCombo.getControl().setLayoutData(td);
		loadManifestAndIntroIds(false);
		if (fAvailableIntroIds != null)
			fIntroCombo.setItems(fAvailableIntroIds);
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
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	private void handleSelection() {
		if (!productDefined()) {
			fIntroCombo.setText(""); //$NON-NLS-1$
			return;
		}
		getIntroInfo().setId(fIntroCombo.getSelection());
		addDependenciesAndPlugins();
	}

	private void loadManifestAndIntroIds(boolean onlyLoadManifest) {
		TreeSet result = new TreeSet();
		String introId;
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions("org.eclipse.ui.intro", true); //$NON-NLS-1$
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] children = extensions[i].getConfigurationElements();
			for (int j = 0; j < children.length; j++) {
				if ("introProductBinding".equals(children[j].getName())) {//$NON-NLS-1$
					String attribute = children[j].getAttribute("productId"); //$NON-NLS-1$
					if (attribute != null && attribute.equals(getProduct().getProductId())) {
						if (fManifest == null) {
							IPluginModelBase base = PluginRegistry.findModel(extensions[i].getContributor().getName());
							if (base == null)
								continue;
							fManifest = (IFile) base.getUnderlyingResource();
						}
						if (onlyLoadManifest)
							return;
						introId = children[j].getAttribute("introId"); //$NON-NLS-1$
						if (introId != null)
							result.add(introId);
					}
				}
			}
		}
		fAvailableIntroIds = (String[]) result.toArray(new String[result.size()]);
	}

	private void handleNewIntro() {
		boolean needNewProduct = false;
		if (!productDefined()) {
			needNewProduct = true;
			MessageDialog mdiag = new MessageDialog(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.IntroSection_undefinedProductId, null, PDEUIMessages.IntroSection_undefinedProductIdMessage, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
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
			addDependenciesAndPlugins();
		}
	}

	public void refresh() {
		String introId = getIntroInfo().getId();
		if (introId == null) {
			fIntroCombo.setText(""); //$NON-NLS-1$
		} else {
			fIntroCombo.setText(introId);
		}
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
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	private boolean productDefined() {
		String id = getProduct().getProductId();
		return id != null && !id.equals(""); //$NON-NLS-1$
	}

	private void addDependenciesAndPlugins() {
		IProduct product = getProduct();
		if (!product.useFeatures()) {
			IProductModelFactory factory = product.getModel().getFactory();
			IProductPlugin plugin = factory.createPlugin();
			plugin.setId(INTRO_PLUGIN_ID);
			product.addPlugins(new IProductPlugin[] {plugin});
			boolean includeOptional = false;
			IFormPage page = getPage().getEditor().findPage(DependenciesPage.PLUGIN_ID);
			if (page != null)
				includeOptional = ((DependenciesPage) page).includeOptionalDependencies();
			PluginSection.handleAddRequired(new IProductPlugin[] {plugin}, includeOptional);
		}
		if (fManifest == null)
			loadManifestAndIntroIds(true);
		if (fManifest != null)
			addRequiredBundle();
	}

	private void addRequiredBundle() {
		ModelModification mod = new ModelModification(fManifest) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IBundlePluginModelBase))
					return;
				IBundlePluginModelBase modelBase = (IBundlePluginModelBase) model;
				IBundle bundle = modelBase.getBundleModel().getBundle();
				IManifestHeader header = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
				if (header instanceof RequireBundleHeader) {
					RequireBundleObject[] requires = ((RequireBundleHeader) header).getRequiredBundles();
					for (int i = 0; i < requires.length; i++)
						if (requires[i].getId().equals(INTRO_PLUGIN_ID))
							return;
					((RequireBundleHeader) header).addBundle(INTRO_PLUGIN_ID);
				} else
					bundle.setHeader(Constants.REQUIRE_BUNDLE, INTRO_PLUGIN_ID);
			}
		};
		PDEModelUtility.modifyModel(mod, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

}
