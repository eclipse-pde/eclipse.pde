package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class ReferenceWizardPage extends WizardPage {
	public static final String KEY_PLUGINS =
		"FeatureEditor.PluginSection.new.label";
	protected IFeatureModel model;
	private TablePart checkboxTablePart;
	private CheckboxTableViewer pluginViewer;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getChoices();
		}
	}
	
	class TablePart extends WizardCheckboxTablePart {
		public TablePart() {
			super(PDEPlugin.getResourceString(KEY_PLUGINS));
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			setPageComplete(count>0);
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormWidgetFactory factory) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, factory);
			viewer.setSorter(ListUtil.PLUGIN_SORTER);
			return viewer;
		}
	}

	public ReferenceWizardPage(IFeatureModel model) {
		super("newFeaturePluginPage");
		this.model = model;
		setPageComplete(false);
		checkboxTablePart = new TablePart();	
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		createPluginList(container);
		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
	}

	protected void createPluginList(Composite parent) {
		checkboxTablePart.createControl(parent);
		pluginViewer = checkboxTablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) object;
					return !isOnTheList(model);
				}
				return true;
			}
		});
		GridData gd = (GridData)checkboxTablePart.getControl().getLayoutData();
		gd.heightHint = 300;
	}
	
	protected abstract boolean isOnTheList(IPluginModelBase candidate);

	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		pluginViewer.setInput(model.getFeature());
		checkboxTablePart.setSelection(new Object[0]);
	}

	private Object[] getChoices() {
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModel[] plugins = mng.getWorkspacePluginModels();
		IFragmentModel[] fragments = mng.getWorkspaceFragmentModels();
		Object[] choices = new Object[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, choices, 0, plugins.length);
		System.arraycopy(fragments, 0, choices, plugins.length, fragments.length);
		return choices;
	}

	public boolean finish() {
		final Object [] candidates = checkboxTablePart.getSelection();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doAdd(candidates, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			getContainer().run(false, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	protected abstract void doAdd(Object [] candidates, IProgressMonitor monitor) throws CoreException;
}