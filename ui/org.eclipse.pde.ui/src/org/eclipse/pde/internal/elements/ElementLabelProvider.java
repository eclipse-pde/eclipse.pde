package org.eclipse.pde.internal.elements;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;

public class ElementLabelProvider extends LabelProvider {
	public static final ElementLabelProvider INSTANCE = new ElementLabelProvider();

public ElementLabelProvider() {
	super();
}
public Image getImage(Object element) {
   if (element instanceof IPDEElement) {
	   return ((IPDEElement)element).getImage();
   }
   return super.getImage(element);
}
public String getText(Object element) {
   if (element instanceof IPDEElement) {
	   return ((IPDEElement)element).getLabel();
   }
   return super.getText(element);
}
}
