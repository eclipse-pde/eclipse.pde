/*******************************************************************************
 * Copyright (c) 2019 Ed Scadding.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import org.eclipse.pde.internal.ui.views.features.action.CollapseAllAction;
import org.eclipse.pde.internal.ui.views.features.action.ContentProviderAction;
import org.eclipse.pde.internal.ui.views.features.action.FeatureAndPluginCopyAction;
import org.eclipse.pde.internal.ui.views.features.action.FilterFeatureChildAction;
import org.eclipse.pde.internal.ui.views.features.action.ShowCalleesContentProviderAction;
import org.eclipse.pde.internal.ui.views.features.action.ShowCallersContentProviderAction;
import org.eclipse.pde.internal.ui.views.features.action.ShowPluginsAction;
import org.eclipse.pde.internal.ui.views.features.action.ShowProductsAction;
import org.eclipse.pde.internal.ui.views.features.action.ViewerFilterAction;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;
import org.eclipse.pde.internal.ui.views.features.viewer.DeferredFeaturesViewInput;
import org.eclipse.pde.internal.ui.views.features.viewer.FeatureElementComparer;
import org.eclipse.pde.internal.ui.views.features.viewer.FeatureViewerComparator;
import org.eclipse.pde.internal.ui.views.features.viewer.RootElementsFilteredTree;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;

public class FeaturesView extends ViewPart {

	private final Action fOpenAction = new Action(PDEUIMessages.FeaturesView_OpenAction_label) {
		@Override
		public void run() {
			handleOpen();
		}
	};

	private final Collection<ViewerFilter> fViewerFilters = new ArrayList<>();

	private PatternFilter fPatternFilter;

	private ViewerFilterAction fFilterFeatureChildAction;

	private ViewerFilterAction fShowPluginsAction;

	private ViewerFilterAction fShowProductsAction;

	private TreeViewer fViewer;

	private Clipboard fClipboard;

	private Action fCopyAction;

	private FeaturesViewInput fInput;

	@Override
	public void createPartControl(Composite parent) {
		fInput = new FeaturesViewInput();

		FilteredTree filteredTree = createFilteredTree(parent);
		fViewer = filteredTree.getViewer();
		fPatternFilter = filteredTree.getPatternFilter();

		fClipboard = new Clipboard(parent.getDisplay());
		fCopyAction = new FeatureAndPluginCopyAction(fViewer, fClipboard, fInput);

		registerGlobalActions();
		contributeToActionBar();
		hookContextMenu();

		initializeViewer();
	}

	@Override
	public void setFocus() {
		fViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		fInput.dispose();
		fClipboard.dispose();
	}

	private FilteredTree createFilteredTree(Composite parent) {
		FilteredTree filteredTree = new RootElementsFilteredTree(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		filteredTree.setInitialText(PDEUIMessages.FeaturesView_searchPlaceholder);

		PatternFilter patternFilter = filteredTree.getPatternFilter();
		patternFilter.setIncludeLeadingWildcard(true);

		TreeViewer viewer = filteredTree.getViewer();
		viewer.setExpandPreCheckFilters(true);
		viewer.setComparer(new FeatureElementComparer());
		viewer.setLabelProvider(new PDELabelProvider());

		viewer.addDoubleClickListener(event -> handleOpen());
		viewer.addSelectionChangedListener(event -> {
			Collection<?> selection = getViewerSelection();
			fOpenAction.setEnabled(!selection.isEmpty());
			fCopyAction.setEnabled(!selection.isEmpty());
		});

		return filteredTree;
	}

	private void initializeViewer() {
		resetViewerFilters();
		fViewer.setComparator(new FeatureViewerComparator(fInput));
		fViewer.setInput(new DeferredFeaturesViewInput(fInput));
	}

	private void resetViewerFilters() {
		fViewerFilters.clear();

		fViewerFilters.add(fPatternFilter);
		addViewerFilter(fViewerFilters, fFilterFeatureChildAction);
		addViewerFilter(fViewerFilters, fShowPluginsAction);
		addViewerFilter(fViewerFilters, fShowProductsAction);

		fViewer.setFilters(fViewerFilters.toArray(new ViewerFilter[fViewerFilters.size()]));
	}

	private void addViewerFilter(Collection<ViewerFilter> viewerFilters, ViewerFilterAction viewerFilterAction) {
		if (viewerFilterAction.isEnabled() && !viewerFilterAction.isChecked()) {
			viewerFilters.add(viewerFilterAction.getViewerFilter());
		}
	}

	public void toggle(ViewerFilter filter) {
		if (fViewerFilters.contains(filter)) {
			fViewerFilters.remove(filter);
		} else {
			fViewerFilters.add(filter);
		}
		resetViewerFilters();
	}

	public boolean isActive(ViewerFilter filter) {
		return fViewerFilters.contains(filter);
	}

	private void setContentProvider(IContentProvider contentProvider, boolean supportsFeatureChildFilter,
			boolean supportsPlugins, boolean supportsProducts) {
		fViewer.setContentProvider(contentProvider);

		fFilterFeatureChildAction.setEnabled(supportsFeatureChildFilter);
		fShowPluginsAction.setEnabled(supportsPlugins);
		fShowProductsAction.setEnabled(supportsProducts);

		resetViewerFilters();
	}

	public void configureContent(Consumer<FeaturesViewInput> configurator) {
		DeferredFeaturesViewInput deferredInput = (DeferredFeaturesViewInput) fViewer.getInput();
		configurator.accept(deferredInput.getFeaturesViewInput());
		fViewer.refresh();
	}

	private void registerGlobalActions() {
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
	}

	private void contributeToActionBar() {
		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();

		toolBarManager.add(new CollapseAllAction(fViewer));
		toolBarManager.add(new Separator());

		ContentProviderAction calleesAction = new ShowCalleesContentProviderAction(this, fInput);
		calleesAction.setChecked(true);
		toolBarManager.add(calleesAction);

		ContentProviderAction callersAction = new ShowCallersContentProviderAction(this, fInput);
		toolBarManager.add(callersAction);
		toolBarManager.add(new Separator());

		fFilterFeatureChildAction = new FilterFeatureChildAction(this);
		fFilterFeatureChildAction.setChecked(true);
		toolBarManager.add(fFilterFeatureChildAction);

		fShowPluginsAction = new ShowPluginsAction(this, fInput);
		toolBarManager.add(fShowPluginsAction);
		fShowProductsAction = new ShowProductsAction(this, fInput);
		toolBarManager.add(fShowProductsAction);

		setContentProvider(calleesAction);
		setContentProvider(callersAction);

		actionBars.updateActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(this::fillContextMenu);
		Menu menu = menuManager.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(fOpenAction);

		IPluginModelBase selectedPluginModel = getSelectedPluginModel();
		if (selectedPluginModel != null) {
			Action dependenciesAction = new OpenPluginDependenciesAction(selectedPluginModel);
			manager.add(dependenciesAction);
		}

		manager.add(new Separator());
		manager.add(fCopyAction);
	}

	public void setContentProvider(ContentProviderAction contentProviderAction) {
		if (contentProviderAction.isChecked()) {
			setContentProvider(contentProviderAction.createContentProvider(), contentProviderAction.isSupportsFeatureChildFilter(),
					contentProviderAction.isSupportsPlugins(), contentProviderAction.isSupportsProducts());
		}
	}

	private void handleOpen() {
		for (Object selection : getViewerSelection()) {
			IFeatureModel featureModel = fInput.getFeatureSupport().toFeatureModel(selection);
			if (featureModel != null) {
				FeatureEditor.openFeatureEditor(featureModel);
				continue;
			}

			IPluginModelBase pluginModel = fInput.getPluginSupport().toPluginModel(selection);
			if (pluginModel != null) {
				ManifestEditor.openPluginEditor(pluginModel);
				continue;
			}

			IProductModel productModel = fInput.getProductSupport().toProductModel(selection);
			if (productModel != null) {
				fInput.getProductSupport().openProductEditor(productModel);
			}
		}
	}

	private Collection<?> getViewerSelection() {
		return fViewer.getStructuredSelection().toList();
	}

	private IPluginModelBase getSelectedPluginModel() {
		return fInput.getPluginSupport().toSinglePluginModel(fViewer.getStructuredSelection());
	}

}
