/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionPointsSection extends TableSection {
	public static final String SECTION_TITLE = "ManifestEditor.DetailExtensionPointSection.title";
	public static final String SECTION_DESC = "ManifestEditor.DetailExtensionPointSection.desc";
	public static final String SECTION_NEW = "ManifestEditor.DetailExtensionPointSection.new";
	private TableViewer pointTable;
	
	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IPluginModelBase model = (IPluginModelBase)getPage().getModel();
			return model.getPluginBase().getExtensionPoints();
		}
	}
	
	public ExtensionPointsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[]{PDEPlugin
				.getResourceString(SECTION_NEW)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		handleDefaultButton = false;
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		TablePart tablePart = getTablePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		pointTable = tablePart.getTableViewer();
		pointTable.setContentProvider(new TableContentProvider());
		pointTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		pointTable.setInput(getPage());
		selectFirstExtensionPoint();
		IModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider)model).addModelChangedListener(this);
	}
	private void selectFirstExtensionPoint() {
		Table table = pointTable.getTable();
		TableItem [] items = table.getItems();
		if (items.length==0) return;
		TableItem firstItem = items[0];
		Object obj = firstItem.getData();
		pointTable.setSelection(new StructuredSelection(obj));
	}
	void fireSelection() {
		pointTable.setSelection(pointTable.getSelection());
	}
	public void dispose() {
		IModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider)model).removeModelChangedListener(this);
		super.dispose();
	}
	public void refresh() {
		pointTable.refresh();
		getForm().fireSelectionChanged(this, pointTable.getSelection());
	}
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtensionPoint) {
			pointTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}
	
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginExtensionPoint) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				pointTable.add(changeObject);
				pointTable.setSelection(
					new StructuredSelection(changeObject),
					true);
				pointTable.getTable().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				pointTable.remove(changeObject);
			} else {
				pointTable.update(changeObject, null);
			}
		}
	}
}