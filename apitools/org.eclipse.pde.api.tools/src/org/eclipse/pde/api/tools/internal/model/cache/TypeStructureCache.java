/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model.cache;

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.model.Messages;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile.IModificationStamp;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.search.LRUMap;

import com.ibm.icu.text.MessageFormat;

/**
 * LRU Cache of type structures.
 * 
 * @since 1.1
 */
public class TypeStructureCache {

	/**
	 * Cache of entries
	 */
	private static LRUMap fgCache = new LRUMap(250);
	
	private static int fgHits = 0;
	private static int fgMiss = 0;	
	
	/**
	 * A cache entry
	 */
	static class StructureEntry {
		IApiType fType;
		IModificationStamp fModStamp;
		/**
		 * Constructs a an entry 
		 */
		public StructureEntry(IApiType type, IModificationStamp stamp) {
			fType = type;
			fModStamp = stamp;
		}
	}

	/**
	 * Caching strategies - none, CRCs/modification stamps, or URIs
	 * with clearing per build.
	 */
	private static final int CACHE_NONE = 0;
	private static final int CACHE_CRC = 1;
	private static final int CACHE_URI = 2;
	
	/**
	 * Used for performance tuning.
	 */
	private static int CACHE_STRATEGY = CACHE_NONE; 	
	
	/**
	 * Returns a type model object for the specified type originating from the specified component.
	 * Note that when an API component is not specified, some operations will not be available
	 * on the resulting {@link IApiType} (such as navigating super types, member types, etc).
	 *  
	 * @param classFile class file
	 * @param component API component or <code>null</code>
	 * @return type structure or
	 * @throws CoreException if unable to retrieve build the structure.
	 */
	public static IApiType getTypeStructure(IClassFile classFile, IApiComponent component) throws CoreException {
		switch (CACHE_STRATEGY) {
			case CACHE_CRC: {
				URI key = classFile.getURI();
				if (key == null) {
					throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
							MessageFormat.format(Messages.TypeStructureCache_0, new String[]{classFile.getTypeName()})));
				}
				IApiType type = null;
				StructureEntry entry = (StructureEntry) fgCache.get(key);
				IModificationStamp stamp = classFile.getModificationStamp();
				if (entry == null || (entry.fModStamp.getModificationStamp() != stamp.getModificationStamp())) {
					fgMiss++;
					byte[] contents = stamp.getContents();
					if (contents == null) {
						contents = classFile.getContents();
					}
					type = TypeStructureBuilder.buildTypeStructure(contents, component);
					fgCache.put(key, new StructureEntry(type, stamp));
				} else {
					type = entry.fType;
					fgHits++;
				}
				return type;
			}
			case CACHE_NONE: {
				return TypeStructureBuilder.buildTypeStructure(classFile.getContents(), component);
			}
			case CACHE_URI: {
				URI key = classFile.getURI();
				if (key == null) {
					throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
							MessageFormat.format(Messages.TypeStructureCache_0, new String[]{classFile.getTypeName()})));
				}
				IApiType type = (IApiType) fgCache.get(key);
				if (type == null) {
					fgMiss++;
					type = TypeStructureBuilder.buildTypeStructure(classFile.getContents(), component);
					fgCache.put(key, type);
				}
				return type;
			}
			default:
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
						"Internal error: invalid cache strategy")); //$NON-NLS-1$
		}
	}

	/**
	 * Clears all structures in the cache.
	 * 
	 * @param component API component
	 */
	public static void clearCache() {
		if (CACHE_STRATEGY == CACHE_URI) {
			fgCache.clear();
		}
	}
	
	public static String getStats() {
		StringBuffer buf = new StringBuffer();
		buf.append("Size: "); //$NON-NLS-1$
		buf.append(fgCache.size());
		buf.append("\nOverflows: ");  //$NON-NLS-1$
		buf.append(fgCache.getOverflows());
		buf.append("\nHits: "); //$NON-NLS-1$
		buf.append(fgHits);
		buf.append("\nMisses: "); //$NON-NLS-1$
		buf.append(fgMiss);
		return buf.toString();
	}
}
