/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.core.util.ILRUCacheable;

/**
 * This class is used by
 * {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent}
 * for storing its references
 */
public class UseScanReferences implements ILRUCacheable, IReferenceCollection {

	Map<String, List<IReferenceDescriptor>> fReferencesMap;

	public UseScanReferences() {
		fReferencesMap = new LinkedHashMap<>();
	}

	@Override
	public void add(String type, IReferenceDescriptor refDesc) {
		List<IReferenceDescriptor> refDescList = fReferencesMap.get(type);
		if (refDescList == null) {
			refDescList = new ArrayList<>();
			fReferencesMap.put(type, refDescList);
		}
		if (!refDescList.contains(refDesc)) {
			refDescList.add(refDesc);
		}
	}

	@Override
	public boolean hasReferencesTo(String type) {
		List<IReferenceDescriptor> refDescList = fReferencesMap.get(type);
		return refDescList != null && refDescList.size() > 0;
	}

	@Override
	public IReferenceDescriptor[] getExternalDependenciesTo(String[] types) {
		if (types == null || types.length == 0) {
			return new IReferenceDescriptor[0];
		}

		List<IReferenceDescriptor> referenceDescriptorList = new ArrayList<>();
		for (String type : types) {
			List<IReferenceDescriptor> refDescs = fReferencesMap.get(type);
			if (refDescs == null || refDescs.isEmpty()) {
				continue;
			}
			referenceDescriptorList.addAll(refDescs);
		}

		return referenceDescriptorList.toArray(new IReferenceDescriptor[referenceDescriptorList.size()]);
	}

	@Override
	public IReferenceDescriptor[] getAllExternalDependencies() {
		List<IReferenceDescriptor> allRefDescs = new ArrayList<>();
		for (List<IReferenceDescriptor> refDescList : fReferencesMap.values()) {
			allRefDescs.addAll(refDescList);
		}
		return allRefDescs.toArray(new IReferenceDescriptor[allRefDescs.size()]);
	}

	@Override
	public int getCacheFootprint() {
		return fReferencesMap.size();
	}

	@Override
	public void clear() {
		fReferencesMap.clear();
	}
}
