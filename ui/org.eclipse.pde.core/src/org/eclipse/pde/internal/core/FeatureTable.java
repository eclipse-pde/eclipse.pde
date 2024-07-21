/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * Stores IFeatureModels. Models are indexed by id and id with version for fast
 * retrieval. Given id or version may have more than one corresponding model. A
 * model has only one id and version that can be null. When models changes, its
 * Idver stays unchanged until the models reinserted.
 */
class FeatureTable {

	static record Idver(String id, String version) {
	}

	/**
	 * Map of IFeatureModel to Idver
	 */
	private final Map<IFeatureModel, Idver> fModel2idver = new HashMap<>();

	/**
	 * Map of Idver to List of IFeatureModel
	 */
	private final Map<Idver, List<IFeatureModel>> fIdver2models = new HashMap<>();

	/**
	 * Map of Id to List of Idver
	 */
	private final Map<String, List<Idver>> fId2idvers = new HashMap<>();

	public synchronized Idver get(IFeatureModel model) {
		return fModel2idver.get(model);
	}

	public List<IFeatureModel> get(String id, String version) {
		return get(new Idver(id, version));
	}

	public synchronized List<IFeatureModel> get(Idver idver) {
		List<IFeatureModel> models = fIdver2models.get(idver);
		if (models == null) {
			return List.of();
		}
		return List.copyOf(models); // decouple returned list from this table
	}

	public synchronized List<IFeatureModel> getAllValidFeatures(String id) {
		List<Idver> idvers = fId2idvers.get(id);
		if (idvers == null) {
			return List.of();
		}
		return idvers.stream().map(fIdver2models::get) //
				.filter(Objects::nonNull).flatMap(List::stream) //
				.filter(IFeatureModel::isValid).toList();
	}

	public synchronized IFeatureModel[] getAllValidFeatures() {
		return fModel2idver.keySet().stream().filter(IFeatureModel::isValid).toArray(IFeatureModel[]::new);
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
		Idver idver = fModel2idver.remove(model);
		if (idver == null) {
			return null;
		}
		if (removeValueFromMultimap(fIdver2models, idver, model)) {
			removeValueFromMultimap(fId2idvers, idver.id(), idver);
		}
		return idver;
	}

	/**
	 * Removes the given key-value mappings from the given multi-value map.
	 *
	 * @return true if the map contains no more value for the given key
	 */
	private static <K, V> boolean removeValueFromMultimap(Map<K, List<V>> map, K key, V value) {
		return map.computeIfPresent(key, (k, values) -> {
			values.remove(value);
			return values.isEmpty() ? null : values;
		}) == null;
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

		List<IFeatureModel> models = fIdver2models.computeIfAbsent(idver, iv -> new ArrayList<>(1));
		models.add(model);

		List<Idver> idvers = fId2idvers.computeIfAbsent(id, i -> new ArrayList<>(1));
		idvers.add(idver);

		return idver;
	}

	@Override
	public synchronized String toString() {
		StringJoiner joiner = new StringJoiner(", ", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		for (IFeatureModel model : fModel2idver.keySet()) {
			IFeature feature = model.getFeature();
			String str = get(model) + "@" + feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
			joiner.add(str);
		}
		return joiner.toString();
	}

}
