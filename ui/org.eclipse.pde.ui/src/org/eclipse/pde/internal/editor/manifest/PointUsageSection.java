package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.part.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.*;
import java.io.*;
import org.eclipse.jface.action.*;
import java.util.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.elements.DefaultTableProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.wizards.ListUtil;
import org.eclipse.pde.internal.parts.TablePart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.model.*;

public class PointUsageSection extends TableSection {
	public static final String SECTION_TITLE =
		"ManifestEditor.PointUsageSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.PointUsageSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.PointUsageSection.fdesc";
	public static final String POPUP_OPEN = "Actions.open.label";
	private FormWidgetFactory factory;
	private TableViewer tableViewer;
	private boolean needsUpdate = false;

	class UsageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginExtensionPoint)
				return getReferencedPlugins((IPluginExtensionPoint) parent);
			else
				return new Object[0];
		}
	}

	public PointUsageSection(ManifestExtensionPointPage page) {
		super(page, null);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
	}

	private void addReferencingPlugins(
		String pluginId,
		String fullPointId,
		IPluginModelBase[] models,
		Vector result) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (model.isEnabled() == false)
				continue;
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase.getId().equals(pluginId))
				continue;
			if (testUsage(pluginBase, fullPointId)) {
				result.addElement(pluginBase);
			}
		}
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = createClientContainer(parent, 2, factory);
		TablePart part = getTablePart();
		createViewerPartControl(container, SWT.SINGLE, 2, factory);
		tableViewer = part.getTableViewer();
		tableViewer.setContentProvider(new UsageContentProvider());
		tableViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		tableViewer.setSorter(ListUtil.NAME_SORTER);
		factory.paintBordersFor(container);
		return container;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}

	private Object[] getReferencedPlugins(IPluginExtensionPoint currentPoint) {
		Vector result = new Vector();
		if (currentPoint == null)
			return new Object[0];

		String fullPointId = currentPoint.getFullId();

		// Test this plugin
		Object model = getFormPage().getModel();
		IPluginBase thisPluginBase = ((IPluginModelBase) model).getPluginBase();
		if (testUsage(thisPluginBase, fullPointId)) {
			result.add(thisPluginBase);
		}

		WorkspaceModelManager manager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		addReferencingPlugins(
			thisPluginBase.getId(),
			fullPointId,
			manager.getWorkspacePluginModels(),
			result);

		ExternalModelManager registry =
			PDEPlugin.getDefault().getExternalModelManager();
		addReferencingPlugins(
			thisPluginBase.getId(),
			fullPointId,
			registry.getModels(),
			result);
		return result.toArray();
	}

	protected void fillContextMenu(IMenuManager manager) {
		final IStructuredSelection sel =
			(IStructuredSelection) tableViewer.getSelection();
		if (!sel.isEmpty()) {
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_OPEN)) {
				public void run() {
					handleOpen(sel);
				}
			});
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	}
	private void handleOpen(IStructuredSelection selection) {
		IPluginBase pluginToOpen = (IPluginBase) selection.getFirstElement();
		if (pluginToOpen != null) {
			((ManifestEditor) getFormPage().getEditor()).openPluginEditor(pluginToOpen);
		} else {
			Display.getCurrent().beep();
		}
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		model.addModelChangedListener(this);
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		inputChanged((IPluginExtensionPoint) changeObject);
	}

	private boolean testUsage(IPluginBase pluginBase, String pointId) {
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			if (extension.getPoint().equals(pointId)) {
				return true;
			}
		}
		return false;
	}

	private void inputChanged(IPluginExtensionPoint extensionPoint) {
		tableViewer.setInput(extensionPoint);
	}
	public void modelChanged(IModelChangedEvent event) {
		/*
		int type = event.getChangeType();
		if (type == IModelChangedEvent.WORLD_CHANGED)
			needsUpdate = true;
		else {
			Object[] objects = event.getChangedObjects();
			if (objects[0] instanceof IPluginImport) {
				needsUpdate = true;
			}
		}
		if (getFormPage().isVisible())
			update();
		*/
	}
	public void update() {
		tableViewer.refresh();
		needsUpdate = false;
	}
}