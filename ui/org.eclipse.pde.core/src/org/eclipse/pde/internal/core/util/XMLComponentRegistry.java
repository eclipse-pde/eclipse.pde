/*******************************************************************************
 *  Copyright (c) 2006, 2018 IBM Corporation and others.
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

	private static ArrayList<Object> fConsumers = new ArrayList<>();

	private final Map<String, HashMap<String, String>> fSchemaComponentMap;

	private final Map<String, HashMap<String, String>> fAttributeComponentMap;

	private final Map<String, HashMap<String, String>> fElementComponentMap;

	private XMLComponentRegistry() {
		fSchemaComponentMap = Collections.synchronizedMap(new HashMap<String, HashMap<String, String>>());
		fAttributeComponentMap = Collections.synchronizedMap(new HashMap<String, HashMap<String, String>>());
		fElementComponentMap = Collections.synchronizedMap(new HashMap<String, HashMap<String, String>>());
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

	private Map<String, HashMap<String, String>> getTargetMap(int mask) {
		Map<String, HashMap<String, String>> targetMap = null;
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
			Map<String, HashMap<String, String>> targetMap = getTargetMap(mapType);
			if (targetMap == null) {
				return;
			}
			HashMap<String, String> previousValue = targetMap.get(key);
			if (previousValue == null) {
				HashMap<String, String> newValue = new HashMap<>();
				newValue.put(valueKey, value);
				targetMap.put(key, newValue);
			} else {
				previousValue.put(valueKey, value);
			}
		}
	}

	public void put(String key, HashMap<String, String> value, int mapType) {
		if (key != null) {
			Map<String, HashMap<String, String>> targetMap = getTargetMap(mapType);
			if (targetMap == null) {
				return;
			}
			targetMap.put(key, value);
		}
	}

	public HashMap<?, ?> get(String key, int mapType) {
		Map<String, HashMap<String, String>> targetMap = getTargetMap(mapType);
		if (targetMap == null) {
			return null;
		}
		return targetMap.get(key);
	}

	private String getValue(String valueKey, String key, int mapType) {
		if (key != null) {
			HashMap<?, ?> map = get(key, mapType);
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
		if (fConsumers.isEmpty()) {
			dispose();
		}
	}

}
