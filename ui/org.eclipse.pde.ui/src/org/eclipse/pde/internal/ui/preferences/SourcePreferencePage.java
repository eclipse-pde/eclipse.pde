package org.eclipse.pde.internal.ui.preferences;

import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.util.OverlayIcon;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
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
	private static final String KEY_LABEL = "SourcePreferencePage.label";
	private static final String KEY_ADD = "SourcePreferencePage.add";
	private static final String KEY_DELETE = "SourcePreferencePage.delete";
	private static final String KEY_DESC = "SourcePreferencePage.desc";
	private TablePart tablePart;
	private TableViewer tableViewer;
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
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			SourceLocation location = (SourceLocation) obj;
			if (location.isUserDefined())
				return userImage;
			else
				return extensionImage;
		}
	}

	class LocationPart extends TablePart {
		public LocationPart(String[] buttonLabels) {
			super(buttonLabels);
		}
		protected void buttonSelected(Button button, int index) {
			if (index == 0)
				handleAdd();
			else if (index == 1)
				handleDelete();
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
			tablePart.setButtonEnabled(1, enabled);
		}
	}

	public SourcePreferencePage() {
		tablePart =
			new LocationPart(
				new String[] {
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
			PDEPlugin.getDefault().getSourceLocationManager();
		userLocations = (ArrayList) mng.getUserLocationArray().clone();
	}

	private void store() {
		SourceLocationManager mng =
			PDEPlugin.getDefault().getSourceLocationManager();
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
		super.performDefaults();
	}

	private Object[] getLocations() {
		SourceLocationManager mng =
			PDEPlugin.getDefault().getSourceLocationManager();
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

	private void handleAdd() {
		DirectoryDialog dd =
			new DirectoryDialog(tableViewer.getControl().getShell());
		String path = dd.open();
		if (path != null) {
			SourceLocation location = new SourceLocation(new Path(path));
			userLocations.add(location);
			tableViewer.add(location);
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
		tablePart.createControl(container, SWT.BORDER, 2, null);
		tableViewer = tablePart.getTableViewer();
		tableViewer.setContentProvider(new SourceProvider());
		tableViewer.setLabelProvider(new SourceLabelProvider());
		load();
		tableViewer.setInput(this);
		tablePart.setButtonEnabled(1, false);
		return container;
	}
}