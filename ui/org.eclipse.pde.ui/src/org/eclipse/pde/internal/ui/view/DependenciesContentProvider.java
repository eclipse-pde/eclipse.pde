package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class DependenciesContentProvider
	extends DefaultContentProvider
	implements ITreeContentProvider, IStructuredContentProvider, IPluginModelListener {
	private PluginModelManager manager;
	private TreeViewer viewer;
	private DependenciesView view;

	/**
	 * Constructor for PluginsContentProvider.
	 */
	public DependenciesContentProvider(DependenciesView view, PluginModelManager manager) {
		this.manager = manager;
		manager.addPluginModelListener(this);
		this.view = view;
	}

	public void dispose() {
		manager.removePluginModelListener(this);
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;
		if (newInput==null) return;
		view.updateTitle(newInput);
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IPluginModelBase) {
			return new Object[] {((IPluginModelBase)parentElement).getPluginBase()};
		}
		if (parentElement instanceof IPluginBase) {
			return createImportObjects((IPluginBase)parentElement);
		}
		if (parentElement instanceof ImportObject) {
			ImportObject iobj = (ImportObject)parentElement;
			IPlugin plugin = iobj.getPlugin();
			if (plugin==null) return new Object[0];
			return createImportObjects(plugin);
		}
		return new Object[0];
	}
	
	private Object [] createImportObjects(IPluginBase plugin) {
		IPluginImport [] imports = plugin.getImports();
		Object [] result = new Object[imports.length];
		for (int i=0; i<imports.length; i++) {
			result[i] = new ImportObject(imports[i]);
		}
		return result;
	}

	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		return null;
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length>0;
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void modelsChanged(final PluginModelDelta delta) {
		if (viewer == null || viewer.getTree().isDisposed())
			return;

		viewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int kind = delta.getKind();
				if (viewer.getTree().isDisposed())
					return;
				if ((kind & PluginModelDelta.CHANGED) !=0 ||
					(kind & PluginModelDelta.ADDED) !=0) {
					// Don't know exactly what change - 
					// the safest way out is to refresh
					viewer.refresh();
					return;
				}
				if ((kind & PluginModelDelta.REMOVED) != 0) {
					ModelEntry[] removed = delta.getRemovedEntries();
					handleRemoved(removed);
				}
				if ((kind & PluginModelDelta.ADDED) != 0) {
					viewer.refresh();
				}
			}
		});
	}
	private void handleRemoved(ModelEntry [] removed) {
		for (int i=0; i<removed.length; i++) {
			ModelEntry entry = removed[i];
			IPluginModelBase model = entry.getActiveModel();
			if (model!=null && model.equals(viewer.getInput())) {
				viewer.setInput(null);
				return;
			}
		}
		viewer.refresh();
	}
}