/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.Comparator;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class FilteredSchemaAttributeSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.pde.ui.dialogs.FilteredSchemaAttributeSelectionDialog"; //$NON-NLS-1$
	private static final String S_OPTIONAL_ATTRIBUTES = "showOptionalAttributes"; //$NON-NLS-1$

	private Action optionalAttributesAction = new ShowOptionalAttributesAction();
	private OptionalAttributesFilter optionalAttributesFilter = new OptionalAttributesFilter();
	private final SchemaListLabelProvider listLabelProvider = new SchemaListLabelProvider();
	private final SchemaDetailsLabelProvider detailsLabelProvider = new SchemaDetailsLabelProvider();

	private class ShowOptionalAttributesAction extends Action {

		public ShowOptionalAttributesAction() {
			super(PDEUIMessages.FilteredSchemaAttributeSelectionDialog_showOptionalAttributes, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

		public void run() {
			optionalAttributesFilter.setEnabled(isChecked());
			scheduleRefresh();
		}

	}

	private class OptionalAttributesFilter extends ViewerFilter {

		private boolean enabled = true;

		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (enabled) // select everything
				return true;

			if (element instanceof ISchemaAttribute) {
				ISchemaAttribute attribute = (ISchemaAttribute) element;
				return attribute.getUse() != ISchemaAttribute.OPTIONAL;
			}
			return true;
		}

		public void setEnabled(boolean value) {
			this.enabled = value;
		}
	}

	public FilteredSchemaAttributeSelectionDialog(Shell shell) {
		super(shell, false);

		setTitle(PDEUIMessages.FilteredSchemaAttributeSelectionDialog_title);
		setMessage(PDEUIMessages.FilteredSchemaAttributeSelectionDialog_message);

		PDEPlugin.getDefault().getLabelProvider().connect(this);
		setListLabelProvider(listLabelProvider);
		setListSelectionLabelDecorator(listLabelProvider);
		setDetailsLabelProvider(detailsLabelProvider);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.FILTERED_SCHEMA_ATTRIBUTE_SELECTION_DIALOG);
	}

	private class SchemaListLabelProvider extends LabelProvider implements ILabelDecorator {
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		public String getText(Object element) {
			if (element instanceof ISchemaAttribute) {
				ISchemaAttribute attribute = (ISchemaAttribute) element;
				if (isDuplicateElement(element)) {
					ISchemaObject object = attribute.getParent();
					if (object != null)
						return getQualifiedName(attribute);
				}
			}
			return PDEPlugin.getDefault().getLabelProvider().getText(element);
		}

		public Image decorateImage(Image image, Object element) {
			return image; // nothing to decorate
		}

		public String decorateText(String text, Object element) {
			if (element instanceof ISchemaAttribute) {
				ISchemaAttribute attribute = (ISchemaAttribute) element;
				ISchemaObject object = attribute.getParent();
				if (object != null && !isDuplicateElement(element))
					return getQualifiedName(attribute);
			}
			return text;
		}

	}

	private class SchemaDetailsLabelProvider extends LabelProvider {

		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_EXT_POINT_OBJ);
		}

		public String getText(Object element) {
			if (element instanceof ISchemaAttribute) {
				ISchemaAttribute attribute = (ISchemaAttribute) element;
				ISchema schema = attribute.getSchema();
				return schema.getPointId() + ' ' + '(' + schema.getPluginId() + ')';
			}
			return super.getText(element);
		}
	}

	private class SchemaItemsFilter extends ItemsFilter {

		public boolean isConsistentItem(Object item) {
			return true;
		}

		public boolean matchItem(Object item) {
			if (item instanceof ISchemaAttribute) {
				ISchemaAttribute attribute = (ISchemaAttribute) item;
				ISchemaObject object = attribute.getParent();
				ISchema schema = attribute.getSchema();
				String id = getQualifiedName(attribute);
				// match the attribute name, element name, qualified id or schema name
				return matches(attribute.getName()) || matches(object.getName()) || matches(id) || matches(schema.getName());
			}
			return false;
		}
	}

	private class SchemaComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			return 0;
		}

	}

	protected ItemsFilter createFilter() {
		return new SchemaItemsFilter();
	}

	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {

		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();

		// cycle through all active plug-ins and their extension points
		progressMonitor.beginTask(PDEUIMessages.FilteredSchemaAttributeSelectionDialog_searching, models.length);
		for (int i = 0; i < models.length; i++) {
			IPluginExtensionPoint[] points = models[i].getPluginBase().getExtensionPoints();

			for (int j = 0; j < points.length; j++) {
				String pointID = IdUtil.getFullId(points[j], models[i]);

				ISchema schema = registry.getSchema(pointID);
				if (schema == null) // if we don't find a schema
					continue;
				ISchemaElement[] elements = schema.getElements();

				for (int k = 0; k < elements.length; k++) {
					ISchemaElement element = elements[k];
					ISchemaAttribute[] attributes = element.getAttributes();

					for (int l = 0; l < attributes.length; l++) {
						ISchemaAttribute attribute = attributes[l];
						// only add attributes of the string kind and isn't translatable
						if (attribute.getKind() == IMetaAttribute.STRING && ISchemaAttribute.TYPES[ISchemaAttribute.STR_IND].equals(attribute.getType().getName()) && !attribute.isTranslatable())
							contentProvider.add(attribute, itemsFilter);
					}
				}
			}
			progressMonitor.worked(1);
		}

		progressMonitor.done();

	}

	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	public String getElementName(Object item) {
		if (item instanceof ISchemaAttribute) {
			ISchemaAttribute attribute = (ISchemaAttribute) item;
			return attribute.getName();
		}
		return null;
	}

	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);
		menuManager.add(new Separator());
		menuManager.add(optionalAttributesAction);
	}

	protected Comparator getItemsComparator() {
		return new SchemaComparator();
	}

	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, "org.eclipse.pde.ui", 0, "", null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		if (settings.get(S_OPTIONAL_ATTRIBUTES) != null) {
			boolean state = settings.getBoolean(S_OPTIONAL_ATTRIBUTES);
			optionalAttributesAction.setChecked(state);
		}

		addListFilter(optionalAttributesFilter);
		applyFilter();
	}

	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);
		settings.put(S_OPTIONAL_ATTRIBUTES, optionalAttributesAction.isChecked());
	}

	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	private String getQualifiedName(ISchemaAttribute attribute) {
		ISchemaObject object = attribute.getParent();
		ISchema schema = attribute.getSchema();
		return attribute.getName() + " - " + object.getName() + " [" + schema.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
