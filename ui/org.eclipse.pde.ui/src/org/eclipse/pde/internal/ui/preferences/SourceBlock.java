/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocation;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.pde.internal.ui.util.OverlayIcon;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;


public class SourceBlock implements IHyperlinkListener {	
	private Image fFolderImage;
	private TreeViewer fTreeViewer;
	private Image fExtensionImage;
	private Image fUserImage;
	
	private SourceLocation[] fExtensionLocations = new SourceLocation[0];
	private ArrayList fUserLocations = new ArrayList();
	private NamedElement fSystemNode;
	private NamedElement fUserNode;
	private Button fAddButton;
	private Button fRemoveButton;
	
	class NamedElement {
		String text;
		public NamedElement(String text) {
			this.text = text;
		}
		public String toString() {
			return text;
		}
	}

	class SourceProvider extends DefaultContentProvider implements ITreeContentProvider {
		public Object[] getElements(Object input) {
			return new Object[] {fSystemNode, fUserNode};
		}

		public Object[] getChildren(Object element) {
			if (element.equals(fUserNode))
				return fUserLocations.toArray();
			if (element.equals(fSystemNode))
				return fExtensionLocations;
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof SourceLocation) {
				SourceLocation loc = (SourceLocation)element;
				return loc.isUserDefined() ? fUserNode : fSystemNode;
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element.equals(fSystemNode))
				return fExtensionLocations.length > 0;
			if (element.equals(fUserNode))
				return fUserLocations.size() > 0;
			return false;
		}
	}

	class SourceLabelProvider extends LabelProvider {		
		public String getText(Object obj) {
			if (obj instanceof SourceLocation) {
				SourceLocation location = (SourceLocation) obj;
				return location.getPath().toOSString();
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof SourceLocation)
				return fFolderImage;
		
			return obj.equals(fUserNode)? fUserImage : fExtensionImage;
		}	
	}

	public SourceBlock() {
		initializeImages();
		fSystemNode = new NamedElement(PDEUIMessages.SourceBlock_target); //$NON-NLS-1$
		fUserNode = new NamedElement(PDEUIMessages.SourceBlock_additional); //$NON-NLS-1$
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		fExtensionLocations = manager.getExtensionLocations();
		fUserLocations.addAll(Arrays.asList(manager.getUserLocations()));
	}
	
	private void initializeImages() {
		fExtensionImage = PDEPluginImages.DESC_SOURCE_ATTACHMENT_OBJ.createImage();
		ImageDescriptor userDesc =
			new OverlayIcon(
				PDEPluginImages.DESC_SOURCE_ATTACHMENT_OBJ,
				new ImageDescriptor[][] { { PDEPluginImages.DESC_DOC_CO }
		});
		fUserImage = userDesc.createImage();
		fFolderImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	}
	
	public void resetExtensionLocations(IPluginModelBase[] models) {
		fExtensionLocations = SourceLocationManager.computeSourceLocations(models);
		fTreeViewer.refresh(fSystemNode);
	}

	private String encodeSourceLocations() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < fUserLocations.size(); i++) {
			if (i > 0)
				buf.append(File.pathSeparatorChar);
			buf.append(((SourceLocation) fUserLocations.get(i)).getPath().toOSString());
		}
		return buf.toString();
	}
	
	public void dispose() {
		fExtensionImage.dispose();
		fUserImage.dispose();
	}

	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setValue(ICoreConstants.P_SOURCE_LOCATIONS, encodeSourceLocations());
		PDECore.getDefault().getSourceLocationManager().setExtensionLocations(fExtensionLocations);
		PDECore.getDefault().getJavadocLocationManager().reset();
		return true;
	}
	
	public void performDefaults() {
		fUserLocations.clear();
		fTreeViewer.refresh();
	}

	protected void handleAdd() {
		String path = getDirectoryDialog(null).open();
		if (path != null) {
			SourceLocation location = new SourceLocation(new Path(path));
			fUserLocations.add(location);
			fTreeViewer.add(fUserNode, location);
			fTreeViewer.setSelection(new StructuredSelection(location));
		}
	}
	
	private DirectoryDialog getDirectoryDialog(String filterPath) {
		DirectoryDialog dialog = new DirectoryDialog(PDEPlugin.getActiveWorkbenchShell());
		dialog.setMessage(PDEUIMessages.SourcePreferencePage_dialogMessage); //$NON-NLS-1$
		if (filterPath != null)
			dialog.setFilterPath(filterPath);
		return dialog;
	}

	protected void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		Object object = selection.getFirstElement();
		if (object instanceof SourceLocation) {
			SourceLocation location = (SourceLocation) object;
			if (location.isUserDefined()) {
				fUserLocations.remove(location);
				fTreeViewer.remove(location);
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
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());	
		FormText text = toolkit.createFormText(container, true);
		text.setText(PDEUIMessages.SourceBlock_desc, true, false); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL);
		gd.horizontalSpan = 2;
		text.setLayoutData(gd);
		text.setBackground(null);
		text.addHyperlinkListener(this);
		toolkit.dispose();

		fTreeViewer = new TreeViewer(container, SWT.BORDER);
		fTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		fTreeViewer.setContentProvider(new SourceProvider());
		fTreeViewer.setLabelProvider(new SourceLabelProvider());
		fTreeViewer.setInput(this);
		fTreeViewer.expandAll();
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ssel = (IStructuredSelection)event.getSelection();
				boolean removeEnabled = false;
				if (ssel != null && ssel.size() > 0) {
					Object object = ssel.getFirstElement();
					removeEnabled = (object instanceof SourceLocation && ((SourceLocation)object).isUserDefined());
				}
				fRemoveButton.setEnabled(removeEnabled);
			}
		});
		fTreeViewer.getTree().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					handleRemove();
				}
			}
		});	
	
		Composite buttonContainer = new Composite(container, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fAddButton = new Button(buttonContainer, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.SourceBlock_add); //$NON-NLS-1$
		fAddButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		
		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.SourceBlock_remove); //$NON-NLS-1$
		fRemoveButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setEnabled(false);
		
		Dialog.applyDialogFont(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.SOURCE_PREFERENCE_PAGE);
		return container;
	}

	public void linkEntered(HyperlinkEvent e) {
	}

	public void linkExited(HyperlinkEvent e) {
	}

	public void linkActivated(HyperlinkEvent e) {
		IPluginExtensionPoint point = PDECore.getDefault().findExtensionPoint("org.eclipse.pde.core.source"); //$NON-NLS-1$
		if (point != null)
			new ShowDescriptionAction(point, true).run();
	}
	
}
