/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.pluginsview;

import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.viewers.IBasicPropertyConstants;

public class PluginsViewSorter extends ViewerSorter {
	public boolean isSorterProperty(Object element, Object propertyId) {
		return propertyId.equals(IBasicPropertyConstants.P_TEXT);
	}
	
	public int category(Object element) {
		if (element instanceof ParentElement) {
			ParentElement pe = (ParentElement)element;
			switch (pe.getId()) {
				case ParentElement.PLUGIN_PROFILES:
				return 4;
				case ParentElement.WORKSPACE_PLUGINS:
				return 6;
				case ParentElement.EXTERNAL_PLUGINS:
				return 8;
			}
		}
		return 2;
	}
}
