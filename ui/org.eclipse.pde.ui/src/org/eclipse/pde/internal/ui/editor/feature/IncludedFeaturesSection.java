package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class IncludedFeaturesSection
	extends TableSection
	implements IModelProviderListener {
	private static final String SECTION_TITLE = "FeatureEditor.IncludedFeaturesSection.title";
	private static final String SECTION_DESC = "FeatureEditor.IncludedFeaturesSection.desc";
	private static final String KEY_NEW = "FeatureEditor.IncludedFeaturesSection.new";
	private static final String POPUP_NEW = "Menus.new.label";
	private static final String POPUP_DELETE = "Actions.delete.label";
	private boolean updateNeeded;
	private PropertiesAction propertiesAction;
	private TableViewer includesViewer;
	private Action newAction;
	private Action openAction;
	private Action deleteAction;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getIncludedFeatures();
			}
			return new Object[0];
		}
	}

	public IncludedFeaturesSection(FeatureAdvancedPage page) {
		super(page, new String[] { PDEPlugin.getResourceString(KEY_NEW)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
		//setCollapsable(true);
		IFeatureModel model = (IFeatureModel)page.getModel();
		IFeature feature = model.getFeature();
		//setCollapsed(feature.getData().length==0);
	}

	public void commitChanges(boolean onSave) {
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 9;

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		TablePart tablePart = getTablePart();
		includesViewer = tablePart.getTableViewer();
		includesViewer.setContentProvider(new PluginContentProvider());
		includesViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		includesViewer.setSorter(ListUtil.NAME_SORTER);
		factory.paintBordersFor(container);
		makeActions();
		return container;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		openAction.run();
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}
	public void expandTo(Object object) {
		if (object instanceof IFeatureChild) {
			includesViewer.setSelection(new StructuredSelection(object), true);
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(openAction);
		manager.add(new Separator());
		manager.add(newAction);
		manager.add(deleteAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}

	private void handleNew() {
		final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IResource resource = model.getUnderlyingResource();
		final IProject project = resource.getProject();

		BusyIndicator.showWhile(includesViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				IncludeFeaturesWizard wizard = new IncludeFeaturesWizard(model);
				WizardDialog dialog = new WizardDialog(includesViewer.getTable().getShell(), wizard);
				dialog.open();
			}
		});
	}
	private void handleSelectAll() {
		IStructuredContentProvider provider =
			(IStructuredContentProvider) includesViewer.getContentProvider();
		Object[] elements = provider.getElements(includesViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		includesViewer.setSelection(ssel);
	}
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) includesViewer.getSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();

		try {
			IFeatureChild[] removed = new IFeatureChild[ssel.size()];
			int i = 0;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				IFeatureChild iobj = (IFeatureChild) iter.next();
				removed[i++] = iobj;
			}
			feature.removeIncludedFeatures(removed);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			BusyIndicator.showWhile(includesViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator.showWhile(includesViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleSelectAll();
				}
			});
			return true;
		}
		return false;
	}
	protected void selectionChanged(IStructuredSelection selection) {
		getFormPage().setSelection(selection);
	}
	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
			if (getFormPage().isVisible()) {
				update();
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureChild) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					includesViewer.update(obj, null);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					includesViewer.add(e.getChangedObjects());
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					includesViewer.remove(e.getChangedObjects());
				}
			}
		}
	}
	private void makeActions() {
		IModel model = (IModel)getFormPage().getModel();
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
		newAction.setEnabled(model.isEditable());

		deleteAction = new Action() {
			public void run() {
				BusyIndicator.showWhile(includesViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		deleteAction.setEnabled(model.isEditable());
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		openAction = new OpenReferenceAction(includesViewer);
		propertiesAction = new PropertiesAction(getFormPage().getEditor());
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (includesViewer != null)
			includesViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		IFeature feature = model.getFeature();
		includesViewer.setInput(feature);
		updateNeeded = false;
	}
}