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
package org.eclipse.pde.internal.ui.preferences;

import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.ui.help.WorkbenchHelp;
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
	private Image extensionImage;
	private Image userImage;
	private Preferences preferences;
	private SourceLocation[] extensionLocations = new SourceLocation[0];
	private ArrayList userLocations = new ArrayList();

	class SourceProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			return getLocations();
		}
	}

	class SourceLabelProvider extends LabelProvider {
		
		public String getText(Object obj) {
			SourceLocation location = (SourceLocation) obj;
			return location.getName() + " - " + location.getPath().toOSString();
		}

		public Image getImage(Object obj) {
			SourceLocation location = (SourceLocation) obj;
			return (location.isUserDefined()) ? userImage : extensionImage;
		}
	}

	class LocationPart extends CheckboxTablePart {
		public LocationPart(String[] buttonLabels) {
			super(buttonLabels);
		}
		protected void buttonSelected(Button button, int index) {
			switch (index) {
				case 0 :
					handleAdd();
					break;
				case 1 :
					handleDelete();
					break;
				case 2 :
					selectAll(true);
					break;
				case 3 :
					selectAll(false);
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
			tablePart.setButtonEnabled(1, enabled);
		}
		/**
		 * @see org.eclipse.pde.internal.ui.parts.CheckboxTablePart#elementChecked(Object, boolean)
		 */
		protected void elementChecked(Object element, boolean checked) {
			((SourceLocation)element).setEnabled(checked);
		}

	}

	public SourcePreferencePage() {
		tablePart =
			new LocationPart(
				new String[] {
					PDEPlugin.getResourceString(KEY_ADD),
					PDEPlugin.getResourceString(KEY_DELETE),
					PDEPlugin.getResourceString(KEY_SELECT_ALL),
					PDEPlugin.getResourceString(KEY_DESELECT_ALL)
					});
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
		preferences = PDECore.getDefault().getPluginPreferences();
		initializeExtensionLocations();
		initializeUserlocations();

	}

	private void initializeExtensionLocations() {
		extensionLocations = PDECore.getDefault().getSourceLocationManager().getExtensionLocations();
	}
	
	private void initializeUserlocations() {
		userLocations = PDECore.getDefault().getSourceLocationManager().getUserLocationArray();
	}
	
	private String encodeSourceLocations(Object[] locations) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < locations.length; i++) {
			SourceLocation loc = (SourceLocation) locations[i];
			if (i > 0)
				buf.append(File.pathSeparatorChar);
			buf.append(encodeSourceLocation(loc));
		}
		return buf.toString();
	}

	private String encodeSourceLocation(SourceLocation location) {
		return location.getName()
			+ "@"
			+ location.getPath().toOSString()
			+ ","
			+ (location.isEnabled() ? "t" : "f");
	}
	
	public void dispose() {
		super.dispose();
		userImage.dispose();
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		//initializeExtensionLocations();
		//initializeUserlocations();
	}

	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		preferences.setValue(ICoreConstants.P_EXT_LOCATIONS, encodeSourceLocations(extensionLocations));
		preferences.setValue(ICoreConstants.P_SOURCE_LOCATIONS, encodeSourceLocations(userLocations.toArray()));
		PDECore.getDefault().savePluginPreferences();
		PDECore.getDefault().getSourceLocationManager().initializeClasspathVariables(null);
		return super.performOk();
	}

	public void performDefaults() {
		for (int i = 0; i < extensionLocations.length; i++) {
			SourceLocation location = (SourceLocation)extensionLocations[i];
			location.setEnabled(true);
			tableViewer.setChecked(location, true);
		}
		for (int i = 0; i < userLocations.size(); i++) {
			SourceLocation location = (SourceLocation)userLocations.get(i);
			location.setEnabled(false);
			tableViewer.setChecked(location, false);
		}
		tableViewer.refresh();
		super.performDefaults();
	}

	private Object[] getLocations() {
		Object[] merged =
			new Object[extensionLocations.length + userLocations.size()];
		System.arraycopy(
			extensionLocations,
			0,
			merged,
			0,
			extensionLocations.length);
		System.arraycopy(
			userLocations.toArray(),
			0,
			merged,
			extensionLocations.length,
			userLocations.size());
		return merged;
	}

	private void selectAll(boolean selected) {
		for (int i = 0; i < extensionLocations.length; i++) {
			((SourceLocation)extensionLocations[i]).setEnabled(selected);
		}
		for (int i = 0; i < userLocations.size(); i++) {
			((SourceLocation)userLocations.get(i)).setEnabled(selected);
		}
		tableViewer.setAllChecked(selected);
	}

	private void handleAdd() {
		SourceLocationDialog dialog =
			new SourceLocationDialog(getShell(), null);
		dialog.create();
		dialog.setInvalidNames(getAllLocationNames());
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
		tablePart.setButtonEnabled(4,false);
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
		tableViewer.setContentProvider(new SourceProvider());
		tableViewer.setLabelProvider(new SourceLabelProvider());
		tableViewer.setInput(this);
		initializeStates();
		tablePart.setButtonEnabled(4, false);
		Dialog.applyDialogFont(parent);
		WorkbenchHelp.setHelp(parent, IHelpContextIds.SOURCE_PREFERENCE_PAGE);
		return container;
	}
	
	private void initializeStates() {
		for (int i = 0; i < extensionLocations.length; i++) {
			SourceLocation loc = (SourceLocation) extensionLocations[i];
			tableViewer.setChecked(loc, loc.isEnabled());
		}
		for (int i = 0; i < userLocations.size(); i++) {
			SourceLocation loc = (SourceLocation) userLocations.get(i);
			tableViewer.setChecked(loc, loc.isEnabled());
		}
	}
	
	
	private HashSet getAllLocationNames() {
		HashSet set = new HashSet();
		for (int i = 0; i < extensionLocations.length; i++) {
			set.add(extensionLocations[i].getName());
		}
		for (int i = 0; i < userLocations.size(); i++) {
			set.add(((SourceLocation)userLocations.get(i)).getName());
		}
		return set;
	}
}