package org.eclipse.pde.internal.ui.preferences;

import java.util.*;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.CheckboxTablePart;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class SourcePreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {
	private static final String KEY_LABEL = "SourcePreferencePage.label"; //$NON-NLS-1$
	public static final String KEY_SELECT_ALL =
		"WizardCheckboxTablePart.selectAll"; //$NON-NLS-1$
	public static final String KEY_DESELECT_ALL =
		"WizardCheckboxTablePart.deselectAll"; //$NON-NLS-1$
	private static final String KEY_ADD = "SourcePreferencePage.add"; //$NON-NLS-1$
	private static final String KEY_DELETE = "SourcePreferencePage.delete"; //$NON-NLS-1$
	private static final String KEY_DESC = "SourcePreferencePage.desc"; //$NON-NLS-1$
	private CheckboxTablePart tablePart;
	private CheckboxTableViewer tableViewer;
	private ArrayList userLocations;
	private Image extensionImage;
	private Image userImage;

	class SourceProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			return getLocations();
		}
	}

	class SourceLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			SourceLocation location = (SourceLocation) obj;
			if (index == 0)
				return location.getName();
			if (index == 1)
				return location.getPath().toOSString();
			return ""; //$NON-NLS-1$
		}

		public Image getColumnImage(Object obj, int index) {
			if (index == 0) {
				SourceLocation location = (SourceLocation) obj;
				if (location.isUserDefined())
					return userImage;
				else
					return extensionImage;
			}
			return null;
		}
	}

	class LocationPart extends CheckboxTablePart {
		public LocationPart(String[] buttonLabels) {
			super(buttonLabels);
		}
		protected void buttonSelected(Button button, int index) {
			switch (index) {
				case 0 :
					selectAll(true);
					break;
				case 1 :
					selectAll(false);
					break;
				case 2 : // nothing
					break;
				case 3 :
					handleAdd();
					break;
				case 4 :
					handleDelete();
					break;
			}
		}
		protected Button createButton(
			Composite parent,
			String label,
			int index,
			FormWidgetFactory factory) {
			Button button = super.createButton(parent, label, index, factory);
			SWTUtil.setButtonDimensionHint(button);
			return button;
		}
		protected void createMainLabel(
			Composite parent,
			int span,
			FormWidgetFactory factory) {
			Label label = new Label(parent, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(KEY_LABEL));
			GridData gd = new GridData(GridData.FILL);
			gd.horizontalSpan = span;
			label.setLayoutData(gd);
		}
		protected void selectionChanged(IStructuredSelection selection) {
			boolean enabled = true;
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				SourceLocation location = (SourceLocation) obj;
				if (location.isUserDefined() == false) {
					enabled = false;
					break;
				}
			}
			tablePart.setButtonEnabled(4, enabled);
		}
	}

	public SourcePreferencePage() {
		tablePart =
			new LocationPart(
				new String[] {
					PDEPlugin.getResourceString(KEY_SELECT_ALL),
					PDEPlugin.getResourceString(KEY_DESELECT_ALL),
					null,
					PDEPlugin.getResourceString(KEY_ADD),
					PDEPlugin.getResourceString(KEY_DELETE)});
		extensionImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER);
		ImageDescriptor userDesc =
			new OverlayIcon(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
					ISharedImages.IMG_OBJ_FOLDER),
				new ImageDescriptor[][] { { PDEPluginImages.DESC_DOC_CO }
		});
		userImage = userDesc.createImage();
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}

	public void dispose() {
		super.dispose();
		userImage.dispose();
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	private void load() {
		SourceLocationManager mng =
			PDECore.getDefault().getSourceLocationManager();
		userLocations = (ArrayList) mng.getUserLocationArray().clone();
	}

	private void store() {
		SourceLocationManager mng =
			PDECore.getDefault().getSourceLocationManager();
		transferSelections();
		mng.setUserLocations(userLocations);
	}

	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		store();
		boolean value = super.performOk();
		return value;
	}

	public void performDefaults() {
		load();
		tableViewer.refresh();
		initializeStates();
		super.performDefaults();
	}

	private Object[] getLocations() {
		SourceLocationManager mng =
			PDECore.getDefault().getSourceLocationManager();
		Object[] extensionLocations = mng.getExtensionLocations();
		Object[] userArray = userLocations.toArray();
		Object[] merged =
			new Object[extensionLocations.length + userArray.length];
		System.arraycopy(
			extensionLocations,
			0,
			merged,
			0,
			extensionLocations.length);
		System.arraycopy(
			userArray,
			0,
			merged,
			extensionLocations.length,
			userArray.length);
		return merged;
	}

	private void selectAll(boolean selected) {
		tableViewer.setAllChecked(selected);
	}

	private void handleAdd() {
		SourceLocationDialog dialog =
			new SourceLocationDialog(getShell(), null);
		dialog.create();
		dialog.getShell().setText(PDEPlugin.getResourceString("SourcePreferencePage.new.title")); //$NON-NLS-1$
		SWTUtil.setDialogSize(dialog, 400, 200);
		if (dialog.open() == SourceLocationDialog.OK) {
			SourceLocation location =
				new SourceLocation(dialog.getName(), dialog.getPath(), true);
			userLocations.add(location);
			tableViewer.add(location);
			tableViewer.setChecked(location, location.isEnabled());
		}
	}

	private void handleDelete() {
		IStructuredSelection selection =
			(IStructuredSelection) tableViewer.getSelection();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			SourceLocation location = (SourceLocation) obj;
			if (location.isUserDefined()) {
				userLocations.remove(location);
				tableViewer.remove(location);
			}
		}
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		tablePart.setMinimumSize(150, 200);
		tablePart.createControl(container, SWT.BORDER, 2, null);
		tableViewer = tablePart.getTableViewer();
		configureColumns(tableViewer.getTable());
		tableViewer.setContentProvider(new SourceProvider());
		tableViewer.setLabelProvider(new SourceLabelProvider());
		load();
		tableViewer.setInput(this);
		initializeStates();
		tablePart.setButtonEnabled(4, false);
		return container;
	}
	
	private void configureColumns(Table table) {
		table.setHeaderVisible(true);
		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(PDEPlugin.getResourceString("SourcePreferencePage.column.name")); //$NON-NLS-1$
		
		column = new TableColumn(table, SWT.NULL);
		column.setText(PDEPlugin.getResourceString("SourcePreferencePage.column.path")); //$NON-NLS-1$
		
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(50, 100, true));
		layout.addColumnData(new ColumnWeightData(50, 100, true));
		table.setLayout(layout);
	}

	private void initializeStates() {
		SourceLocationManager mng =
			PDECore.getDefault().getSourceLocationManager();
		Object[] extensionLocations = mng.getExtensionLocations();
		ArrayList selected = new ArrayList();
		for (int i = 0; i < extensionLocations.length; i++) {
			SourceLocation loc = (SourceLocation) extensionLocations[i];
			if (loc.isEnabled())
				selected.add(loc);
		}
		for (int i = 0; i < userLocations.size(); i++) {
			SourceLocation loc = (SourceLocation) userLocations.get(i);
			if (loc.isEnabled())
				selected.add(loc);
		}
		tableViewer.setCheckedElements(selected.toArray());
	}
	private void transferSelections() {
		SourceLocationManager mng =
			PDECore.getDefault().getSourceLocationManager();
		Object[] extensionLocations = mng.getExtensionLocations();
		for (int i = 0; i < extensionLocations.length; i++) {
			SourceLocation loc = (SourceLocation) extensionLocations[i];
			loc.setEnabled(tableViewer.getChecked(loc));
		}
		for (int i = 0; i < userLocations.size(); i++) {
			SourceLocation loc = (SourceLocation) userLocations.get(i);
			loc.setEnabled(tableViewer.getChecked(loc));
		}
	}
}