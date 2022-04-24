/*******************************************************************************
 *  Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 230248
 *     Code 9 Corporation - onging enhancements
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 449348
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - Bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import java.io.File;
import java.text.BreakIterator;
import java.util.Comparator;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.StyledStringHighlighter;

public class FilteredPluginArtifactsSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.pde.ui.dialogs.FilteredPluginArtifactsSelectionDialog"; //$NON-NLS-1$
	private static final String S_EXTENSIONS = "showExtensions"; //$NON-NLS-1$
	private static final String S_EXTENSION_POINTS = "showExtensionPoints"; //$NON-NLS-1$
	private static final String S_EXPORTED_PACKAGES = "showExportedPackages"; //$NON-NLS-1$
	private static final String S_FEATURES = "showFeatures"; //$NON-NLS-1$

	private static final int TYPE_PLUGIN = 0;
	private static final int TYPE_EXTENSION = 1;
	private static final int TYPE_EXTENSION_POINT = 2;
	private static final int TYPE_EXPORTED_PACKAGE = 3;
	private static final int TYPE_FEATURE = 4;

	private Action extensionsAction = new ExtensionsAction();
	private Action extensionPointsAction = new ExtensionPointsAction();
	private Action exportedPackagesAction = new ExportedPackagesAction();
	private Action featuresAction = new FeaturesAction();
	private ExtensionsFilter extensionsFilter = new ExtensionsFilter();
	private ExtensionPointsFilter extensionPointsFilter = new ExtensionPointsFilter();
	private ExportedPackagesFilter exportedPackagesFilter = new ExportedPackagesFilter();
	private FeaturesFilter featuresFilter = new FeaturesFilter();

	private SearchLabelProvider searchLabelProvider = new SearchLabelProvider();
	private ILabelProvider detailsLabelProvider = new DetailedLabelProvider();

	private class SearchLabelProvider extends LabelProvider implements ILabelDecorator, IStyledLabelProvider {

		private BoldStylerProvider boldStylerProvider;
		private StyledStringHighlighter styledStringHighlighter = new StyledStringHighlighter();

		@Override
		public Image getImage(Object element) {
			if (element instanceof ExportPackageDescription) {
				return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKAGE);
			}
			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		@Override
		public String getText(Object object) {
			if (object instanceof IPluginBase)
				return ((IPluginBase) object).getId();

			if (object instanceof IPluginImport) {
				IPluginImport dep = (IPluginImport) object;
				return dep.getId() + " - " //$NON-NLS-1$
						+ dep.getPluginBase().getId();
			}

			if (object instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension) object;
				return extension.getPoint() + " - " + extension.getPluginBase().getId(); //$NON-NLS-1$
			}

			if (object instanceof IPluginExtensionPoint)
				return ((IPluginExtensionPoint) object).getFullId();

			if (object instanceof ExportPackageDescription) {
				ExportPackageDescription epd = (ExportPackageDescription) object;
				return epd.getName() + ' ' + '(' + epd.getVersion() + ')';
			}

			if (object instanceof IFeatureModel) {
				IFeatureModel fModel = (IFeatureModel) object;
				IFeature feature = fModel.getFeature();
				return feature.getId() + ' ' + '(' + feature.getVersion() + ')';
			}

			return PDEPlugin.getDefault().getLabelProvider().getText(object);
		}

		@Override
		public Image decorateImage(Image image, Object element) {
			return null;
		}

		@Override
		public String decorateText(String text, Object element) {
			if (element instanceof ExportPackageDescription) {
				ExportPackageDescription epd = (ExportPackageDescription) element;
				return text.concat(" - " + epd.getSupplier().getSymbolicName()); //$NON-NLS-1$
			}
			return text;
		}

		@Override
		public StyledString getStyledText(Object element) {
			String text = getText(element);
			Text patternControl = (Text) FilteredPluginArtifactsSelectionDialog.this.getPatternControl();

			if (patternControl == null) {
				return new StyledString(text);
			}

			String pattern = patternControl.getText();
			return styledStringHighlighter.highlight(text, pattern, getBoldStylerProvider().getBoldStyler());
		}

		private BoldStylerProvider getBoldStylerProvider() {
			if (boldStylerProvider != null) {
				return boldStylerProvider;
			}

			return boldStylerProvider = new BoldStylerProvider(getDialogArea().getFont());
		}

		@Override
		public void dispose() {
			if (boldStylerProvider != null) {
				boldStylerProvider.dispose();
			}

			super.dispose();
		}

	}

	private class DetailedLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			if (element instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) element;
				return getImage(model.getInstallLocation());
			} else if (element instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint model = (IPluginExtensionPoint) element;
				return getImage(model.getModel().getInstallLocation());
			} else if (element instanceof IPluginExtension) {
				IPluginExtension model = (IPluginExtension) element;
				return getImage(model.getModel().getInstallLocation());
			} else if (element instanceof ExportPackageDescription) {
				ExportPackageDescription model = (ExportPackageDescription) element;
				String id = model.getSupplier().getName();
				String version = model.getSupplier().getVersion().toString();
				IPluginModelBase base = getModel(id, version);
				return getImage(base.getInstallLocation());
			} else if (element instanceof IFeatureModel) {
				IFeatureModel model = (IFeatureModel) element;
				return getImage(model.getInstallLocation());
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) element;
				return model.getInstallLocation();
			} else if (element instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint model = (IPluginExtensionPoint) element;
				return model.getModel().getInstallLocation();
			} else if (element instanceof IPluginExtension) {
				IPluginExtension model = (IPluginExtension) element;
				return model.getModel().getInstallLocation();
			} else if (element instanceof ExportPackageDescription) {
				ExportPackageDescription model = (ExportPackageDescription) element;
				String id = model.getSupplier().getName();
				String version = model.getSupplier().getVersion().toString();
				IPluginModelBase base = getModel(id, version);
				return base.getInstallLocation();
			} else if (element instanceof IFeatureModel) {
				IFeatureModel model = (IFeatureModel) element;
				return model.getInstallLocation();
			}
			return null;
		}

		private Image getImage(String location) {
			if (location != null && new File(location).isFile())
				return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		}

	}

	private static class ExtensionsFilter extends ViewerFilter {

		private boolean enabled = true;

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (enabled) // select everything
				return true;

			if (element instanceof IPluginExtension) {
				return false;
			}
			return true;
		}

		public void setEnabled(boolean value) {
			this.enabled = value;
		}
	}

	private static class ExtensionPointsFilter extends ViewerFilter {

		private boolean enabled = true;

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (enabled) // select everything
				return true;

			if (element instanceof IPluginExtensionPoint) {
				return false;
			}
			return true;
		}

		public void setEnabled(boolean value) {
			this.enabled = value;
		}

	}

	private static class ExportedPackagesFilter extends ViewerFilter {

		private boolean enabled = true;

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (enabled) // select everything
				return true;

			if (element instanceof ExportPackageDescription) {
				return false;
			}
			return true;
		}

		public void setEnabled(boolean value) {
			this.enabled = value;
		}

	}

	private static class FeaturesFilter extends ViewerFilter {

		private boolean enabled = true;

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (enabled) // select everything
				return true;

			if (element instanceof IFeatureModel) {
				return false;
			}
			return true;
		}

		public void setEnabled(boolean value) {
			this.enabled = value;
		}

	}

	private class ExtensionsAction extends Action {

		public ExtensionsAction() {
			super(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_showExtensions, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

		@Override
		public void run() {
			extensionsFilter.setEnabled(isChecked());
			scheduleRefresh();
		}

	}

	private class ExtensionPointsAction extends Action {

		public ExtensionPointsAction() {
			super(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_showExtensionPoints, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

		@Override
		public void run() {
			extensionPointsFilter.setEnabled(isChecked());
			scheduleRefresh();
		}

	}

	private class ExportedPackagesAction extends Action {

		public ExportedPackagesAction() {
			super(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_showExportedPackages, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

		@Override
		public void run() {
			exportedPackagesFilter.setEnabled(isChecked());
			scheduleRefresh();
		}

	}

	private class FeaturesAction extends Action {

		public FeaturesAction() {
			super(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_showFeatures, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

		@Override
		public void run() {
			featuresFilter.setEnabled(isChecked());
			scheduleRefresh();
		}

	}

	public FilteredPluginArtifactsSelectionDialog(Shell shell) {
		super(shell, false);

		setTitle(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_title);
		setMessage(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_message);
		setSelectionHistory(new PluginSearchSelectionHistory());

		PDEPlugin.getDefault().getLabelProvider().connect(this);
		setListLabelProvider(searchLabelProvider);
		setListSelectionLabelDecorator(searchLabelProvider);
		setDetailsLabelProvider(detailsLabelProvider);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.FILTERED_PLUGIN_ARTIFACTS_DIALOG);
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new PluginSearchItemsFilter();
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {

		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase[] models = manager.getActiveModels(true);

		FeatureModelManager fManager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] fModels = fManager.getModels();

		SubMonitor subMonitor = SubMonitor.convert(progressMonitor,
				PDEUIMessages.FilteredPluginArtifactsSelectionDialog_searching, models.length * 2 + fModels.length);

		// cycle through all the features first
		for (IFeatureModel model : fModels) {
			contentProvider.add(model, itemsFilter);
			subMonitor.worked(1);
		}

		// cycle through all the models and grab entries
		for (IPluginModelBase model : models) {
			IPluginExtensionPoint[] eps = model.getPluginBase().getExtensionPoints();
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			int length = eps.length + extensions.length;
			SubMonitor subMonitor2 = subMonitor.split(1).setWorkRemaining(length);
			for (IPluginExtensionPoint ep : eps) {
				contentProvider.add(ep, itemsFilter);
				subMonitor2.worked(1);
			}
			for (IPluginExtension extension : extensions) {
				contentProvider.add(extension, itemsFilter);
				subMonitor2.worked(1);
			}

			subMonitor2 = subMonitor.split(1);
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				ExportPackageDescription[] epds = desc.getExportPackages();
				subMonitor2.setWorkRemaining(epds.length);
				for (ExportPackageDescription epd : epds) {
					// ensure we don't get EE packages
					int ee = ((Integer) epd.getDirective("x-equinox-ee")).intValue(); //$NON-NLS-1$
					if (ee < 0) {
						contentProvider.add(epd, itemsFilter);
					}
					subMonitor2.worked(1);
				}
			}

			contentProvider.add(model, itemsFilter);
		}
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	@Override
	public String getElementName(Object item) {
		if (item instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) item;
			return model.getPluginBase().getId();
		} else if (item instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint model = (IPluginExtensionPoint) item;
			return model.getFullId();
		} else if (item instanceof IPluginExtension) {
			IPluginExtension model = (IPluginExtension) item;
			return model.getPoint();
		} else if (item instanceof ExportPackageDescription) {
			ExportPackageDescription model = (ExportPackageDescription) item;
			return model.getName();
		} else if (item instanceof IFeatureModel) {
			IFeatureModel model = (IFeatureModel) item;
			return model.getFeature().getId();
		}
		return null;
	}

	@Override
	protected Comparator<?> getItemsComparator() {
		return new PluginSearchComparator();
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);
		menuManager.add(new Separator());
		menuManager.add(extensionsAction);
		menuManager.add(extensionPointsAction);
		menuManager.add(exportedPackagesAction);
		menuManager.add(featuresAction);
	}

	@Override
	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		if (settings.get(S_EXTENSIONS) != null) {
			boolean state = settings.getBoolean(S_EXTENSIONS);
			extensionsAction.setChecked(state);
		}

		if (settings.get(S_EXTENSION_POINTS) != null) {
			boolean state = settings.getBoolean(S_EXTENSION_POINTS);
			extensionPointsAction.setChecked(state);
		}

		if (settings.get(S_EXPORTED_PACKAGES) != null) {
			boolean state = settings.getBoolean(S_EXPORTED_PACKAGES);
			exportedPackagesAction.setChecked(state);
		}

		if (settings.get(S_FEATURES) != null) {
			boolean state = settings.getBoolean(S_FEATURES);
			featuresAction.setChecked(state);
		}

		addListFilter(extensionsFilter);
		addListFilter(extensionPointsFilter);
		addListFilter(exportedPackagesFilter);
		addListFilter(featuresFilter);
		applyFilter();
	}

	@Override
	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);
		settings.put(S_EXTENSIONS, extensionsAction.isChecked());
		settings.put(S_EXTENSION_POINTS, extensionPointsAction.isChecked());
		settings.put(S_EXPORTED_PACKAGES, exportedPackagesAction.isChecked());
		settings.put(S_FEATURES, featuresAction.isChecked());
	}

	private static class PluginSearchSelectionHistory extends SelectionHistory {

		private static final String M_ID = "id"; //$NON-NLS-1$
		private static final String M_PLUGIN_VERSION = "p_version"; //$NON-NLS-1$
		private static final String M_PLUGIN_ID = "p_id"; //$NON-NLS-1$
		private static final String M_TYPE = "type"; //$NON-NLS-1$

		@Override
		protected Object restoreItemFromMemento(IMemento memento) {
			Integer itype = memento.getInteger(M_TYPE);
			if (itype == null)
				return null;
			int type = itype.intValue();

			switch (type) {
				case TYPE_PLUGIN :
					IPluginModelBase model = getModel(memento);
					if (model == null)
						return null;
					return model;
				case TYPE_EXTENSION_POINT :
					model = getModel(memento);
					if (model == null)
						return null;
					IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
					String epid = memento.getString(M_ID);
					for (IPluginExtensionPoint point : points) {
						if (epid.equals(point.getFullId()))
							return point;
					}
					break;
				case TYPE_EXTENSION :
					model = getModel(memento);
					if (model == null)
						return null;
					IPluginExtension[] extensions = model.getPluginBase().getExtensions();
					String eid = memento.getString(M_ID);
					for (IPluginExtension extension : extensions) {
						if (eid.equals(extension.getPoint()))
							return extension;
					}
					break;
				case TYPE_EXPORTED_PACKAGE :
					model = getModel(memento);
					if (model == null)
						return null;
					ExportPackageDescription[] descriptions = model.getBundleDescription().getExportPackages();
					String pid = memento.getString(M_ID);
					for (ExportPackageDescription desc : descriptions) {
						if (pid.equals(desc.getName()))
							return desc;
					}
				case TYPE_FEATURE :
					IFeatureModel fModel = getFeatureModel(memento);
					if (fModel == null)
						return null;
					return fModel;
			}
			return null;
		}

		protected IFeatureModel getFeatureModel(IMemento memento) {
			String id = memento.getString(M_ID);
			String version = memento.getString(M_PLUGIN_VERSION);
			IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
			for (IFeatureModel model : models) {
				IFeature feature = model.getFeature();
				if (feature.getId().equals(id) && feature.getVersion().equals(version)) {
					return model;
				}
			}
			return null;
		}

		protected IPluginModelBase getModel(IMemento memento) {
			String id = memento.getString(M_PLUGIN_ID);
			String version = memento.getString(M_PLUGIN_VERSION);
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry == null)
				return null;
			IPluginModelBase[] models = entry.getActiveModels();
			for (IPluginModelBase model : models) {
				if (version.equals(model.getPluginBase().getVersion()))
					return model;
			}
			return null;
		}

		@Override
		protected void storeItemToMemento(Object item, IMemento memento) {
			if (item instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) item;
				memento.putInteger(M_TYPE, TYPE_PLUGIN);
				memento.putString(M_PLUGIN_ID, model.getPluginBase().getId());
				memento.putString(M_PLUGIN_VERSION, model.getPluginBase().getVersion());
			} else if (item instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint model = (IPluginExtensionPoint) item;
				memento.putInteger(M_TYPE, TYPE_EXTENSION_POINT);
				memento.putString(M_ID, model.getFullId());
				memento.putString(M_PLUGIN_ID, model.getPluginBase().getId());
				memento.putString(M_PLUGIN_VERSION, model.getPluginBase().getVersion());
			} else if (item instanceof IPluginExtension) {
				IPluginExtension model = (IPluginExtension) item;
				memento.putInteger(M_TYPE, TYPE_EXTENSION);
				memento.putString(M_ID, model.getPoint());
				memento.putString(M_PLUGIN_ID, model.getPluginBase().getId());
				memento.putString(M_PLUGIN_VERSION, model.getPluginBase().getVersion());
			} else if (item instanceof ExportPackageDescription) {
				ExportPackageDescription model = (ExportPackageDescription) item;
				memento.putInteger(M_TYPE, TYPE_EXPORTED_PACKAGE);
				memento.putString(M_ID, model.getName());
				memento.putString(M_PLUGIN_ID, model.getSupplier().getSymbolicName());
				memento.putString(M_PLUGIN_VERSION, model.getSupplier().getVersion().toString());
			} else if (item instanceof IFeatureModel) {
				IFeatureModel model = (IFeatureModel) item;
				memento.putInteger(M_TYPE, TYPE_FEATURE);
				memento.putString(M_ID, model.getFeature().getId());
				memento.putString(M_PLUGIN_VERSION, model.getFeature().getVersion());
			}
		}

	}

	private class PluginSearchItemsFilter extends ItemsFilter {

		public PluginSearchItemsFilter() {
			super();
			String pattern = patternMatcher.getPattern();
			if (pattern.indexOf('*') != 0 && pattern.indexOf('?') != 0 && pattern.indexOf('.') != 0) {
				pattern = "*" + pattern; //$NON-NLS-1$
				patternMatcher.setPattern(pattern);
			}
		}

		// TODO probably have to make this realistic
		@Override
		public boolean isConsistentItem(Object item) {
			return true;
		}

		@Override
		public boolean matchItem(Object item) {
			String id = null;
			if (item instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) item;
				id = model.getPluginBase().getId();
			} else if (item instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint model = (IPluginExtensionPoint) item;
				id = model.getFullId();
			} else if (item instanceof IPluginExtension) {
				IPluginExtension model = (IPluginExtension) item;
				id = model.getPoint();
			} else if (item instanceof ExportPackageDescription) {
				ExportPackageDescription model = (ExportPackageDescription) item;
				id = model.getName();
			} else if (item instanceof IFeatureModel) {
				IFeatureModel model = (IFeatureModel) item;
				id = model.getFeature().getId();
			}

			// if the id does not match, check to see if a segment matches.
			// This is how PatternFilter searches for matches (see PatternFilter.getWords(String))
			return (matches(id)) ? true : matchesSegments(id);
		}

		private boolean matchesSegments(String id) {
			BreakIterator iter = BreakIterator.getWordInstance();
			iter.setText(id);
			int i = iter.first();
			while (i != java.text.BreakIterator.DONE && i < id.length()) {
				int j = iter.following(i);
				if (j == java.text.BreakIterator.DONE) {
					j = id.length();
				}
				// match the word
				if (Character.isLetterOrDigit(id.charAt(i))) {
					String word = id.substring(i, j);
					if (matches(word))
						return true;
				}
				i = j;
			}
			return false;
		}
	}

	private static class PluginSearchComparator implements Comparator<Object> {

		@Override
		public int compare(Object o1, Object o2) {
			int id1 = getId(o1);
			int id2 = getId(o2);

			if (id1 != id2)
				return id1 - id2;
			return compareSimilarObjects(o1, o2);
		}

		private int getId(Object element) {
			if (element instanceof IPluginModelBase) {
				return 100;
			} else if (element instanceof IPluginExtensionPoint) {
				return 200;
			} else if (element instanceof IPluginExtension) {
				return 300;
			} else if (element instanceof ExportPackageDescription) {
				return 400;
			} else if (element instanceof IFeatureModel) {
				return 500;
			}
			return 0;
		}

		private int compareSimilarObjects(Object o1, Object o2) {
			if (o1 instanceof IPluginModelBase && o2 instanceof IPluginModelBase) {
				IPluginModelBase ipmb1 = (IPluginModelBase) o1;
				IPluginModelBase ipmb2 = (IPluginModelBase) o2;
				return comparePlugins(ipmb1.getPluginBase(), ipmb2.getPluginBase());
			} else if (o1 instanceof IPluginExtensionPoint && o2 instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint ipep1 = (IPluginExtensionPoint) o1;
				IPluginExtensionPoint ipep2 = (IPluginExtensionPoint) o2;
				return compareExtensionPoints(ipep1, ipep2);
			} else if (o1 instanceof IPluginExtension && o2 instanceof IPluginExtension) {
				IPluginExtension ipe1 = (IPluginExtension) o1;
				IPluginExtension ipe2 = (IPluginExtension) o2;
				int comparePointsResult = ipe1.getPoint().compareTo(ipe2.getPoint());
				if (comparePointsResult == 0)
					return comparePlugins(ipe1.getPluginBase(), ipe2.getPluginBase());
				// else
				return comparePointsResult;
			} else if (o1 instanceof ExportPackageDescription && o2 instanceof ExportPackageDescription) {
				ExportPackageDescription epd1 = (ExportPackageDescription) o1;
				ExportPackageDescription epd2 = (ExportPackageDescription) o2;
				int compareNamesResult = epd1.getName().compareTo(epd2.getName());
				if (compareNamesResult == 0)
					return compareBundleDescriptions(epd1.getSupplier(), epd2.getSupplier());
				// else
				return compareNamesResult;
			} else if (o1 instanceof IFeatureModel && o2 instanceof IFeatureModel) {
				IFeatureModel ifm1 = (IFeatureModel) o1;
				IFeatureModel ifm2 = (IFeatureModel) o2;
				return compareFeatures(ifm1, ifm2);
			}
			return 0;
		}

		private int comparePlugins(IPluginBase ipmb1, IPluginBase ipmb2) {
			return ipmb1.getId().compareTo(ipmb2.getId());
		}

		private int compareExtensionPoints(IPluginExtensionPoint ipep1, IPluginExtensionPoint ipep2) {
			return ipep1.getFullId().compareTo(ipep2.getFullId());
		}

		private int compareBundleDescriptions(BundleDescription bd1, BundleDescription bd2) {
			return bd1.getName().compareTo(bd2.getName());
		}

		private int compareFeatures(IFeatureModel ifm1, IFeatureModel ifm2) {
			return ifm1.getFeature().getId().compareTo(ifm2.getFeature().getId());
		}
	}

	@Override
	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	private IPluginModelBase getModel(String id, String version) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		IPluginModelBase[] models = entry.getActiveModels();
		for (IPluginModelBase model : models) {
			if (version.equals(model.getPluginBase().getVersion()))
				return model;
		}
		return null;
	}

}
