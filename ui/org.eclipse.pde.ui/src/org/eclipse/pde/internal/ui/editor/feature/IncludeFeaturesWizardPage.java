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
package org.eclipse.pde.internal.ui.editor.feature;

import java.lang.reflect.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.ui.forms.internal.*;

public class IncludeFeaturesWizardPage extends WizardPage {
	public static final String KEY_TITLE = "FeatureEditor.IncludedFeaturesSection.new.title";
	public static final String KEY_DESC = "FeatureEditor.IncludedFeaturesSection.new.desc";
	public static final String KEY_FEATURES =
		"FeatureEditor.IncludedFeaturesSection.new.label";
	public static final String KEY_ADDING = "FeatureEditor.IncludedFeaturesSection.new.adding";
	public static final String KEY_UPDATING =
		"FeatureEditor.IncludedFeaturesSection.new.updating";
	private IFeatureModel model;
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
			super(PDEPlugin.getResourceString(KEY_FEATURES));
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
			viewer.setSorter(ListUtil.FEATURE_SORTER);
			return viewer;
		}
	}

	public IncludeFeaturesWizardPage(IFeatureModel model) {
		super("IncludeFeaturesPage");
		this.model = model;
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
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
		WorkbenchHelp.setHelp(container, IHelpContextIds.FEATURE_INCLUDED_FEATURES_WIZARD);
	}

	protected void createPluginList(Composite parent) {
		checkboxTablePart.createControl(parent);
		pluginViewer = checkboxTablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IFeatureModel) {
					IFeatureModel model = (IFeatureModel) object;
					return !isOnTheList(model);
				}
				return true;
			}
		});
		GridData gd = (GridData)checkboxTablePart.getControl().getLayoutData();
		gd.heightHint = 300;
	}

	private boolean isOnTheList(IFeatureModel candidate) {
		IFeatureChild[] features = model.getFeature().getIncludedFeatures();
		IFeature cfeature = candidate.getFeature();
		
		if (isThisModel(cfeature)) return true;

		for (int i = 0; i < features.length; i++) {
			IFeatureChild child = features[i];
			if (child.getId().equals(cfeature.getId()) &&
				child.getVersion().equals(cfeature.getVersion())) return true;
		}
		return false;
	}
	
	private boolean isThisModel(IFeature cfeature) {
		IFeature thisFeature = this.model.getFeature();
		
		return cfeature.getId().equals(thisFeature.getId()) &&
			cfeature.getVersion().equals(thisFeature.getVersion());
	}

	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		pluginViewer.setInput(model.getFeature());
		checkboxTablePart.setSelection(new Object[0]);
	}

	private Object[] getChoices() {
		NewWorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		return mng.getFeatureModels();
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

	private void doAdd(Object [] candidates, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_ADDING),
			candidates.length + 1);
		IFeature feature = model.getFeature();
		IFeatureChild[] added = new IFeatureChild[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			String name = candidate.getFeature().getLabel();
			monitor.subTask(candidate.getResourceString(name));
			FeatureChild child = (FeatureChild) model.getFactory().createChild();
			child.loadFrom(candidate.getFeature());
			added[i] = child;
			monitor.worked(1);
		}
		monitor.subTask("");
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		feature.addIncludedFeatures(added);
		monitor.worked(1);
	}
}
