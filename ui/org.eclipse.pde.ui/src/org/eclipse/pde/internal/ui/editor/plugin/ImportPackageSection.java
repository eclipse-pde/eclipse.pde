/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;

import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.osgi.framework.*;

public class ImportPackageSection extends TableSection implements IModelChangedListener {

    class PackageObject {
        String name;
        String version;
        boolean optional;
        ManifestElement element;
        
        public String toString() {
            StringBuffer buffer = new StringBuffer(name);
            if (version != null && version.length() > 0) {
                buffer.append(" ");
                boolean wrap = Character.isDigit(version.charAt(0));
                if (wrap)
                    buffer.append("(");
                buffer.append(version);
                if (wrap)
                    buffer.append(")");
            }
            return buffer.toString();
        }
        
        public String write() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(name);
            if (version != null && version.length() > 0) {
                buffer.append(";");
                buffer.append(getVersionAttribute());
                buffer.append("=\"");
                buffer.append(version.trim());
                buffer.append("\"");
            }
            if (optional) {
                buffer.append(";");
                buffer.append(Constants.RESOLUTION_DIRECTIVE);
                buffer.append(":=");
                buffer.append(Constants.RESOLUTION_OPTIONAL);
            }
            if (element == null)
                return buffer.toString();
            
            Enumeration attrs = element.getKeys();
            if (attrs != null) {
                while (attrs.hasMoreElements()) {
                    String attr = attrs.nextElement().toString();
                    if (attr.equals(getVersionAttribute()))
                        continue;
                    buffer.append(";");
                    buffer.append(attr);
                    buffer.append("=\"");
                    buffer.append(element.getAttribute(attr));
                    buffer.append("\"");
                }
            }
            Enumeration directives = element.getDirectiveKeys();
            if (directives != null) {
                while (directives.hasMoreElements()) {
                    String directive = directives.nextElement().toString();
                    if (directive.equals(Constants.RESOLUTION_DIRECTIVE))
                        continue;
                    buffer.append(";");
                    buffer.append(directive);
                    buffer.append(":=");
                    buffer.append("\"");
                    buffer.append(element.getDirective(directive));
                    buffer.append("\"");
                }
            }
            return buffer.toString();
        }
    }
    
	class ImportPackageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fPackages == null)
				createImportObjects();
			return fPackages.values().toArray();
		}

		private void createImportObjects() {
			fPackages = new TreeMap();
			try {
				String value = getBundle().getHeader(Constants.IMPORT_PACKAGE);
				if (value != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, value);
					for (int i = 0; i < elements.length; i++) {
						PackageObject p = new PackageObject();
                        p.name = elements[i].getValue();
                        p.version = elements[i].getAttribute(getVersionAttribute());
                        p.optional = Constants.RESOLUTION_OPTIONAL.equals(elements[i].getDirective(Constants.RESOLUTION_DIRECTIVE));
                        p.element = elements[i];
                        fPackages.put(p.name, p);
					}
				}
			} catch (BundleException e) {
			}
        }
	}

	class ImportPackageLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}
	}

	class ImportPackageDialogLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}

		public String getText(Object element) {
			ExportPackageDescription p = (ExportPackageDescription) element;
            StringBuffer buffer = new StringBuffer(p.getName());
            String version = p.getVersion().toString();
            if (!version.equals(Version.emptyVersion.toString())) {
                buffer.append(" (");
                buffer.append(version);
                buffer.append(")");
            }
			return buffer.toString();
		}
	}

    private TableViewer fPackageViewer;

    private Map fPackages;

	public ImportPackageSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[] {"Add...", "Remove"}); 
		getSection().setText("Required Packages"); 
		getSection()
				.setDescription("You can specify packages this plug-in depends on without explicitly restricting what plug-ins they must come from."); 
		getTablePart().setEditable(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPackageViewer = tablePart.getTableViewer();
		fPackageViewer
				.setContentProvider(new ImportPackageContentProvider());
		fPackageViewer.setLabelProvider(new ImportPackageLabelProvider());
		fPackageViewer.setSorter(new ViewerSorter() {
            public int compare(Viewer viewer, Object e1, Object e2) {
                String s1 = e1.toString();
                String s2 = e2.toString();
                if (s1.indexOf(" ") != -1)
                    s1 = s1.substring(0, s1.indexOf(" "));
                if (s2.indexOf(" ") != -1)
                    s2 = s2.substring(0, s2.indexOf(" "));
                return super.compare(viewer, s1, s2);
            }
        });
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		initialize();
	}

	protected void selectionChanged(IStructuredSelection sel) {
		Object item = sel.getFirstElement();
		getTablePart().setButtonEnabled(1, item != null);
		getTablePart().setButtonEnabled(2, item != null);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}

	/**
	 * 
	 */
	protected void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) fPackageViewer
				.getSelection();
		Object[] items = ssel.toArray();
		fPackageViewer.remove(items);
		removeImportPackages(items);

	}

	/**
	 * @param items
	 */
	private void removeImportPackages(Object[] removed) {
		for (int k = 0; k < removed.length; k++) {
			PackageObject p = (PackageObject) removed[k];
			fPackages.remove(p.name);
		}
		writeImportPackages();
	}

	/**
	 * 
	 */
	protected void handleAdd() {
       ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                PDEPlugin.getActiveWorkbenchShell(), 
                new ImportPackageDialogLabelProvider());
        dialog.setElements(getAvailablePackages());
        dialog.setMultipleSelection(true);
        dialog.setMessage("Packages exported by other plug-ins:");
        dialog.setTitle("Package Selection");
        dialog.create();
        SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == ElementListSelectionDialog.OK) {
			Object[] selected = dialog.getResult();
			for (int i = 0; i < selected.length; i++) {
				ExportPackageDescription candidate = (ExportPackageDescription) selected[i];
				PackageObject p = new PackageObject();
				p.name = candidate.getName();
                String version = candidate.getVersion().toString();
                if (!version.equals(Version.emptyVersion.toString()))
                    p.version = candidate.getVersion().toString();
                p.optional = Constants.RESOLUTION_OPTIONAL.equals(candidate.getDirective(Constants.RESOLUTION_DIRECTIVE));
			}
			if (selected.length > 0) {
				writeImportPackages();
			}
		}
	}

	public void addImportPackage(PackageObject p) {
		fPackages.put(p.name, p);
		fPackageViewer.add(p);
        writeImportPackages();
	}

	private void writeImportPackages() {
		StringBuffer buffer = new StringBuffer();
		if (fPackages != null) {
            Iterator iter = fPackages.values().iterator();
			while (iter.hasNext()) {
				buffer.append(((PackageObject)iter.next()).write());
				if (iter.hasNext()) {
					buffer.append("," + System.getProperty("line.separator") + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		}
		getBundle().setHeader(Constants.IMPORT_PACKAGE, buffer.toString());
	}

	private ExportPackageDescription[] getAvailablePackages() {
		ArrayList result = new ArrayList();
        String id = getId();
        
        //TODO add method to PluginModelManager
        PDEState state = PDECore.getDefault().getExternalModelManager().getState();
        ExportPackageDescription[] desc = state.getState().getExportedPackages();
        for (int i = 0; i < desc.length; i++) {
			if (desc[i].getExporter().getSymbolicName().equals(id))
                continue;
			if (fPackages != null && !fPackages.containsKey(desc[i].getName()))
				result.add(desc[i]);			
		}
		return (ExportPackageDescription[])result.toArray(new ExportPackageDescription[result.size()]);
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		fPackageViewer.setInput(model.getPluginBase());

		getBundleModel().addModelChangedListener(this);
		getTablePart().setButtonEnabled(0, true);
		getTablePart().setButtonEnabled(1, false);

	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
	}

	public void refresh() {
		fPackages = null;
		fPackageViewer.refresh();
		super.refresh();
	}

	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}

    private BundleInputContext getBundleContext() {
        InputContextManager manager = getPage().getPDEEditor().getContextManager();
        return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
    }
    
    private IBundleModel getBundleModel() {
        BundleInputContext context = getBundleContext();
        return (context != null) ? (IBundleModel)context.getModel() : null;
        
    }
    
    private IBundle getBundle() {
        IBundleModel model = getBundleModel();
         return (model != null) ? model.getBundle() : null;
    }
    
    private String getVersionAttribute() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? Constants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
    
    private String getId() {
        String value = getBundle().getHeader(Constants.BUNDLE_SYMBOLICNAME);
        try {
            ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, value);
            if (elements.length > 0)
                return elements[0].getValue();
        } catch (BundleException e) {
        }
        return null;
    }

}
