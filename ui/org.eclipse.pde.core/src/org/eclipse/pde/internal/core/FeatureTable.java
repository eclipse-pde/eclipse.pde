/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * Stores IFeatureModels. Models are indexed by id and id with version for fast
 * retrieval. Given id or version may have more than one corresponding model. A
 * model has only one id and version that can be null. When models changes, its
 * Idver stays unchanged until the models reinserted.
 */
class FeatureTable {
	public class Idver {
		private final String fId;

		private final String fVer;

		public Idver(String id, String version) {
			fId = id;
			fVer = version;
		}

		public String getId() {
			return fId;
		}

		public String getVer() {
			return fVer;
		}

		public int hashCode() {
			int code = 0;
			if (fId != null)
				code += fId.hashCode();
			if (fVer != null)
				code += fVer.hashCode();

			return code;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Idver))
				return false;
			return equals(((Idver) obj).getId(), ((Idver) obj).getVer());
		}

		public boolean equals(String id, String version) {
			boolean sameId = fId == null && id == null || fId != null && fId.equals(id);
			boolean sameVer = fVer == null && version == null || fVer != null && fVer.equals(version);
			return sameId && sameVer;

		}

		public String toString() {
			return fId + "_" + fVer; //$NON-NLS-1$
		}
	}

	private static final IFeatureModel[] NO_MODELS = new IFeatureModel[0];

	/**
	 * Map of IFeatureModel to Idver
	 */
	private Map fModel2idver;

	/**
	 * Map of Idver to ArrayList of IFeatureModel
	 */
	private Map fIdver2models;

	/**
	 * Map of Id to ArrayList of Idver
	 */
	private Map fId2idvers;

	public FeatureTable() {

		fModel2idver = new HashMap();
		fIdver2models = new HashMap();
		fId2idvers = new HashMap();
	}

	public synchronized Idver get(IFeatureModel model) {
		return (Idver) fModel2idver.get(model);
	}

	public synchronized IFeatureModel[] get(String id, String version) {
		return getImpl(new Idver(id, version));
	}

	public synchronized IFeatureModel[] get(Idver idver) {
		return getImpl(idver);
	}

	private IFeatureModel[] getImpl(Idver idver) {
		ArrayList models = (ArrayList) fIdver2models.get(idver);
		if (models == null) {
			return NO_MODELS;
		}
		return (IFeatureModel[]) models.toArray(new IFeatureModel[models.size()]);
	}

	public synchronized IFeatureModel[] get(String id) {
		ArrayList idvers = (ArrayList) fId2idvers.get(id);
		if (idvers == null)
			return NO_MODELS;
		ArrayList allModels = new ArrayList();
		for (int i = 0; i < idvers.size(); i++) {
			Idver idver = (Idver) idvers.get(i);
			ArrayList models = (ArrayList) fIdver2models.get(idver);
			if (models == null) {
				continue;
			}
			allModels.addAll(models);
		}
		return (IFeatureModel[]) allModels.toArray(new IFeatureModel[allModels.size()]);
	}

	public synchronized IFeatureModel[] getAll() {
		return getAllImpl();
	}

	private IFeatureModel[] getAllImpl() {
		return (IFeatureModel[]) fModel2idver.keySet().toArray(new IFeatureModel[fModel2idver.size()]);
	}

	/**
	 * Removes the model.
	 * 
	 * @return Idver if model existed and was removed, null otherwise
	 */
	public synchronized Idver remove(IFeatureModel model) {
		return removeImpl(model);
	}

	private Idver removeImpl(IFeatureModel model) {
		Idver idver = (Idver) fModel2idver.remove(model);
		if (idver == null) {
			return null;
		}
		ArrayList models = (ArrayList) fIdver2models.get(idver);
		for (int i = 0; i < models.size(); i++) {
			if (models.get(i) == model) {
				models.remove(i);
				break;
			}
		}
		if (models.size() <= 0) {
			fIdver2models.remove(idver);

			ArrayList idvers = (ArrayList) fId2idvers.get(idver.getId());
			for (int i = 0; i < idvers.size(); i++) {
				if (idvers.get(i).equals(idver)) {
					idvers.remove(i);
					break;
				}
			}
			if (idvers.size() <= 0) {
				fId2idvers.remove(idver.getId());
			}
		}
		return idver;
	}

	/**
	 * Adds the model. Updates the position of the model if already exist.
	 * 
	 * @return Idver used during insertion
	 */
	public synchronized Idver add(IFeatureModel model) {
		removeImpl(model);

		IFeature feature = model.getFeature();
		String id = feature.getId();
		String ver = feature.getVersion();
		Idver idver = new Idver(id, ver);

		fModel2idver.put(model, idver);

		ArrayList models = (ArrayList) fIdver2models.get(idver);
		if (models == null) {
			models = new ArrayList(1);
			fIdver2models.put(idver, models);
		}
		models.add(model);

		ArrayList idvers = (ArrayList) fId2idvers.get(id);
		if (idvers == null) {
			idvers = new ArrayList(1);
			fId2idvers.put(id, idvers);
		}
		idvers.add(idver);

		return idver;
	}

	public synchronized String toString() {
		IFeatureModel[] models = getAllImpl();
		StringBuffer buf = new StringBuffer(30 * models.length);
		buf.append("["); //$NON-NLS-1$
		for (int i = 0; i < models.length; i++) {
			if (i > 0) {
				buf.append(",  "); //$NON-NLS-1$
			}
			buf.append(get(models[i]));
			buf.append("@"); //$NON-NLS-1$
			buf.append(models[i].getFeature().getId());
			buf.append("_"); //$NON-NLS-1$
			buf.append(models[i].getFeature().getVersion());
		}
		buf.append("]"); //$NON-NLS-1$
		return buf.toString();
	}

}
