/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocation;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.ui.IHelpContextIds;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


public class TargetSourceTab {	
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
	private String fLastUserPath = null;
	
	private TargetPlatformPreferencePage fPage = null;

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
				return getSourceLocations();
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
				return true;
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

	public TargetSourceTab(TargetPlatformPreferencePage page) {
		initializeImages();
		fSystemNode = new NamedElement(PDEUIMessages.SourceBlock_target); 
		fUserNode = new NamedElement(PDEUIMessages.SourceBlock_additional); 
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		fExtensionLocations = manager.getExtensionLocations();
		fUserLocations.addAll(Arrays.asList(manager.getUserLocations()));
		fPage = page;
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
		fTreeViewer.getTree().getItem(0).setExpanded(false);
		fTreeViewer.refresh(fSystemNode);
		fExtensionLocations = null;
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
		// fExtensionLocations either equal actual locations or null.  If null, by setting ExtensionLocations to null, the locations will be lazy loaded
		PDECore.getDefault().getSourceLocationManager().setExtensionLocations(fExtensionLocations);
		PDECore.getDefault().getJavadocLocationManager().reset();
		return true;
	}
	
	public void performDefaults() {
		fUserLocations.clear();
		fTreeViewer.refresh();
	}
	
	protected void loadTargetProfile(ITarget target) {
		// this tab should do nothing.  It may later be removed altogether
	}

	protected void handleAdd() {
		String path = getDirectoryDialog(fLastUserPath).open();
		if (path != null) {
			SourceLocation location = new SourceLocation(new Path(path));
			fUserLocations.add(location);
			fTreeViewer.add(fUserNode, location);
			fTreeViewer.setSelection(new StructuredSelection(location));
			fLastUserPath = path;
		}
	}
	
	private DirectoryDialog getDirectoryDialog(String filterPath) {
		DirectoryDialog dialog = new DirectoryDialog(fPage.getShell());
		dialog.setMessage(PDEUIMessages.SourcePreferencePage_dialogMessage); 
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

		Link text = new Link(container, SWT.WRAP);
		text.setText(PDEUIMessages.SourceBlock_desc); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		gd.widthHint = 400;
		text.setLayoutData(gd);
		text.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint("org.eclipse.pde.core.source"); //$NON-NLS-1$
			if (point != null)
				new ShowDescriptionAction(point, true).run();	
			}
		});

		fTreeViewer = new TreeViewer(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 1;
		gd.heightHint = 1;
		fTreeViewer.getTree().setLayoutData(gd);
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
		fAddButton.setText(PDEUIMessages.SourceBlock_add); 
		fAddButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		
		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.SourceBlock_remove); 
		fRemoveButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setEnabled(false);
		
		Dialog.applyDialogFont(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.SOURCE_PREFERENCE_PAGE);
		return container;
	}
	
	private SourceLocation[] getSourceLocations() {
		if (fExtensionLocations == null) {
			ArrayList result = new ArrayList();
			IExtension[] extensions = fPage.getExtensionRegistry().findExtensions(PDECore.PLUGIN_ID + ".source", false); //$NON-NLS-1$
			State resolverState = fPage.getCurrentState().getState();
			for (int i = 0; i < extensions.length; i++) {
				long id = Long.parseLong(((RegistryContributor)extensions[i].getContributor()).getId());
				BundleDescription desc = resolverState.getBundle(id);
				IPath path = new Path(desc.getLocation());
				IConfigurationElement[] children = extensions[i].getConfigurationElements();
				for (int j = 0; j < children.length; j++) {
					if (children[j].getName().equals("location")) { //$NON-NLS-1$
						String pathAttr = children[j].getAttribute("path"); //$NON-NLS-1$
						path.append(pathAttr);
						if (path.toFile().exists()) {
							SourceLocation location = new SourceLocation(path);
							location.setUserDefined(false);
							if (!result.contains(location))
								result.add(location);
						}
					}	
				}
			}
			fExtensionLocations = (SourceLocation[])result.toArray(new SourceLocation[result.size()]);
		}
		return fExtensionLocations;
	}

}
