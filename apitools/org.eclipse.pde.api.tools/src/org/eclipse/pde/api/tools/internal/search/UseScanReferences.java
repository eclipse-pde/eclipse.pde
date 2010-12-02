/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.core.util.ILRUCacheable;

/**
 * This class is used by {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent} for storing
 * its references
 */
public class UseScanReferences implements ILRUCacheable, IReferenceCollection {

	Map fReferencesMap;

	public UseScanReferences() {
		fReferencesMap = new HashMap();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceCollection#add(java.lang.String, org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor)
	 */
	public void add(String type, IReferenceDescriptor refDesc) {
		List refDescList = (List) fReferencesMap.get(type);
		if (refDescList == null) {
			refDescList = new ArrayList();
			fReferencesMap.put(type, refDescList);
		}
		if (!refDescList.contains(refDesc)) {
			refDescList.add(refDesc);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceCollection#hasReferencesTo(java.lang.String)
	 */
	public boolean hasReferencesTo(String type) {
		List refDescList = (List) fReferencesMap.get(type);
		if (refDescList == null || refDescList.size() == 0) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceCollection#getExternalDependenciesTo(java.lang.String[])
	 */
	public IReferenceDescriptor[] getExternalDependenciesTo(String[] types) {
		if (types == null || types.length == 0) {
			return new IReferenceDescriptor[0];
		}
		
		List referenceDescriptorList = new ArrayList();
		for (int i = 0; i < types.length; i++) {
			List refDescs = (List) fReferencesMap.get(types[i]);
			if (refDescs == null || refDescs.size() == 0) {
				continue;
			}
			referenceDescriptorList.addAll(refDescs);			
		}

		return (IReferenceDescriptor[]) referenceDescriptorList.toArray(new IReferenceDescriptor[referenceDescriptorList.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.IReferenceCollection#getAllExternalDependencies()
	 */
	public IReferenceDescriptor[] getAllExternalDependencies() {
		List allRefDescs = new ArrayList();
		for (Iterator iterator = fReferencesMap.values().iterator(); iterator.hasNext();) {
			List refDescList = (List) iterator.next();
			allRefDescs.addAll(refDescList);
		}
		return (IReferenceDescriptor[]) allRefDescs.toArray(new IReferenceDescriptor[allRefDescs.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.core.util.ILRUCacheable#getCacheFootprint()
	 */
	public int getCacheFootprint() {
		return fReferencesMap.size();
	}

	public void clear() {
		fReferencesMap.clear();
	}
}
