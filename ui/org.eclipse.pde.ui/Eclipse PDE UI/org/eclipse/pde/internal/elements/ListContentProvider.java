package org.eclipse.pde.internal.elements;

import java.util.*;
import org.eclipse.jface.viewers.*;

public class ListContentProvider extends DefaultContentProvider implements IStructuredContentProvider {

public ListContentProvider() {
	super();
}
public Object[] getElements(Object element) {
	if (element instanceof IPDEElement) {
		return ((IPDEElement)element).getChildren();
	}
	return null;
}
}
