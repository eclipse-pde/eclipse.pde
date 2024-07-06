/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class CompositeManifestHeader extends ManifestHeader {

	private static final PDEManifestElement[] NO_ELEMENTS = new PDEManifestElement[0];

	private static final long serialVersionUID = 1L;

	private final boolean fSort;

	protected List<PDEManifestElement> fManifestElements;

	protected Map<String, PDEManifestElement> fElementMap;

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

	@Override
	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, value);
			if (elements != null) {
				for (ManifestElement element : elements) {
					addManifestElement(createElement(element), false);
				}
			}
		} catch (BundleException e) {
		}
	}

	protected PDEManifestElement createElement(ManifestElement element) {
		return new PDEManifestElement(this, element);
	}

	@Override
	public void update() {
		// let subclasses fire changes
		update(false);
	}

	@Override
	public void update(boolean notify) {
		StringBuilder sb = new StringBuilder();
		PDEManifestElement[] elements = getElements();
		for (PDEManifestElement element : elements) {
			if (sb.length() > 0) {
				sb.append(","); //$NON-NLS-1$
				sb.append(fLineDelimiter);
				sb.append(" "); //$NON-NLS-1$
			}
			sb.append(element.write());
		}
		String old = fValue;
		fValue = sb.toString();
		if (notify) {
			firePropertyChanged(this, fName, old, fValue);
		}
	}

	protected void addManifestElement(String value) {
		addManifestElement(new PDEManifestElement(this, value));
	}

	protected void addManifestElement(String value, int index) {
		PDEManifestElement element = new PDEManifestElement(this, value);
		addManifestElement(element, index, true);
	}

	protected void addManifestElement(PDEManifestElement element) {
		addManifestElement(element, true);
	}

	protected void addManifestElements(List<? extends PDEManifestElement> elements) {
		for (PDEManifestElement element : elements) {
			addManifestElement(element, false);
		}
		update(false);
		fireStructureChanged(elements.toArray(PDEManifestElement[]::new), IModelChangedEvent.INSERT);
	}

	protected void addManifestElement(PDEManifestElement element, boolean update) {
		element.setModel(getModel());
		element.setHeader(this);
		if (fSort) {
			if (fElementMap == null) {
				fElementMap = new TreeMap<>();
			}
			fElementMap.put(element.getValue(), element);
		} else {
			if (fManifestElements == null) {
				fManifestElements = new ArrayList<>(1);
			}
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
		} else if (fManifestElements != null) {
			for (int i = 0; i < fManifestElements.size(); i++) {
				PDEManifestElement element = fManifestElements.get(i);
				if (name.equals(element.getValue())) {
					object = fManifestElements.remove(i);
				}
			}
		}
		update(false);
		if (object instanceof BundleObject) {
			fireStructureChanged((BundleObject) object, IModelChangedEvent.REMOVE);
		}
		return object;
	}

	public PDEManifestElement[] getElements() {
		if (fSort && fElementMap != null) {
			return fElementMap.values().toArray(new PDEManifestElement[fElementMap.size()]);
		}

		if (fManifestElements != null) {
			return fManifestElements.toArray(new PDEManifestElement[fManifestElements.size()]);
		}

		return NO_ELEMENTS;
	}

	public boolean isEmpty() {
		if (fSort) {
			return fElementMap == null || fElementMap.isEmpty();
		}
		return fManifestElements == null || fManifestElements.isEmpty();
	}

	public boolean hasElement(String name) {
		if (fSort && fElementMap != null) {
			return fElementMap.containsKey(name);
		}

		if (fManifestElements != null) {
			for (PDEManifestElement element : fManifestElements) {
				if (name.equals(element.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getElementNames() {
		PDEManifestElement[] elements = getElements();
		return Arrays.stream(elements).map(PDEManifestElement::getValue).toList();
	}

	public void swap(int index1, int index2) {
		if (fSort || fManifestElements == null) {
			return;
		}
		int size = fManifestElements.size();
		if (index1 >= 0 && index2 >= 0 && size > Math.max(index1, index2)) {
			Collections.swap(fManifestElements, index1, index2);
			update(true);
		}
	}

	protected PDEManifestElement getElementAt(int index) {
		if (fManifestElements != null && fManifestElements.size() > index) {
			return fManifestElements.get(index);
		}
		return null;
	}

	/**
	 * Method not applicable for headers that are sorted
	 */
	public PDEManifestElement getPreviousElement(PDEManifestElement targetElement) {
		// Ensure we have elements
		if (fSort) {
			return null;
		} else if (fManifestElements == null) {
			return null;
		} else if (fManifestElements.size() <= 1) {
			return null;
		}
		// Get the index of the target element
		int targetIndex = fManifestElements.indexOf(targetElement);
		// Validate index
		if (targetIndex < 0) {
			// Target element does not exist
			return null;
		} else if (targetIndex == 0) {
			// Target element has no previous element
			return null;
		}
		// 1 <= index < size()
		// Get the previous element
		return fManifestElements.get(targetIndex - 1);
	}

	/**
	 * Method not applicable for headers that are sorted
	 */
	public PDEManifestElement getNextElement(PDEManifestElement targetElement) {
		// Ensure we have elements
		if (fSort) {
			return null;
		} else if (fManifestElements == null) {
			return null;
		} else if (fManifestElements.size() <= 1) {
			return null;
		}
		// Get the index of the target element
		int targetIndex = fManifestElements.indexOf(targetElement);
		// Get the index of the last element
		int lastIndex = fManifestElements.size() - 1;
		// Validate index
		if (targetIndex < 0) {
			// Target element does not exist
			return null;
		} else if (targetIndex >= lastIndex) {
			// Target element has no next element
			return null;
		}
		// 0 <= index < last element < size()
		// Get the next element
		return fManifestElements.get(targetIndex + 1);
	}

	/**
	 * Method not applicable for headers that are sorted
	 */
	protected void addManifestElement(PDEManifestElement element, int index, boolean update) {
		// Validate index
		int elementCount = 0;
		if (fManifestElements != null) {
			elementCount = fManifestElements.size();
		}
		// 0 <= index <= size()
		if (fSort) {
			return;
		} else if (index < 0) {
			return;
		} else if (index > elementCount) {
			return;
		}
		// Set element properties
		element.setModel(getModel());
		element.setHeader(this);
		// Add the element to the list
		if (fManifestElements == null) {
			// Initialize the element list if not defined
			fManifestElements = new ArrayList<>(1);
			// Add the element to the end of the list
			fManifestElements.add(element);
		} else {
			// Add the element to the list at the specified index
			fManifestElements.add(index, element);
		}
		// Fire event
		if (update) {
			update(false);
			fireStructureChanged(element, IModelChangedEvent.INSERT);
		}
	}

	/**
	 * Method not applicable for headers that are sorted
	 */
	public int indexOf(PDEManifestElement targetElement) {
		if (fSort) {
			// Elements are sorted. Position is irrelevant
			return -1;
		} else if (fManifestElements == null) {
			// No elements
			return -1;
		}
		return fManifestElements.indexOf(targetElement);
	}

}
