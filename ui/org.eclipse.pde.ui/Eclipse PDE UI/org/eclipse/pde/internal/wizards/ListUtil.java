package org.eclipse.pde.internal.wizards;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.graphics.*;


public class ListUtil {
	static class NameSorter extends ViewerSorter {
		public boolean isSorterProperty(Object element, Object propertyId) {
			return propertyId.equals(IBasicPropertyConstants.P_TEXT);
		}
	}

	public static final ViewerSorter NAME_SORTER = new NameSorter();

	static class TableLabelProvider extends ElementLabelProvider implements ITableLabelProvider {
		public String getColumnText(Object o, int index) {
			return getText(o);
		}
		public Image getColumnImage(Object o, int index) {
			return getImage(o);
		}
	}

	public static final ILabelProvider TABLE_LABEL_PROVIDER = new TableLabelProvider();

public ListUtil() {
	super();
}
}
