package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.swt.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;

public class PluginSelectionDialog extends SelectionDialog {
	public static final String KEY_TITLE = "PluginSelectionDialog.title";
	public static final String KEY_WORKSPACE_PLUGINS =
		"PluginSelectionDialog.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"PluginSelectionDialog.externalPlugins";
	private TreeViewer treeViewer;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Label messageLabel;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				ExternalModelManager manager = PDEPlugin.getDefault().getExternalModelManager();
				IPluginModel[] models = manager.getModels();
				ArraySorter.INSTANCE.sortInPlace(models);
				return models;
			}
			if (parent == workspacePlugins) {
				WorkspaceModelManager manager =
					PDEPlugin.getDefault().getWorkspaceModelManager();
				IPluginModel[] models = manager.getWorkspacePluginModels();
				ArraySorter.INSTANCE.sortInPlace(models);
				return models;
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
		super(parentShell);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
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

		Control tree = createTree(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(gd);
		messageLabel = new Label(container, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		messageLabel.setLayoutData(gd);
		updateMessageLabel();
		return container;
	}
	private Control createTree(Composite container) {
		Tree tree = new Tree(container, SWT.SINGLE | SWT.BORDER);

		treeViewer = new TreeViewer(tree);
		treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		treeViewer.setContentProvider(new PluginContentProvider());
		treeViewer.setAutoExpandLevel(999);
		treeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IPluginModel) {
					return ((IPluginModel) object).isEnabled();
				}
				return true;
			}
		});
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				Object item = ((IStructuredSelection) e.getSelection()).getFirstElement();
				if (item instanceof IPluginModel)
					pluginSelected((IPluginModel) item);
				else
					pluginSelected(null);
			}
		});
		Image pluginsImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
		workspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS),
				pluginsImage);
		externalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS),
				pluginsImage);
		treeViewer.setInput(PDEPlugin.getDefault());
		return tree;
	}
	private void pluginSelected(IPluginModel model) {
		if (model != null) {
			Vector result = new Vector();
			result.add(model);
			setResult(result);
			setMessage(((LabelProvider) treeViewer.getLabelProvider()).getText(model));
		} else {
			setResult(null);
			setMessage("");
		}
	}
	public void setMessage(String message) {
		super.setMessage(message);
		updateMessageLabel();
	}
	public void updateMessageLabel() {
		if (getMessage() != null)
			messageLabel.setText(getMessage());
		else
			messageLabel.setText("");
	}
}