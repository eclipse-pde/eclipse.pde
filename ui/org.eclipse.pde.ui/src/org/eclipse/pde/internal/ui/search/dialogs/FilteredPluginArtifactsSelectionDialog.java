/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import java.util.Comparator;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class FilteredPluginArtifactsSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.pde.ui.dialogs.FilteredPluginArtifactsSelectionDialog"; //$NON-NLS-1$
	private static final String S_EXTENSIONS = "showExtensions"; //$NON-NLS-1$
	private static final String S_EXTENSION_POINTS = "showExtensionPoints"; //$NON-NLS-1$

	private static final int TYPE_PLUGIN = 0;
	private static final int TYPE_EXTENSION = 1;
	private static final int TYPE_EXTENSION_POINT = 2;

	private Action extensionsAction = new ExtensionsAction();
	private Action extensionPointsAction = new ExtensionPointsAction();
	private ExtensionsFilter extensionsFilter = new ExtensionsFilter();
	private ExtensionPointsFilter extensionPointsFilter = new ExtensionPointsFilter();

	// TODO implement ILabelDecorator?
	private class SearchLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

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

			return PDEPlugin.getDefault().getLabelProvider().getText(object);
		}
	}

	private class DetailedLabelProvider extends LabelProvider {

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
			}
			return null;
		}

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
			}
			return null;
		}

		private Image getImage(String location) {
			if (location.endsWith(".jar")) //$NON-NLS-1$
				return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		}

	}

	private class ExtensionsFilter extends ViewerFilter {

		private boolean enabled = true;

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

	private class ExtensionPointsFilter extends ViewerFilter {

		private boolean enabled = true;

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

	private class ExtensionsAction extends Action {

		public ExtensionsAction() {
			super(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_showExtensions, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

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

		public void run() {
			extensionPointsFilter.setEnabled(isChecked());
			scheduleRefresh();
		}

	}

	public FilteredPluginArtifactsSelectionDialog(Shell shell) {
		super(shell, false);

		setTitle(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_title);
		setMessage(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_message);
		setSelectionHistory(new PluginSearchSelectionHistory());

		PDEPlugin.getDefault().getLabelProvider().connect(this);
		setListLabelProvider(new SearchLabelProvider());
		setDetailsLabelProvider(new DetailedLabelProvider());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createExtendedContentArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	protected ItemsFilter createFilter() {
		return new PluginSearchItemsFilter();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillContentProvider(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.AbstractContentProvider, org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {

		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase[] models = manager.getActiveModels(true);

		progressMonitor.beginTask(PDEUIMessages.FilteredPluginArtifactsSelectionDialog_searching, models.length);

		// cycle through all the containers and grab entries 
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			IPluginExtensionPoint[] eps = model.getPluginBase().getExtensionPoints();
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			int length = eps.length + extensions.length;
			SubProgressMonitor subMonitor = new SubProgressMonitor(progressMonitor, length);
			for (int j = 0; j < eps.length; j++) {
				contentProvider.add(eps[j], itemsFilter);
				subMonitor.worked(1);
			}
			for (int j = 0; j < extensions.length; j++) {
				contentProvider.add(extensions[j], itemsFilter);
				subMonitor.worked(1);
			}
			subMonitor.done();
			contentProvider.add(models[i], itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getDialogSettings()
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getElementName(java.lang.Object)
	 */
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
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getItemsComparator()
	 */
	protected Comparator getItemsComparator() {
		return new PluginSearchComparator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, "org.eclipse.pde.ui", 0, "", null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);
		menuManager.add(new Separator());
		menuManager.add(extensionsAction);
		menuManager.add(extensionPointsAction);
	}

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

		addListFilter(extensionsFilter);
		addListFilter(extensionPointsFilter);
		applyFilter();
	}

	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);
		settings.put(S_EXTENSIONS, extensionsAction.isChecked());
		settings.put(S_EXTENSION_POINTS, extensionPointsAction.isChecked());
	}

	private class PluginSearchSelectionHistory extends SelectionHistory {

		private static final String M_ID = "id"; //$NON-NLS-1$
		private static final String M_PLUGIN_VERSION = "p_version"; //$NON-NLS-1$
		private static final String M_PLUGIN_ID = "p_id"; //$NON-NLS-1$
		private static final String M_TYPE = "type"; //$NON-NLS-1$

		protected Object restoreItemFromMemento(IMemento memento) {
			int type = memento.getInteger(M_TYPE).intValue();
			IPluginModelBase model = getModel(memento);
			if (model == null)
				return null;
			switch (type) {
				case TYPE_PLUGIN :
					return model;
				case TYPE_EXTENSION_POINT :
					IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
					String epid = memento.getString(M_ID);
					for (int i = 0; i < points.length; i++) {
						IPluginExtensionPoint point = points[i];
						if (epid.equals(point.getFullId()))
							return point;
					}
					break;
				case TYPE_EXTENSION :
					IPluginExtension[] extensions = model.getPluginBase().getExtensions();
					String eid = memento.getString(M_ID);
					for (int i = 0; i < extensions.length; i++) {
						IPluginExtension extension = extensions[i];
						if (eid.equals(extension.getPoint()))
							return extension;
					}
					break;
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
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				if (version.equals(model.getPluginBase().getVersion()))
					return model;
			}
			return null;
		}

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
			}
		}

	}

	private class PluginSearchItemsFilter extends ItemsFilter {

		// TODO probably have to make this realistic
		public boolean isConsistentItem(Object item) {
			return true;
		}

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

	private class PluginSearchComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			Collator collator = Collator.getInstance();
			String s1 = ""; //$NON-NLS-1$
			String s2 = ""; //$NON-NLS-1$
			if (o1 instanceof IPluginModelBase) {
				IPluginModelBase item = (IPluginModelBase) o1;
				s1 = item.getPluginBase().getId();
			}
			if (o2 instanceof IPluginModelBase) {
				IPluginModelBase item = (IPluginModelBase) o2;
				s2 = item.getPluginBase().getId();
			}
			if (o1 instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint item = (IPluginExtensionPoint) o1;
				s1 = item.getFullId();
			}
			if (o2 instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint item = (IPluginExtensionPoint) o2;
				s2 = item.getFullId();
			}
			if (o1 instanceof IPluginExtension) {
				IPluginExtension item = (IPluginExtension) o1;
				s1 = item.getPoint();
			}
			if (o2 instanceof IPluginExtension) {
				IPluginExtension item = (IPluginExtension) o2;
				s2 = item.getPoint();
			}
			return collator.compare(s1, s2);
		}

	}

	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

}
