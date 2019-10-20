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
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import org.eclipse.pde.internal.ui.views.features.action.*;
import org.eclipse.pde.internal.ui.views.features.support.*;
import org.eclipse.pde.internal.ui.views.features.viewer.*;
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

	private final Collection<ViewerFilterAction> fViewerFilterActions = new ArrayList<>();

	private final Collection<ViewerFilter> fViewerFilters = new ArrayList<>();

	private PatternFilter fPatternFilter;

	private Action fShowPluginsAction;

	private TreeViewer fViewer;

	private Clipboard fClipboard;

	private Action fCopyAction;

	private FeatureIndex fFeatureIndex;

	@Override
	public void createPartControl(Composite parent) {
		FeatureModelManager featureModelManager = FeatureSupport.getManager();
		FeatureInput input = new FeatureInput(featureModelManager);
		fFeatureIndex = new FeatureIndex(featureModelManager);

		FilteredTree filteredTree = createFilteredTree(parent);
		fViewer = filteredTree.getViewer();
		fPatternFilter = filteredTree.getPatternFilter();
		fViewerFilters.add(fPatternFilter);

		fClipboard = new Clipboard(parent.getDisplay());
		fCopyAction = new FeatureAndPluginCopyAction(fViewer, fClipboard);

		registerGlobalActions();
		contributeToActionBar(featureModelManager);
		hookContextMenu();

		initializeViewer(input);
	}

	@Override
	public void setFocus() {
		fViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		fFeatureIndex.dispose();
		fClipboard.dispose();
	}

	private FilteredTree createFilteredTree(Composite parent) {
		FilteredTree filteredTree = new RootElementsFilteredTree(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		filteredTree.setInitialText(PDEUIMessages.FeaturesView_searchPlaceholder);

		PatternFilter patternFilter = filteredTree.getPatternFilter();
		patternFilter.setIncludeLeadingWildcard(true);

		TreeViewer viewer = filteredTree.getViewer();
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

	private void initializeViewer(FeatureInput input) {
		resetViewerFilters();
		fViewer.setComparator(new FeatureViewerComparator());
		fViewer.setInput(new DeferredFeatureInput(input));
	}

	private void resetViewerFilters() {
		fViewer.setFilters(fViewerFilters.toArray(new ViewerFilter[fViewerFilters.size()]));
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

	private void setContentProvider(IContentProvider contentProvider, boolean supportsFilters,
			boolean supportsPlugins) {
		fViewer.setContentProvider(contentProvider);

		setViewerFilterActionsEnabled(supportsFilters);
		fShowPluginsAction.setEnabled(supportsPlugins);

		if (supportsFilters) {
			resetViewerFilters();
		} else {
			fViewer.setFilters(new ViewerFilter[] { fPatternFilter });
		}
	}

	private void setViewerFilterActionsEnabled(boolean supportsFilters) {
		for (ViewerFilterAction viewerFilterAction : fViewerFilterActions) {
			viewerFilterAction.setEnabled(supportsFilters);
		}
	}

	public void configureContent(Consumer<FeatureInput> configurator) {
		DeferredFeatureInput deferredFeatureInput = (DeferredFeatureInput) fViewer.getInput();
		configurator.accept(deferredFeatureInput.getFeatureInput());
		fViewer.refresh();
	}

	private void registerGlobalActions() {
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
	}

	private void contributeToActionBar(FeatureModelManager featureModelManager) {
		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();

		toolBarManager.add(new CollapseAllAction(fViewer));
		toolBarManager.add(new Separator());

		ContentProviderAction calleesAction = new ShowCalleesContentProviderAction(this, featureModelManager);
		calleesAction.setChecked(true);
		toolBarManager.add(calleesAction);

		ContentProviderAction callersAction = new ShowCallersContentProviderAction(this, featureModelManager,
				fFeatureIndex);
		toolBarManager.add(callersAction);
		toolBarManager.add(new Separator());

		ViewerFilterAction filterFeatureChildAction = new FilterFeatureChildAction(this, fFeatureIndex);
		filterFeatureChildAction.setChecked(true);
		toolBarManager.add(filterFeatureChildAction);

		fShowPluginsAction = new ShowPluginsAction(this);
		toolBarManager.add(fShowPluginsAction);

		setContentProvider(calleesAction);
		setContentProvider(callersAction);
		registerFilterAction(filterFeatureChildAction);

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

	private void registerFilterAction(ViewerFilterAction filterAction) {
		fViewerFilterActions.add(filterAction);
		if (!filterAction.isChecked()) {
			fViewerFilters.add(filterAction.getViewerFilter());
		}
	}

	public void setContentProvider(ContentProviderAction contentProviderAction) {
		if (contentProviderAction.isChecked()) {
			setContentProvider(contentProviderAction.createContentProvider(), contentProviderAction.isSupportsFilters(),
					contentProviderAction.isSupportsPlugins());
		}
	}

	private void handleOpen() {
		for (Object selection : getViewerSelection()) {
			IFeatureModel featureModel = FeatureSupport.toFeatureModel(selection);
			if (featureModel != null) {
				FeatureEditor.openFeatureEditor(featureModel);
				continue;
			}

			IPluginModelBase pluginModel = PluginSupport.toPluginModel(selection);
			if (pluginModel != null) {
				ManifestEditor.openPluginEditor(pluginModel);
				continue;
			}
		}
	}

	private Collection<?> getViewerSelection() {
		return fViewer.getStructuredSelection().toList();
	}

	private IPluginModelBase getSelectedPluginModel() {
		return PluginSupport.toSinglePluginModel(fViewer.getStructuredSelection());
	}

}
