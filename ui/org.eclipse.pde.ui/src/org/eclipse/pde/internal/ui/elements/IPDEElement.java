package org.eclipse.pde.internal.ui.elements;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*; 

public interface IPDEElement {

public Object[] getChildren();
public Image getImage();
public String getLabel();
public Object getParent();
}
