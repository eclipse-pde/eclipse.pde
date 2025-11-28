/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Hannes Wellmann - Bug 325614 - Support mixed products (features and bundles)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.pde.internal.core.iproduct.IProduct.ProductType.BUNDLES;
import static org.eclipse.pde.internal.core.iproduct.IProduct.ProductType.FEATURES;
import static org.eclipse.pde.internal.core.iproduct.IProduct.ProductType.MIXED;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProduct.ProductType;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.launcher.LaunchAction;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.wizards.product.ProductDefinitionWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class ProductInfoSection extends PDESection implements IRegistryChangeListener, IStateDeltaListener {

	private ExtensionIdComboPart fAppCombo;
	private ExtensionIdComboPart fProductCombo;
	private Button fPluginButton;
	private Button fFeatureButton;
	private Button fMixedButton;

	private static final int NUM_COLUMNS = 3;

	class ExtensionIdComboPart extends ComboPart implements SelectionListener {
		private String fRemovedId;

		@Override
		public void createControl(Composite parent, FormToolkit toolkit, int style) {
			super.createControl(parent, toolkit, style);
			addSelectionListener(this);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (getSelectionIndex() != getItemCount() - 1) {
				fRemovedId = null;
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}

		private void addItem(String item, int index) {
			int selection = getSelectionIndex();
			super.add(item, index);
			if (item.equals(fRemovedId)) {
				select(index);
				fRemovedId = null;
			} else if (selection >= index) {
				select(selection + 1);
			}
		}

		private void removeItem(int index) {
			int selection = getSelectionIndex();
			if (index == selection) {
				fRemovedId = getSelection();
				select(getItemCount() - 2);
			}
			super.remove(index);
			if (selection > index) {
				select(selection - 1);
			}
		}

		public void handleExtensionDelta(IExtensionDelta[] deltas) {
			for (IExtensionDelta delta : deltas) {
				IExtension extension = delta.getExtension();
				if (extension == null) {
					return;
				}
				String id = extension.getUniqueIdentifier();
				if (id == null) {
					continue;
				}
				if (delta.getKind() == IExtensionDelta.ADDED) {
					int index = computeIndex(id);
					// index of -1 means id is already in combo
					if (index >= 0) {
						addItem(id, index);
					}
				} else {
					int index = indexOf(id);
					if (index >= 0) {
						removeItem(index);
					}
				}
			}
		}

		private int computeIndex(String newId) {
			// Easy linear search to compute the index to insert. If this take
			// too much time (ie. long list) suggest binary search
			int i = 0;
			String[] entries = getItems();
			// go to entries.length -1 because last entry in both ComboParts is
			// a ""
			for (; i < entries.length - 1; i++) {
				int compareValue = entries[i].compareTo(newId);
				if (compareValue == 0) {
					return -1;
				} else if (compareValue > 0) {
					break;
				}
			}
			return i;
		}

		public void reload(String[] newItems) {
			if (fRemovedId == null) {
				fRemovedId = getSelection();
			}
			setItems(newItems);
			int index = indexOf(fRemovedId);
			if (index > 0) {
				select(index);
				fRemovedId = null;
			}
		}

	}

	public ProductInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ProductInfoSection_title);
		section.setDescription(PDEUIMessages.ProductInfoSection_desc);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		data.colspan = 2;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, NUM_COLUMNS));

		// product section
		createProductEntry(client, toolkit);
		createApplicationEntry(client, toolkit);
		createConfigurationOption(client, toolkit);

		toolkit.paintBordersFor(client);
		section.setClient(client);

		getModel().addModelChangedListener(this);
		PDECore.getDefault().getExtensionsRegistry().addListener(this);
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		PDECore.getDefault().getExtensionsRegistry().removeListener(this);
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
		super.dispose();
	}

	private void createProductEntry(Composite client, FormToolkit toolkit) {
		Label label = toolkit.createLabel(client, PDEUIMessages.ProductInfoSection_product);
		label.setToolTipText(PDEUIMessages.ProductInfoSection_productTooltip);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fProductCombo = new ExtensionIdComboPart();
		fProductCombo.createControl(client, toolkit, SWT.READ_ONLY);
		fProductCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProductCombo.setItems(TargetPlatform.getProducts());
		fProductCombo.add(""); //$NON-NLS-1$
		fProductCombo.addSelectionListener(
				widgetSelectedAdapter(e -> getProduct().setProductId(fProductCombo.getSelection())));
		fProductCombo.getControl().addListener(SWT.MouseWheel, event -> {
			// Cancel the event to prevent default scrolling
			event.doit = false;
		});
		Button button = toolkit.createButton(client, PDEUIMessages.ProductInfoSection_new, SWT.PUSH);
		button.setEnabled(isEditable());
		button.addSelectionListener(widgetSelectedAdapter(e -> handleNewDefinition()));
		fProductCombo.getControl().setEnabled(isEditable());
	}

	private void handleNewDefinition() {
		ProductDefinitionWizard wizard = new ProductDefinitionWizard(getProduct());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		if (dialog.open() == Window.OK) {
			String id = wizard.getProductId();
			IProduct product = getProduct();
			product.setProductId(id);
			product.setApplication(wizard.getApplication());
		}
	}

	private void createApplicationEntry(Composite client, FormToolkit toolkit) {
		Label label = toolkit.createLabel(client, PDEUIMessages.ProductInfoSection_app, SWT.WRAP);
		label.setToolTipText(PDEUIMessages.ProductInfoSection_appTooltip);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		IProduct products = getProduct();
		Set<IPluginModelBase> models = null;
		try {
			models = LaunchAction.getLaunchedBundlesForProduct(products);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (models == null) {
			models = java.util.Collections.emptySet(); // <-- tiny null guard
		}

		Set<String> productBundleSymbolicNames = new HashSet<>();
		for (IPluginModelBase model : models) {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null && desc.getSymbolicName() != null) {
				String symbolicName = desc.getSymbolicName();
				productBundleSymbolicNames.add(symbolicName);
			}
		}
		fAppCombo = new ExtensionIdComboPart();
		fAppCombo.createControl(client, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = NUM_COLUMNS - 1;
		fAppCombo.getControl().setLayoutData(gd);
		Set<String> filteredApps = TargetPlatformHelper.getApplicationNameSet(productBundleSymbolicNames);
		fAppCombo.setItems(filteredApps.toArray(new String[0]));
		fAppCombo.add(""); //$NON-NLS-1$
		fAppCombo.addSelectionListener(
				widgetSelectedAdapter(e -> getProduct().setApplication(fAppCombo.getSelection())));
		fAppCombo.getControl().addListener(SWT.MouseWheel, event -> {
			// Cancel the event to prevent default scrolling
			event.doit = false;
		});
		fAppCombo.getControl().setEnabled(isEditable());
	}

	private void createConfigurationOption(Composite client, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(client);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		comp.setLayoutData(gd);

		FormText text = toolkit.createFormText(comp, true);
		text.setText(PDEUIMessages.Product_overview_configuration, true, true);
		text.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
				getStatusLineManager().setMessage(e.getLabel());
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				getStatusLineManager().setMessage(null);
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String pageId = DependenciesPage.TYPE_2_ID.get(getProduct().getType());
				getPage().getEditor().setActivePage(pageId);
			}
		});

		fPluginButton = createTypeButton(toolkit, comp, BUNDLES, PDEUIMessages.ProductInfoSection_plugins);
		fFeatureButton = createTypeButton(toolkit, comp, FEATURES, PDEUIMessages.ProductInfoSection_features);
		fMixedButton = createTypeButton(toolkit, comp, MIXED, PDEUIMessages.ProductInfoSection_mixed);
	}

	private Button createTypeButton(FormToolkit toolkit, Composite comp, ProductType type, String label) {
		Button btn = toolkit.createButton(comp, label, SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		btn.setLayoutData(gd);
		btn.setEnabled(isEditable());
		btn.addSelectionListener(widgetSelectedAdapter(e -> {
			IProduct product = getProduct();
			if (btn.getSelection() && product.getType() != type) {
				product.setType(type); // set to changed type
				((ProductEditor) getPage().getEditor()).updateConfigurationPage();
			}
		}));
		return btn;
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	@Override
	public void refresh() {
		IProduct product = getProduct();
		if (product.getProductId() != null) {
			refreshProductCombo(product.getProductId());
		}
		if (product.getApplication() != null) {
			fAppCombo.setText(product.getApplication());
		}
		fPluginButton.setSelection(product.getType() == BUNDLES);
		fFeatureButton.setSelection(product.getType() == FEATURES);
		fMixedButton.setSelection(product.getType() == MIXED);
		super.refresh();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged();
			return;
		}

		String prop = e.getChangedProperty();
		if (prop == null) {
			return;
		}
		if (prop.equals(IProduct.P_ID)) {
			refreshProductCombo(e.getNewValue().toString());
		} else if (prop.equals(IProduct.P_APPLICATION)) {
			fAppCombo.setText(e.getNewValue().toString());
		}
	}

	private void handleModelEventWorldChanged() {
		// Store selection before refresh
		List<Boolean> previousSelection = getSelections();
		refresh();
		// Revert the configuration page if necessary
		revertConfigurationPage(previousSelection);
	}

	private void revertConfigurationPage(List<Boolean> previousSelection) {
		// Compare selection from before and after the refresh
		if (previousSelection.equals(getSelections())) {
			// No update required
			return;
		}
		// The configuration page needs to be updated
		IFormPage currentPage = getPage().getEditor().getActivePageInstance();
		// If the current page is the configuration page, switch to the
		// overview page before doing the update; otherwise, widget disposed
		// errors may result
		if (currentPage instanceof DependenciesPage) {
			getPage().getEditor().setActivePage(OverviewPage.PAGE_ID);
		}
		((ProductEditor) getPage().getEditor()).updateConfigurationPage();
	}

	private List<Boolean> getSelections() {
		return List.of(fPluginButton.getSelection(), fFeatureButton.getSelection(), fMixedButton.getSelection());
	}

	private void refreshProductCombo(String productID) {
		if (productID == null) {
			productID = ""; //$NON-NLS-1$
		} else if (fProductCombo.indexOf(productID) == -1) {
			fProductCombo.add(productID, 0);
		}
		fProductCombo.setText(productID);
	}

	private IStatusLineManager getStatusLineManager() {
		IEditorSite site = getPage().getEditor().getEditorSite();
		return site.getActionBars().getStatusLineManager();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		return c instanceof Text;
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		final IExtensionDelta[] applicationDeltas = event.getExtensionDeltas(IPDEBuildConstants.BUNDLE_CORE_RUNTIME,
				"applications"); //$NON-NLS-1$
		final IExtensionDelta[] productDeltas = event.getExtensionDeltas(IPDEBuildConstants.BUNDLE_CORE_RUNTIME,
				"products"); //$NON-NLS-1$
		if (applicationDeltas.length + productDeltas.length == 0) {
			return;
		}
		Display.getDefault().asyncExec(() -> {
			fAppCombo.handleExtensionDelta(applicationDeltas);
			fProductCombo.handleExtensionDelta(productDeltas);
		});
	}

	@Override
	public void stateChanged(State newState) {
		String[] products = TargetPlatform.getProducts();
		final String[] finalProducts = new String[products.length + 1];
		System.arraycopy(products, 0, finalProducts, 0, products.length);
		finalProducts[products.length] = ""; //$NON-NLS-1$

		String[] apps = TargetPlatform.getApplications();
		final String[] finalApps = new String[apps.length + 1];
		System.arraycopy(apps, 0, finalApps, 0, apps.length);
		finalApps[apps.length] = ""; //$NON-NLS-1$

		Display.getDefault().asyncExec(() -> {
			fAppCombo.reload(finalApps);
			fProductCombo.reload(finalProducts);
		});
	}

	@Override
	public void stateResolved(StateDelta delta) {
		// do nothing
	}

}
