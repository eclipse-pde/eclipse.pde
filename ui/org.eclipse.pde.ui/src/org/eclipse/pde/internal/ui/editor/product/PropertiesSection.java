/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Displays a list of product properties set in the product configuration.  The properties
 * can be added, edited and removed.
 *
 * @see IConfigurationProperty
 * @see ConfigurationPage
 * @see ProductEditor
 *
 * @since 3.7
 */
public class PropertiesSection extends TableSection {

	private class ContentProvider implements IStructuredContentProvider {

		ContentProvider() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IProduct) {
				return ((IProduct) inputElement).getConfigurationProperties();
			}
			return new Object[0];
		}


	}

	private class LabelProvider extends PDELabelProvider {
		@Override
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return get(PDEPluginImages.DESC_PROPERTIES);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int index) {
			IConfigurationProperty configuration = (IConfigurationProperty) obj;
			switch (index) {
				case 0 :
					return configuration.getName();
					//return super.getColumnText(PluginRegistry.findModel(configuration.getId()), index);
				case 1 :
					return configuration.getValue();
				case 2 :
					return configuration.getOs();
				case 3 :
					return configuration.getArch();
			}
			return null;
		}

	}

	private class PropertyDialog extends StatusDialog {
		private Text fName;
		private Text fValue;
		private Combo fOS;
		private Combo fArch;
		private IConfigurationProperty fEdit;
		private Set<String> fExistingNames;

		private String[] COMBO_OSLABELS = new String[] { PDEUIMessages.PropertiesSection_All, Platform.OS_LINUX,
				Platform.OS_MACOSX, Platform.OS_WIN32 };
		private String[] COMBO_ARCHLABELS = new String[] { PDEUIMessages.PropertiesSection_All, Platform.ARCH_X86,
				Platform.ARCH_X86_64 };

		public PropertyDialog(Shell shell, IConfigurationProperty property, Set<String> existingNames) {
			super(shell);
			fEdit = property;
			fExistingNames = existingNames;
			setTitle(PDEUIMessages.PropertiesSection_PropertyDialogTitle);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite comp = (Composite) super.createDialogArea(parent);
			((GridLayout) comp.getLayout()).numColumns = 2;
			SWTFactory.createLabel(comp, PDEUIMessages.PropertiesSection_Name, 1);
			fName = SWTFactory.createSingleText(comp, 1);
			fName.addModifyListener(e -> validate());
			SWTFactory.createLabel(comp, PDEUIMessages.PropertiesSection_Value, 1);
			fValue = SWTFactory.createSingleText(comp, 1);
			fValue.addModifyListener(e -> validate());
			SWTFactory.createLabel(comp, PDEUIMessages.PropertiesSection_OS, 1);
			fOS = SWTFactory.createCombo(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, 1, COMBO_OSLABELS);
			fOS.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateStatus(Status.OK_STATUS);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					updateStatus(Status.OK_STATUS);
				}

			});

			SWTFactory.createLabel(comp, PDEUIMessages.PropertiesSection_Arch, 1);
			fArch = SWTFactory.createCombo(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, 1, COMBO_ARCHLABELS);
			fArch.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateStatus(Status.OK_STATUS);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					updateStatus(Status.OK_STATUS);
				}

			});

			if (fEdit != null) {
				if (fEdit.getName() != null) {
					fName.setText(fEdit.getName());
				}
				if (fEdit.getValue() != null) {
					fValue.setText(fEdit.getValue());
				}
				int index = Arrays.asList(COMBO_OSLABELS).indexOf(fEdit.getOs());
				if (index >= 0) {
					fOS.select(index);
				} else {
					fOS.select(0); // "All"
				}
				index = Arrays.asList(COMBO_ARCHLABELS).indexOf(fEdit.getArch());
				if (index >= 0) {
					fArch.select(index);
				} else {
					fArch.select(0); // "All"
				}
			}

			// Disable ok button on startup
			updateStatus(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, "")); //$NON-NLS-1$

			return comp;
		}

		protected void validate() {
			String name = fName.getText().trim();
			if (name.length() == 0) {
				updateStatus(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, PDEUIMessages.PropertiesSection_ErrorPropertyNoName));
			} else if (fExistingNames.contains(name)) {
				updateStatus(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, NLS.bind(PDEUIMessages.PropertiesSection_ErrorPropertyExists, name)));
			} else {
				updateStatus(Status.OK_STATUS);
			}
		}

		@Override
		protected void okPressed() {
			if (fEdit != null) {
				// Product properties are stored in a map that isn't updated on edit, remove the property and add a new one
				getProduct().removeConfigurationProperties(new IConfigurationProperty[] {fEdit});
			}

			IProductModelFactory factory = getModel().getFactory();
			fEdit = factory.createConfigurationProperty();
			fEdit.setName(fName.getText().trim());
			fEdit.setValue(fValue.getText().trim());
			int index = fOS.getSelectionIndex();
			fEdit.setOs(index == 0 ? "" : COMBO_OSLABELS[index]); //$NON-NLS-1$
			index = fArch.getSelectionIndex();
			fEdit.setArch(index == 0 ? "" : COMBO_ARCHLABELS[index]); //$NON-NLS-1$
			getProduct().addConfigurationProperties(new IConfigurationProperty[] {fEdit});
			super.okPressed();
		}

		@Override
		protected Control createHelpControl(Composite parent) {
			return parent;
		}

		/**
		 * @return a configuration property containing the values set in the dialog or <code>null</code>
		 */
		public IConfigurationProperty getResult() {
			return fEdit;
		}

	}

	class ValueCellModifier implements ICellModifier {
		@Override
		public boolean canModify(Object element, String property) {
			return element instanceof IConfigurationProperty;
		}

		@Override
		public Object getValue(Object element, String property) {
			return ((IConfigurationProperty) element).getValue();
		}

		@Override
		public void modify(Object item, String property, Object value) {
			Object data = ((TableItem) item).getData();
			if (data instanceof IConfigurationProperty) {
				String newValue = ((String) value).trim();
				((IConfigurationProperty) data).setValue(newValue);
				fPropertiesTable.refresh(data);
			}
		}
	}

	private TableViewer fPropertiesTable;

	public PropertiesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, getButtonLabels());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[3];
		labels[0] = PDEUIMessages.PropertiesSection_Add;
		labels[1] = PDEUIMessages.PropertiesSection_Edit;
		labels[2] = PDEUIMessages.PropertiesSection_Remove;
		return labels;
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleAdd();
				break;
			case 1 :
				handleEdit();
				break;
			case 2 :
				handleRemove();
				break;
		}
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.PropertiesSection_PropertiesSectionTitle);
		section.setDescription(PDEUIMessages.PropertiesSection_PropertiesSectionDescription);
		GridData sectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
		sectionData.horizontalSpan = 2;
		section.setLayoutData(sectionData);
		Composite container = createClientContainer(section, 3, toolkit);
		createViewerPartControl(container, SWT.MULTI | SWT.FULL_SELECTION, 3, toolkit);
		fPropertiesTable = getTablePart().getTableViewer();
		fPropertiesTable.setComparator(new ViewerComparator());
		fPropertiesTable.addDoubleClickListener(event -> handleEdit());
		fPropertiesTable.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					handleRemove();
				}
			}
		});

		final Table table = fPropertiesTable.getTable();

		final TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
		nameColumn.setText(PDEUIMessages.PropertiesSection_NameColumn);
		nameColumn.setWidth(160);

		final TableColumn valueColumn = new TableColumn(table, SWT.LEFT);
		valueColumn.setText(PDEUIMessages.PropertiesSection_ValueColumn);
		valueColumn.setWidth(220);

		final TableColumn osColumn = new TableColumn(table, SWT.LEFT);
		osColumn.setText(PDEUIMessages.PropertiesSection_OSColumn);
		osColumn.setWidth(60);

		final TableColumn archColumn = new TableColumn(table, SWT.LEFT);
		archColumn.setText(PDEUIMessages.PropertiesSection_ArchColumn);
		archColumn.setWidth(60);

		table.addControlListener(new ControlListener() {

			@Override
			public void controlMoved(ControlEvent e) {
			}

			@Override
			public void controlResized(ControlEvent e) {
				int size = table.getSize().x;
				nameColumn.setWidth(size / 9 * 3);
				valueColumn.setWidth(size / 9 * 4);
				osColumn.setWidth(size / 9 * 1);
				archColumn.setWidth(size / 9 * 1);
			}

		});

		TextCellEditor cellEditor = new TextCellEditor(table);
		cellEditor.getControl().pack();
		fPropertiesTable.setCellEditors(new CellEditor[] {null, cellEditor});
		fPropertiesTable.setColumnProperties(new String[] {"0", "1"}); // You must enter column properties to have cell editors  //$NON-NLS-1$//$NON-NLS-2$
		fPropertiesTable.setCellModifier(new ValueCellModifier());

		table.setHeaderVisible(true);
		toolkit.paintBordersFor(container);
		fPropertiesTable.setLabelProvider(new LabelProvider());
		fPropertiesTable.setContentProvider(new ContentProvider());
		fPropertiesTable.setInput(getProduct());

		section.setClient(container);
		getModel().addModelChangedListener(this);
		getTablePart().setButtonEnabled(0, isEditable());
		updateButtons();
	}

	private void handleAdd() {
		PropertyDialog dialog = new PropertyDialog(PDEPlugin.getActiveWorkbenchShell(), null, getExistingNames());
		if (dialog.open() == Window.OK) {
			IConfigurationProperty result = dialog.getResult();
			if (result != null) {
				fPropertiesTable.refresh();
				fPropertiesTable.setSelection(new StructuredSelection(result));
				updateButtons();
			}
		}
	}

	private void handleEdit() {
		IStructuredSelection ssel = fPropertiesTable.getStructuredSelection();
		if (!ssel.isEmpty() && ssel.getFirstElement() instanceof IConfigurationProperty) {
			IConfigurationProperty propertyToEdit = (IConfigurationProperty) ssel.getFirstElement();
			Set<String> existing = getExistingNames();
			existing.remove(propertyToEdit.getName());
			PropertyDialog dialog = new PropertyDialog(PDEPlugin.getActiveWorkbenchShell(), propertyToEdit, existing);
			if (dialog.open() == Window.OK) {
				IConfigurationProperty result = dialog.getResult();
				if (result != null) {
					fPropertiesTable.refresh();
					fPropertiesTable.setSelection(new StructuredSelection(result));
					updateButtons();
				}
			}
		}
	}

	/**
	 * @return A list of property names currently in use by the product, possibly empty
	 */
	private Set<String> getExistingNames() {
		Set<String> result = new HashSet<>();
		IConfigurationProperty[] properties = getProduct().getConfigurationProperties();
		for (IConfigurationProperty property : properties) {
			result.add(property.getName());
		}
		return result;
	}

	private void handleRemove() {
		IStructuredSelection ssel = fPropertiesTable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Object[] objects = ssel.toArray();
			IConfigurationProperty[] properties = new IConfigurationProperty[objects.length];
			System.arraycopy(objects, 0, properties, 0, objects.length);
			getProduct().removeConfigurationProperties(properties);
			fPropertiesTable.refresh(false);
		}
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		updateButtons();
	}

	/**
	 * @return the product object that is currently being edited
	 */
	private IProduct getProduct() {
		return getModel().getProduct();
	}

	/**
	 * @return the product model currently being edited
	 */
	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		fPropertiesTable.setInput(getProduct());
		fPropertiesTable.refresh();
		updateButtons();
	}

	private void updateButtons() {
		TablePart tablePart = getTablePart();
		ISelection selection = getViewerSelection();
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable() && !selection.isEmpty());
		tablePart.setButtonEnabled(2, isEditable() && !selection.isEmpty());
	}

}
