package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.editor.PropertiesAction;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.pde.internal.model.feature.FeaturePlugin;
import java.util.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.parts.TablePart;

public class PluginSection
	extends TableSection
	implements IModelProviderListener {
	private static final String PLUGIN_TITLE =
		"FeatureEditor.PluginSection.pluginTitle";
	private static final String PLUGIN_DESC =
		"FeatureEditor.PluginSection.pluginDesc";
	private static final String KEY_NEW = "FeatureEditor.PluginSection.new";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_OPEN = "Actions.open.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private boolean updateNeeded;
	private Object[] references;
	private OpenReferenceAction openAction;
	private PropertiesAction propertiesAction;
	private TableViewer pluginViewer;
	private Image pluginImage;
	private Image warningPluginImage;
	private Image fragmentImage;
	private Image warningFragmentImage;
	private Action newAction;
	private Action deleteAction;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getPlugins();
			}
			return new Object[0];
		}
	}

	class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof FeaturePlugin) {
				FeaturePlugin fref = (FeaturePlugin) obj;
				IPluginBase pluginBase = fref.getPluginBase();
				if (pluginBase != null) {
					return pluginBase.getTranslatedName() + " (" + pluginBase.getVersion() + ")";
				} else
					return obj.toString();
			}
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return getReferenceImage(obj);
		}
	}

	public PluginSection(FeatureReferencePage page) {
		super(page, new String[] { PDEPlugin.getResourceString(KEY_NEW)});
		setHeaderText(PDEPlugin.getResourceString(PLUGIN_TITLE));
		setDescription(PDEPlugin.getResourceString(PLUGIN_DESC));
		pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
		fragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
	}

	public void commitChanges(boolean onSave) {
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 9;

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		TablePart tablePart = getTablePart();
		pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(new PluginLabelProvider());
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

	private Image createWarningImage(
		ImageDescriptor baseDescriptor,
		ImageDescriptor overlayDescriptor) {
		ImageDescriptor desc =
			new OverlayIcon(baseDescriptor, new ImageDescriptor[][] { {
			}, {
			}, {
				overlayDescriptor }
		});
		return desc.createImage();
	}
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		if (warningPluginImage != null)
			warningPluginImage.dispose();
		if (warningFragmentImage != null)
			warningFragmentImage.dispose();
		super.dispose();
	}
	public void expandTo(Object object) {
		if (object instanceof IFeaturePlugin) {
			pluginViewer.setSelection(new StructuredSelection(object), true);
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(openAction);
		// add new
		manager.add(new Separator());
		manager.add(newAction);
		manager.add(deleteAction);
		// add delete
		manager.add(new Separator());
		manager.add(propertiesAction);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	}
	private Image getReferenceImage(Object obj) {
		if (!(obj instanceof FeaturePlugin))
			return null;
		FeaturePlugin fref = (FeaturePlugin) obj;
		IPluginBase pluginBase = fref.getPluginBase();
		if (pluginBase != null) {
			if (fref.isFragment())
				return fragmentImage;
			else
				return pluginImage;
		} else {
			if (warningFragmentImage == null)
				initializeOverlays();
			if (fref.isFragment())
				return warningFragmentImage;
			else
				return warningPluginImage;
		}
	}

	private void handleNew() {
		final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				NewFeaturePluginWizard wizard = new NewFeaturePluginWizard(model);
				WizardDialog dialog =
					new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				dialog.open();
			}
		});
	}
	private void handleSelectAll() {
		IStructuredContentProvider provider =
			(IStructuredContentProvider) pluginViewer.getContentProvider();
		Object[] elements = provider.getElements(pluginViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		pluginViewer.setSelection(ssel);
	}
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) pluginViewer.getSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();

		try {
			IFeaturePlugin[] removed = new IFeaturePlugin[ssel.size()];
			int i = 0;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				IFeaturePlugin iobj = (IFeaturePlugin) iter.next();
				removed[i++] = iobj;
			}
			feature.removePlugins(removed);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
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
		if (model.isEditable() == false) {
			pluginViewer.getTable().setEnabled(false);
		}
		model.addModelChangedListener(this);
		WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}
	private void initializeOverlays() {
		warningFragmentImage =
			createWarningImage(
				PDEPluginImages.DESC_FRAGMENT_OBJ,
				PDEPluginImages.DESC_ERROR_CO);
		warningPluginImage =
			createWarningImage(
				PDEPluginImages.DESC_PLUGIN_OBJ,
				PDEPluginImages.DESC_ERROR_CO);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
			if (getFormPage().isVisible()) {
				update();
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeaturePlugin) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					pluginViewer.update(obj, null);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					pluginViewer.add(e.getChangedObjects());
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					pluginViewer.remove(e.getChangedObjects());
				}
			}
		}
	}
	private void makeActions() {
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));

		deleteAction = new Action() {
			public void run() {
				BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		openAction = new OpenReferenceAction(pluginViewer);
		propertiesAction = new PropertiesAction(getFormPage().getEditor());
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (pluginViewer != null)
			pluginViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			references = null;
			this.update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		IFeature feature = model.getFeature();
		pluginViewer.setInput(feature);
		updateNeeded = false;
	}
}