package org.eclipse.e4.tools.emf.ui.internal.common.uistructure;

import java.util.Collection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;

public class WidgetContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	@Override
	public Object[] getElements(Object inputElement) {
		return ((Collection<?>) inputElement).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Menu m) {
			MenuItem[] items = m.getItems();
			Object[] rv = new Object[items.length];
			System.arraycopy(items, 0, rv, 0, rv.length);
			return rv;
		} else if (parentElement instanceof MenuItem item) {
			if (item.getMenu() != null) {
				MenuItem[] items = item.getMenu().getItems();
				Object[] rv = new Object[items.length];
				System.arraycopy(items, 0, rv, 0, rv.length);
				return rv;
			}
		} else if (parentElement instanceof ToolBar toolbar) {
			ToolItem[] items = toolbar.getItems();
			Object[] rv = new Object[items.length];
			System.arraycopy(items, 0, rv, 0, rv.length);
			return rv;
		} else if (parentElement instanceof CoolBar coolbar) {
			CoolItem[] items = coolbar.getItems();
			Object[] rv = new Object[items.length];
			System.arraycopy(items, 0, rv, 0, rv.length);
			return rv;
		} else if (parentElement instanceof TabItem item) {
			if (item.getControl() != null) {
				return new Object[] { item.getControl() };
			}
		} else if (parentElement instanceof CTabItem item) {
			if (item.getControl() != null) {
				return new Object[] { item.getControl() };
			}
		} else if (parentElement instanceof Tree) {
			// No children

		} else if (parentElement instanceof List) {
			// No children

		} else if (parentElement instanceof Combo) {
			// No children

		} else if (parentElement instanceof CTabFolder tabFolder) {
			CTabItem[] items = tabFolder.getItems();
			Object[] rv = new Object[items.length];
			System.arraycopy(items, 0, rv, 0, rv.length);
			return rv;
		} else if (parentElement instanceof TabFolder tabFolder) {
			TabItem[] items = tabFolder.getItems();
			Object[] rv = new Object[items.length];
			System.arraycopy(items, 0, rv, 0, rv.length);
			return rv;
		} else if (parentElement instanceof Composite comp) {
			Control controls[] = comp.getChildren();
			Object[] rv = new Object[controls.length];
			System.arraycopy(controls, 0, rv, 0, rv.length);
			return rv;
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

}
