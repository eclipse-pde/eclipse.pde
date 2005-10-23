/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class CompositeManifestHeader extends ManifestHeader {
	
	private static final PDEManifestElement[] NO_ELEMENTS = new PDEManifestElement[0];

	private static final long serialVersionUID = 1L;
	
	private boolean fSort;

	protected ArrayList fManifestElements;
	
	protected Map fElementMap;
	
	public CompositeManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		this(name, value, bundle, lineDelimiter, false);
	}
	
	public CompositeManifestHeader(String name, String value, IBundle bundle, String lineDelimiter, boolean sort) {
        fName = name;
        fBundle = bundle;
        fLineDelimiter = lineDelimiter;
        setModel(fBundle.getModel());
		fSort = sort;
		fValue = value;
        processValue(value);
	}
	
	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, value);
			for (int i = 0; i < elements.length; i++)
				addManifestElement(createElement(elements[i]), false);
		} catch (BundleException e) {
		}
	}
	
	protected PDEManifestElement createElement(ManifestElement element) {
		return new PDEManifestElement(this, element);
	}
	
	public void update() {
		update(false);
	}
	
	public void update(boolean notify) {
		StringBuffer sb = new StringBuffer();
		PDEManifestElement[] elements = getElements();
		for (int i = 0; i < elements.length; i++) {	
			if (sb.length() > 0) {
				sb.append(","); //$NON-NLS-1$
				sb.append(fLineDelimiter);
				sb.append(" "); //$NON-NLS-1$
			}
			sb.append(elements[i].write());
		}
		String old = fValue;
		fValue = sb.toString();
		if (notify)
			firePropertyChanged(this, fName, old, fValue);
	}
	
	protected void addManifestElement(String value) {
		addManifestElement(new PDEManifestElement(this, value));
	}
	
	protected void addManifestElement(PDEManifestElement element) {
		addManifestElement(element, true);
	}	
	
	protected void addManifestElement(PDEManifestElement element, boolean update) {
		if (fSort) {
			if (fElementMap == null)
				fElementMap = new TreeMap();
			fElementMap.put(element.getValue(), element);
		} else {
			if (fManifestElements == null)
				fManifestElements = new ArrayList(1);
			fManifestElements.add(element);
		}
		if (update) {
			update(false);
			fireStructureChanged(element, IModelChangedEvent.INSERT);
		}
	}
	
	protected Object removeManifestElement(PDEManifestElement element) {
		return removeManifestElement(element.getValue());
	}
	
	protected Object removeManifestElement(String name) {
		Object object = null;
		if (fSort) {
			if (fElementMap != null) {
				object = fElementMap.remove(name);
			}
		} else if (fManifestElements != null){
			for (int i = 0; i < fManifestElements.size(); i++) {
				PDEManifestElement element = (PDEManifestElement)fManifestElements.get(i);
				if (name.equals(element.getValue()))
					object = fManifestElements.remove(i);
			}
		}
		update(false);
		if (object instanceof BundleObject)
			fireStructureChanged((BundleObject)object, IModelChangedEvent.REMOVE);
		return object;
	}
	
	protected PDEManifestElement[] getElements() {
		if (fSort && fElementMap != null)
			return (PDEManifestElement[])fElementMap.values().toArray(new PDEManifestElement[fElementMap.size()]);
				
		if (fManifestElements != null)
			return (PDEManifestElement[])fManifestElements.toArray(new PDEManifestElement[fManifestElements.size()]);
		
		return NO_ELEMENTS; 
	}
	
   public boolean isEmpty() {
	   if (fSort)
		   return fElementMap == null || fElementMap.size() == 0;
	   return fManifestElements == null || fManifestElements.size() == 0;
   }
   
   public boolean hasElement(String name) {
	   if (fSort && fElementMap != null)
		   return fElementMap.containsKey(name);
	   
	   if (fManifestElements != null) {
			for (int i = 0; i < fManifestElements.size(); i++) {
				PDEManifestElement element = (PDEManifestElement)fManifestElements.get(i);
				if  (name.equals(element.getValue()))
					return true;
			}	   
	   }
	   return false;
   }
   
   public Vector getElementNames() {
	   PDEManifestElement[] elements = getElements();
       Vector vector = new Vector(elements.length);
       for (int i = 0; i < elements.length; i++) {
           vector.add(elements[i].getValue());
       }
       return vector;
   }
   
   public void swap(int index1, int index2) {
	   if (fSort || fManifestElements == null)
		   return;
	   int size = fManifestElements.size();
	   if (index1 >= 0 && index2 >= 0 && size > Math.max(index1, index2)) {
		   Object object1 = fManifestElements.get(index1);
		   Object object2 = fManifestElements.get(index2);
		   fManifestElements.set(index1, object2);
		   fManifestElements.set(index2, object1);
		   update(true);
	   }
   }
   
   protected PDEManifestElement getElementAt(int index) {
	   if (fManifestElements != null && fManifestElements.size() > index)
		   return (PDEManifestElement)fManifestElements.get(index);
	   return null;
   }



}
