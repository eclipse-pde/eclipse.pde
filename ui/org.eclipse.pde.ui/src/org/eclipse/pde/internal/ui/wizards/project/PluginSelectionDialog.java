/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.project;

import java.util.Vector;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class PluginSelectionDialog extends ElementTreeSelectionDialog {
	private TreeViewer treeViewer;
	private static NamedElement workspacePlugins;
	private static NamedElement externalPlugins;

	private static class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				IExternalModelManager manager =
					PDECore.getDefault().getExternalModelManager();
				return manager.getPluginModels();
			}
			if (parent == workspacePlugins) {
				IWorkspaceModelManager manager =
					PDECore.getDefault().getWorkspaceModelManager();
				return manager.getPluginModels();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) child;
				if (model.getUnderlyingResource() != null)
					return workspacePlugins;
				else
					return externalPlugins;
			}
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { workspacePlugins, externalPlugins };
		}
	}

	public PluginSelectionDialog(Shell parentShell) {
		super(
			parentShell,
			PDEPlugin.getDefault().getLabelProvider(),
			new PluginContentProvider());
		setTitle(PDEPlugin.getResourceString("PluginSelectionDialog.title"));
		setMessage(PDEPlugin.getResourceString("PluginSelectionDialog.message"));	
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createMessageArea(container);
		
		initialize();
		
		Image pluginsImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
		workspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString("PluginSelectionDialog.workspacePlugins"),
				pluginsImage);
		externalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString("PluginSelectionDialog.externalPlugins"),
				pluginsImage);
				
		Control tree = createTree(container);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		WorkbenchHelp.setHelp(container, IHelpContextIds.FRAGMENT_ADD_TARGET);
		
		
		return container;
	}
	
	private void initialize(){		
		addFilter(new ViewerFilter() {
				public boolean select(Viewer v, Object parent, Object object) {
					if (object instanceof IPluginModel) {
						return ((IPluginModel) object).isEnabled();
					}
					return true;
				}
			});

		setSorter(new ListUtil.PluginSorter() {
			public int category(Object obj) {
				if (obj == workspacePlugins)
					return -1;
				if (obj == externalPlugins)
					return 1;
				return 0;
			}
		});
	
		setAllowMultiple(false);
	

		setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null
					&& selection.length > 0
					&& selection[0] instanceof IPluginModel)
					return new Status(
						IStatus.OK,
						PDEPlugin.getPluginId(),
						IStatus.OK,
						((LabelProvider) treeViewer.getLabelProvider()).getText(
							selection[0]),
						null);
				return new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					"",
					null);
			}
		});
	}
	
	private Control createTree(Composite container) {
		createTreeViewer(container);
		treeViewer = getTreeViewer();
		treeViewer.setAutoExpandLevel(2);
		treeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				Object item =
					((IStructuredSelection) e.getSelection()).getFirstElement();
				if (item instanceof IPluginModel)
					pluginSelected((IPluginModel) item);
				else
					pluginSelected(null);
			}
		});
		treeViewer.setInput(PDEPlugin.getDefault());
		treeViewer.reveal(workspacePlugins);

		return getTreeViewer().getTree();
	}
	
	private void pluginSelected(IPluginModel model) {
		if (model != null) {
			Vector result = new Vector();
			result.add(model);
			setResult(result);
		} else {
			setResult(null);
		}
	}
	

}
