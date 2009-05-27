/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * SchemaDescriptionRegistry
 *
 */
public class XMLComponentRegistry {

	private static XMLComponentRegistry fPinstance;

	/**
	 * Name value key
	 */
	public static final String F_NAME = "n"; //$NON-NLS-1$

	/**
	 * Description value key
	 */
	public static final String F_DESCRIPTION = "d"; //$NON-NLS-1$

	/**
	 * Identifier to access the registry dedicated to schema components
	 */
	public static final int F_SCHEMA_COMPONENT = 2;

	/**
	 * Identifier to access the registry dedicated to element components
	 */
	public static final int F_ELEMENT_COMPONENT = 4;

	/**
	 * Identifier to access the registry dedicated to attribute components
	 */
	public static final int F_ATTRIBUTE_COMPONENT = 8;

	private static ArrayList fConsumers = new ArrayList();

	private Map fSchemaComponentMap;

	private Map fAttributeComponentMap;

	private Map fElementComponentMap;

	private XMLComponentRegistry() {
		fSchemaComponentMap = Collections.synchronizedMap(new HashMap());
		fAttributeComponentMap = Collections.synchronizedMap(new HashMap());
		fElementComponentMap = Collections.synchronizedMap(new HashMap());
	}

	public static XMLComponentRegistry Instance() {
		if (fPinstance == null) {
			fPinstance = new XMLComponentRegistry();
		}
		return fPinstance;
	}

	public void dispose() {
		if (fSchemaComponentMap != null) {
			fSchemaComponentMap.clear();
		}
		if (fAttributeComponentMap != null) {
			fAttributeComponentMap.clear();
		}
		if (fElementComponentMap != null) {
			fElementComponentMap.clear();
		}
	}

	public void putDescription(String key, String value, int mapType) {
		putValue(F_DESCRIPTION, key, value, mapType);
	}

	public void putName(String key, String value, int mapType) {
		putValue(F_NAME, key, value, mapType);
	}

	private Map getTargetMap(int mask) {
		Map targetMap = null;
		if (mask == F_SCHEMA_COMPONENT) {
			targetMap = fSchemaComponentMap;
		} else if (mask == F_ATTRIBUTE_COMPONENT) {
			targetMap = fAttributeComponentMap;
		} else if (mask == F_ELEMENT_COMPONENT) {
			targetMap = fElementComponentMap;
		}
		return targetMap;
	}

	private void putValue(String valueKey, String key, String value, int mapType) {
		if (key != null) {
			Map targetMap = getTargetMap(mapType);
			if (targetMap == null) {
				return;
			}
			HashMap previousValue = (HashMap) targetMap.get(key);
			if (previousValue == null) {
				HashMap newValue = new HashMap();
				newValue.put(valueKey, value);
				targetMap.put(key, newValue);
			} else {
				previousValue.put(valueKey, value);
			}
		}
	}

	public void put(String key, HashMap value, int mapType) {
		if (key != null) {
			Map targetMap = getTargetMap(mapType);
			if (targetMap == null) {
				return;
			}
			targetMap.put(key, value);
		}
	}

	public HashMap get(String key, int mapType) {
		Map targetMap = getTargetMap(mapType);
		if (targetMap == null) {
			return null;
		}
		return (HashMap) targetMap.get(key);
	}

	private String getValue(String valueKey, String key, int mapType) {
		if (key != null) {
			HashMap map = get(key, mapType);
			if (map != null) {
				return (String) map.get(valueKey);
			}
		}
		return null;
	}

	public String getDescription(String key, int mapType) {
		return getValue(F_DESCRIPTION, key, mapType);
	}

	public String getName(String key, int mapType) {
		return getValue(F_NAME, key, mapType);
	}

	public void connect(Object consumer) {
		if (!fConsumers.contains(consumer)) {
			fConsumers.add(consumer);
		}
	}

	public void disconnect(Object consumer) {
		fConsumers.remove(consumer);
		if (fConsumers.size() == 0) {
			dispose();
		}
	}

}
