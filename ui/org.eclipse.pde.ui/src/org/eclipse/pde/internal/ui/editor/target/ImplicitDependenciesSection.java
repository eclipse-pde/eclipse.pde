package org.eclipse.pde.internal.ui.editor.target;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ImplicitDependenciesSection extends TableSection {
	
	private TableViewer fViewer;
	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int REMOVE_ALL_INDEX = 2;

	public ImplicitDependenciesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.ImplicitDependenicesSection_Add , PDEUIMessages.ImplicitDependenicesSection_Remove, PDEUIMessages.ImplicitDependenicesSection_RemoveAll});
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ImplicitDependenicesSection_Title);
		section.setDescription(PDEUIMessages.TargetImplicitPluginsTab_desc);
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fViewer = getTablePart().getTableViewer();
		fViewer.setContentProvider(new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return getImplicitPluginsInfo().getPlugins();
			}
		});
		fViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				ITargetPlugin p1 = (ITargetPlugin) e1;
				ITargetPlugin p2 = (ITargetPlugin) e2;
				return super.compare(viewer, p1.getId(), p2.getId());
			}
		});
		fViewer.setInput(getTarget());
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
        GridData gd = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(gd);
		updateButtons();
		getModel().addModelChangedListener(this);
	}

    public boolean doGlobalAction(String actionId) {
        if (actionId.equals(ActionFactory.DELETE.getId())) {
            handleRemove();
            return true;
        }
        if (actionId.equals(ActionFactory.CUT.getId())) {
            // delete here and let the editor transfer
            // the selection to the clipboard
            handleRemove();
            return false;
        }
        if (actionId.equals(ActionFactory.PASTE.getId())) {
            doPaste();
            return true;
        }
        return false;
    }
	
	private void updateButtons() {
		TablePart part = getTablePart();
		boolean empty = fViewer.getSelection().isEmpty();
		part.setButtonEnabled(1, !empty);
		boolean hasElements = fViewer.getTable().getItemCount() > 0;
		part.setButtonEnabled(2, hasElements);
	}
	
    protected void buttonSelected(int index) {
        switch (index) {
        case ADD_INDEX:
            handleAdd();
            break;
        case REMOVE_INDEX:
            handleRemove();
            break;
        case REMOVE_ALL_INDEX:
            handleRemoveAll();
        }
	}
    
    protected void handleAdd() {
    	ElementListSelectionDialog dialog = new ElementListSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(), 
				PDEPlugin.getDefault().getLabelProvider());
		
		dialog.setElements(getValidBundles());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title); 
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			ArrayList pluginsToAdd = new ArrayList();
			ITargetModel model = getModel();
			for (int i = 0; i < models.length; i++) {
				BundleDescription desc = (BundleDescription) models[i];
				ITargetPlugin plugin = model.getFactory().createPlugin();
				plugin.setId(desc.getSymbolicName());
				plugin.setModel(model);
				pluginsToAdd.add(plugin);
			}
			getImplicitPluginsInfo().addPlugins((ITargetPlugin[])pluginsToAdd.toArray(new ITargetPlugin[pluginsToAdd.size()]));
			updateButtons();
		}
    }
    
	protected Object[] getValidBundles() {
		ITargetPlugin[] plugins = getImplicitPluginsInfo().getPlugins();
		Set currentPlugins = new HashSet((4/3) * plugins.length + 1);
		for (int i = 0; i < plugins.length; i++) {
			currentPlugins.add(plugins[i].getId());
		}
		
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getAllPlugins();
		Set result = new HashSet((4/3) * models.length + 1);
		for (int i = 0; i < models.length; i++) {
			BundleDescription desc = models[i].getBundleDescription();
			if (desc != null) {
				if (!currentPlugins.contains(desc.getSymbolicName()))
					result.add(desc);
			}
		}
		return result.toArray();
	}
	
	protected void handleRemove() {
		Object[] src = ((IStructuredSelection)fViewer.getSelection()).toArray();
		ITargetPlugin[] plugins = new ITargetPlugin[src.length];
		System.arraycopy(src, 0, plugins, 0, src.length);
		getImplicitPluginsInfo().removePlugins(plugins);
	}
	
	protected void handleRemoveAll() {
		IImplicitDependenciesInfo info =getImplicitPluginsInfo();
		info.removePlugins(info.getPlugins());
	}
	
	private IImplicitDependenciesInfo getImplicitPluginsInfo() {
		IImplicitDependenciesInfo info = getTarget().getImplicitPluginsInfo();
		if (info == null) {
			info = getModel().getFactory().createImplicitPluginInfo();
			getTarget().setImplicitPluginsInfo(info);
		}
		return info;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (e.getChangeType() == IModelChangedEvent.CHANGE && 
				e.getChangedProperty().equals(IImplicitDependenciesInfo.P_IMPLICIT_PLUGINS)) {
			ITargetPlugin[] plugins = (ITargetPlugin[])e.getOldValue();
			for (int i = 0; i < plugins.length; i++)
				fViewer.remove(plugins[i]);
			plugins = (ITargetPlugin[])e.getNewValue();
			for (int i = 0; i < plugins.length; i++) 
				fViewer.add(plugins[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TableSection#handleDoubleClick(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}
	
	private void handleOpen(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		ManifestEditor.openPluginEditor(((ITargetPlugin)object).getId());
	}
	
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof ITargetPlugin))
				return false;
		}
		return true;
	}
	
	protected void doPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if ( objects[i] instanceof ITargetPlugin )
				getImplicitPluginsInfo().addPlugin((ITargetPlugin)objects[i]);	
		}
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
	
//	public void commit(boolean onSave) {
//		
//	}
	
	public void dispose() {
		getModel().removeModelChangedListener(this);
	}
}
