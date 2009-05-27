/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.FeatureURLElement;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.*;

public class URLSection extends TableSection {
	private TableViewer fUrlViewer;

	private Action fNewAction;

	private Action fDeleteAction;

	private Image fUrlImage;

	class URLContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			IFeature feature = (IFeature) input;
			IFeatureURL featureUrl = feature.getURL();
			if (featureUrl != null) {
				return featureUrl.getDiscoveries();
			}
			return new Object[0];
		}
	}

	class URLLabelProvider extends LabelProvider {

		public Image getImage(Object obj) {
			if (obj instanceof IFeatureURLElement) {
				return fUrlImage;
			}
			return null;
		}

	}

	public URLSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.NO_TITLE, false, new String[] {PDEUIMessages.FeatureEditor_URLSection_new});
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		fUrlImage = provider.get(PDEPluginImages.DESC_LINK_OBJ);
		createClient(getSection(), page.getManagedForm().getToolkit());

		getSection().setDescription(PDEUIMessages.FeatureEditor_URLSection_desc);
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 5;

		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		TablePart tablePart = getTablePart();
		fUrlViewer = tablePart.getTableViewer();
		fUrlViewer.setContentProvider(new URLContentProvider());
		fUrlViewer.setLabelProvider(new URLLabelProvider());
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		initialize();
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	protected void fillContextMenu(IMenuManager manager) {
		IModel model = (IModel) getPage().getModel();
		ISelection selection = fUrlViewer.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();

		manager.add(fNewAction);
		fNewAction.setEnabled(model.isEditable());

		if (object != null && object instanceof IFeatureURLElement) {
			manager.add(fDeleteAction);
			fDeleteAction.setEnabled(model.isEditable());
		}

		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleNew() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureURL url = feature.getURL();

		if (url == null) {
			url = model.getFactory().createURL();
			try {
				feature.setURL(url);
			} catch (CoreException e) {
				return;
			}
		}
		try {
			IFeatureURLElement element = model.getFactory().createURLElement(url, IFeatureURLElement.DISCOVERY);
			element.setLabel(PDEUIMessages.FeatureEditor_URLSection_newDiscoverySite);
			element.setURL(new URL(PDEUIMessages.FeatureEditor_URLSection_newURL));
			url.addDiscovery(element);
			fUrlViewer.setSelection(new StructuredSelection(element));

		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleSelectAll() {
		IStructuredContentProvider provider = (IStructuredContentProvider) fUrlViewer.getContentProvider();
		Object[] elements = provider.getElements(fUrlViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		fUrlViewer.setSelection(ssel);
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) fUrlViewer.getSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();

		IFeatureURL url = feature.getURL();
		if (url == null) {
			return;
		}
		for (Iterator iter = ssel.iterator(); iter.hasNext();) {
			IFeatureURLElement urlElement = (IFeatureURLElement) iter.next();
			// IFeature feature = urlElement.getFeature();
			try {
				url.removeDiscovery(urlElement);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fUrlViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			BusyIndicator.showWhile(fUrlViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleSelectAll();
				}
			});
			return true;
		}
		return false;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IFeatureURL) {
			markStale();
			return;
		}
		if (obj instanceof IFeatureURLElement) {
			markStale();
			return;
			// IFeatureURLElement element = (IFeatureURLElement) obj;
			// if (element.getElementType() == IFeatureURLElement.DISCOVERY) {
			// if (e.getChangeType() == IModelChangedEvent.INSERT) {
			// fUrlViewer.add(element);
			// fUrlViewer
			// .setSelection(new StructuredSelection(element),
			// true);
			// } else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			// fUrlViewer.remove(obj);
			// } else {
			// fUrlViewer.update(obj, null);
			// }
			// }
		}
	}

	private void makeActions() {
		IModel model = (IModel) getPage().getModel();
		fNewAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		fNewAction.setText(PDEUIMessages.Menus_new_label);
		fNewAction.setEnabled(model.isEditable());

		fDeleteAction = new Action() {
			public void run() {
				BusyIndicator.showWhile(fUrlViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		fDeleteAction.setText(PDEUIMessages.Actions_delete_label);
		fDeleteAction.setEnabled(model.isEditable());
	}

	public void setFocus() {
		if (fUrlViewer != null)
			fUrlViewer.getTable().setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fUrlViewer.setInput(feature);
		super.refresh();
	}

	public boolean canPaste(Clipboard clipboard) {
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null && objects.length > 0) {
			return canPaste(null, objects);
		}
		return false;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object,
	 *      Object[])
	 */
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof FeatureURLElement))
				return false;
		}
		return true;
	}

	protected void doPaste() {
		Clipboard clipboard = getPage().getPDEEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object,
	 *      Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof FeatureURLElement) {
				FeatureURLElement element = (FeatureURLElement) objects[i];
				element.setModel(model);
				element.setParent(feature);
				try {
					feature.getURL().addDiscovery(element);
				} catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		}
	}

	void fireSelection() {
		fUrlViewer.setSelection(fUrlViewer.getSelection());
	}
}
