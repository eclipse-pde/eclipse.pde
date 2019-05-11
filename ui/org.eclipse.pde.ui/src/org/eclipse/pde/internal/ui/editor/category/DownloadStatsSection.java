/*******************************************************************************
 * Copyright (c) 2014, 2019 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 547155
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Version;

public class DownloadStatsSection extends TableSection {

	private FormEntry fURLEntry;
	private TableViewer fArtifactTable;
	private ISiteModel fModel;

	class ArtifactsContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			// model = (IStatsInfo) inputElement;
			ArrayList<IWritable> result = new ArrayList<>();
			IStatsInfo info = (IStatsInfo) inputElement;
			ISiteFeature[] features = info.getFeatureArtifacts();
			for (ISiteFeature feature : features) {
				result.add(new SiteFeatureAdapter(null, feature));
			}
			ISiteBundle[] bundles = info.getBundleArtifacts();
			for (ISiteBundle bundle : bundles) {
				result.add(new SiteBundleAdapter(null, bundle));
			}
			return result.toArray();
		}
	}

	public DownloadStatsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, getButtonLabels());
		createClient(getSection(), page.getEditor().getToolkit());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[4];
		labels[0] = PDEUIMessages.StatsSection_addFeature;
		labels[1] = PDEUIMessages.StatsSection_addBundle;
		labels[2] = PDEUIMessages.StatsSection_remove;
		labels[3] = PDEUIMessages.StatsSection_removeAll;
		return labels;
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		fModel = (ISiteModel) getPage().getModel();
		fModel.addModelChangedListener(this);

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		// Create and configure client
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite urlContainer = toolkit.createComposite(client);
		urlContainer.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		urlContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Create form entry
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fURLEntry = new FormEntry(urlContainer, toolkit, PDEUIMessages.StatsSection_url, SWT.NONE);
		fURLEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				try {
					ensureStatsInfo().setURL(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}

		});
		boolean editable = isEditable();
		fURLEntry.setEditable(editable);

		Composite container = createClientContainer(client, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		TablePart tablePart = getTablePart();
		fArtifactTable = tablePart.getTableViewer();
		fArtifactTable.setContentProvider(new ArtifactsContentProvider());
		fArtifactTable.setLabelProvider(new CategoryLabelProvider());
		fArtifactTable.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				if (element instanceof SiteFeatureAdapter)
					return 0;
				return 1;
			}
		});
		data = (GridData) tablePart.getControl().getLayoutData();
		data.minimumWidth = 200;
		resetArtifactTable();
		tablePart.setButtonEnabled(0, editable);
		tablePart.setButtonEnabled(1, editable);
		tablePart.setButtonEnabled(2, editable);
		tablePart.setButtonEnabled(3, editable);

		toolkit.paintBordersFor(container);

		section.setClient(client);
		section.setText(PDEUIMessages.StatsSection_title);
		section.setDescription(PDEUIMessages.StatsSection_description);
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNewFeature();
				break;
			case 1 :
				handleNewBundle();
				break;
			case 2 :
				handleRemove();
				break;
			case 3 :
				handleRemoveAll();
				break;

		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = fArtifactTable.getStructuredSelection();
		Iterator<?> iterator = ssel.iterator();
		while (iterator.hasNext()) {
			Object object = iterator.next();
			if (object == null)
				continue;
			if (object instanceof SiteFeatureAdapter) {
				handleRemoveSiteFeatureAdapter((SiteFeatureAdapter) object);
			} else if (object instanceof SiteBundleAdapter) {
				handleRemoveSiteBundleAdapter((SiteBundleAdapter) object);
			}
		}
	}

	private boolean handleRemoveSiteFeatureAdapter(SiteFeatureAdapter adapter) {
		try {
			ISiteFeature feature = adapter.feature;
			ensureStatsInfo().removeFeatureArtifacts(new ISiteFeature[] { feature });
			return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private boolean handleRemoveSiteBundleAdapter(SiteBundleAdapter adapter) {
		try {
			ISiteBundle bundle = adapter.bundle;
			ensureStatsInfo().removeBundleArtifacts(new ISiteBundle[] { bundle });
			return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private void handleRemoveAll() {
		try {
			IStatsInfo statsInfo = ensureStatsInfo();
			statsInfo.removeBundleArtifacts(statsInfo.getBundleArtifacts());
			statsInfo.removeFeatureArtifacts(statsInfo.getFeatureArtifacts());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		fArtifactTable.refresh(false);
		updateButtons();
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection ssel) {
		super.handleDoubleClick(ssel);
		Object selected = ssel.getFirstElement();
		if (selected instanceof SiteFeatureAdapter) {
			IFeature feature = findFeature(((SiteFeatureAdapter) selected).feature);
			FeatureEditor.openFeatureEditor(feature);
		} else if (selected instanceof SiteBundleAdapter) {
			ManifestEditor.openPluginEditor(((SiteBundleAdapter) selected).bundle.getId());
		}
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleRemove();
			return false;
		}
		return super.doGlobalAction(actionId);
	}


	/**
	 * Finds a feature with the same id and version as a site feature. If
	 * feature is not found, but feature with a M.m.s.qualifier exists it will
	 * be returned.
	 *
	 * @param siteFeature
	 * @return IFeature or null
	 */
	private IFeature findFeature(ISiteFeature siteFeature) {
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(siteFeature.getId(), siteFeature.getVersion());
		if (model != null)
			return model.getFeature();
		return null;
	}

	private void handleNewFeature() {
		final Control control = fArtifactTable.getControl();
		BusyIndicator.showWhile(control.getDisplay(), () -> {
			IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
			ArrayList<IFeatureModel> newModels = new ArrayList<>();
			for (IFeatureModel allModel : allModels) {
				if (canAdd(allModel))
					newModels.add(allModel);
			}
			IFeatureModel[] candidateModels = newModels.toArray(new IFeatureModel[newModels.size()]);
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(fArtifactTable.getTable().getShell(), candidateModels, true);
			if (dialog.open() == Window.OK) {
				Object[] models = dialog.getResult();
				try {
					doAddFeatures(models);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		});
	}

	private void handleNewBundle() {
		final Control control = fArtifactTable.getControl();
		BusyIndicator.showWhile(control.getDisplay(), () -> {
			IPluginModelBase[] allModels = PluginRegistry.getAllModels();
			ArrayList<IPluginModelBase> newModels = new ArrayList<>();
			for (IPluginModelBase allModel : allModels) {
				if (canAdd(allModel))
					newModels.add(allModel);
			}
			IPluginModelBase[] candidateModels = newModels.toArray(new IPluginModelBase[newModels.size()]);
			PluginSelectionDialog dialog = new PluginSelectionDialog(fArtifactTable.getTable().getShell(), candidateModels, true);
			if (dialog.open() == Window.OK) {
				Object[] models = dialog.getResult();
				try {
					doAddBundles(models);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		});
	}

	private boolean canAdd(IFeatureModel candidate) {
		ISiteFeature[] features = fModel.getSite().getStatsInfo().getFeatureArtifacts();
		IFeature cfeature = candidate.getFeature();

		for (ISiteFeature bfeature : features) {
			if (bfeature.getId().equals(cfeature.getId()) && bfeature.getVersion().equals(cfeature.getVersion()))
				return false;
		}
		return true;
	}

	private boolean canAdd(IPluginModelBase candidate) {
		ISiteBundle[] currentBundles = fModel.getSite().getStatsInfo().getBundleArtifacts();
		IPluginBase candidateBundle = candidate.getPluginBase();

		for (ISiteBundle currentBundle : currentBundles) {
			if (currentBundle.getId().equals(candidateBundle.getId()) && currentBundle.getVersion().equals(candidateBundle.getVersion()))
				return false;
		}
		return true;
	}

	private ISiteFeature createSiteFeature(ISiteModel model, IFeatureModel featureModel) throws CoreException {
		IFeature feature = featureModel.getFeature();
		ISiteFeature sfeature = model.getFactory().createFeature();
		sfeature.setId(feature.getId());
		sfeature.setVersion(feature.getVersion());
		// sfeature.setURL(model.getBuildModel().getSiteBuild().getFeatureLocation()
		// + "/" + feature.getId() + "_" + feature.getVersion() + ".jar");
		// //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sfeature.setURL("features/" + feature.getId() + "_" + formatVersion(feature.getVersion()) + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sfeature.setOS(feature.getOS());
		sfeature.setWS(feature.getWS());
		sfeature.setArch(feature.getArch());
		sfeature.setNL(feature.getNL());
		sfeature.setIsPatch(isFeaturePatch(feature));
		return sfeature;
	}

	private ISiteBundle createSiteBundle(ISiteModel model, IPluginModelBase candidate) throws CoreException {
		ISiteBundle newBundle = model.getFactory().createBundle();
		newBundle.setId(candidate.getPluginBase().getId());
		newBundle.setVersion(candidate.getPluginBase().getVersion());
		return newBundle;
	}

	private String formatVersion(String version) {
		try {
			Version v = new Version(version);
			return v.toString();
		} catch (IllegalArgumentException e) {
		}
		return version;
	}

	private boolean isFeaturePatch(IFeature feature) {
		IFeatureImport[] imports = feature.getImports();
		for (IFeatureImport import1 : imports) {
			if (import1.isPatch())
				return true;
		}
		return false;
	}

	/**
	 * @param candidates  Array of IFeatureModel
	 */
	public void doAddFeatures(Object[] candidates) throws CoreException {
		ISiteFeature[] added = new ISiteFeature[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			ISiteFeature child = createSiteFeature(fModel, candidate);
			added[i] = child;
		}

		// Update model
		ensureStatsInfo().addFeatureArtifacts(added);
		// Select last added feature
		if (added.length > 0) {
			fArtifactTable.setSelection(new StructuredSelection(new SiteFeatureAdapter(null, added[added.length - 1])), true);
		}
	}

	/**
	 * @param candidates  Array of IPluginModelBase
	 */
	public void doAddBundles(Object[] candidates) throws CoreException {
		ISiteBundle[] added = new ISiteBundle[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates[i];
			ISiteBundle child = createSiteBundle(fModel, candidate);
			added[i] = child;
		}

		// Update model
		ensureStatsInfo().addBundleArtifacts(added);
		// Select last added bundle
		if (added.length > 0) {
			fArtifactTable.setSelection(new StructuredSelection(new SiteBundleAdapter(null, added[added.length - 1])), true);
		}
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();

	}

	@Override
	public void refresh() {
		IStatsInfo info = getSite().getStatsInfo();
		if (info != null) {
			fURLEntry.setValue(info.getURL(), true);
		} else {
			fURLEntry.setValue(null, true);
		}
		fArtifactTable.refresh();
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		fURLEntry.commit();
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fURLEntry.cancelEdit();
		super.cancelEdit();
	}

	private IStatsInfo ensureStatsInfo() {
		IStatsInfo info = getSite().getStatsInfo();
		if (info == null) {
			info = getModel().getFactory().createStatsInfo();
			try {
				getSite().setStatsInfo(info);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
		return info;
	}

	public ISiteModel getModel() {
		return fModel;
	}

	public ISite getSite() {
		return fModel.getSite();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		resetArtifactTable();
		refresh();
		updateButtons();
	}

	private void resetArtifactTable() {
		IStatsInfo statsInfo = getSite().getStatsInfo();
		if (statsInfo != null) {
			fArtifactTable.setInput(statsInfo);
		}
	}

	@Override
	public void dispose() {
		ISiteModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	private void updateButtons() {
		TablePart tablePart = getTablePart();
		ISelection selection = getViewerSelection();
		boolean editable = isEditable();
		tablePart.setButtonEnabled(2, editable && !selection.isEmpty() && selection instanceof IStructuredSelection);
		boolean hasInfo = getSite().getStatsInfo() != null;
		tablePart.setButtonEnabled(3, editable && hasInfo && (ensureStatsInfo().getFeatureArtifacts().length > 0
				|| ensureStatsInfo().getBundleArtifacts().length > 0));
	}

}
