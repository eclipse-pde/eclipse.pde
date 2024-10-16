/*******************************************************************************
 * Copyright (c) 2016, 2024 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 531918] filter suggestions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.pde.internal.genericeditor.target.extension.p2.P2Fetcher;

/**
 * This class is used to cache the p2 repositories completion information order
 * to minimize IO round trips and have instant completion for IUs and their
 * versions.
 *
 * There will be only one cache shared between editors. In the future a function
 * will be added for the user to be able to flush this cache.
 */
public class RepositoryCache {

	private RepositoryCache() {
		// avoid instantiation
	}

	private static final Map<URI, Map<String, List<IVersionedId>>> CACHE = new ConcurrentHashMap<>();

	/**
	 * Fetches information and caches it.
	 * <p>
	 * All available IUs are returned as a map mapping the IDs of all IUs
	 * available in the {@code repositories} to the list of all available
	 * {@link IVersionedId versioned IDs} for that ID. All keys are sorted in
	 * alphabetical order and all versions are sorted in descending order.
	 * </p>
	 * 
	 * @return all available units in the specified {@code repository} in a map
	 *         mapping all IDs to all available versions.
	 */
	public static Map<String, List<IVersionedId>> fetchP2UnitsFromRepos(List<String> repositories) {
		if (repositories.size() == 1) {
			return fetchP2DataOfRepo(repositories.get(0));
		}
		var repos = repositories.stream().map(RepositoryCache::fetchP2DataOfRepo).toList();
		return toSortedMap(repos.stream().map(Map::values).flatMap(Collection::stream).flatMap(List::stream));
	}

	private static Map<String, List<IVersionedId>> fetchP2DataOfRepo(String repository) {
		URI location;
		try {
			location = new URI(repository);
		} catch (URISyntaxException e) {
			return Map.of();
		}
		return CACHE.computeIfAbsent(location, repo -> toSortedMap(P2Fetcher.fetchAvailableUnits(repo)));
	}

	private static final Comparator<IVersionedId> BY_ID_FIRST_THEN_DESCENDING_VERSION = Comparator
			.comparing(IVersionedId::getId, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(IVersionedId::getVersion, Comparator.reverseOrder());

	private static Map<String, List<IVersionedId>> toSortedMap(Stream<IVersionedId> units) {
		return units.sorted(BY_ID_FIRST_THEN_DESCENDING_VERSION).collect(
				Collectors.groupingBy(IVersionedId::getId, LinkedHashMap::new, Collectors.toUnmodifiableList()));
	}

	/**
	 *
	 * Method used to narrow down proposals in case a prefix is provided.
	 * Example:
	 *
	 * <pre>
	 *  &lt;unit id="org.^
	 * </pre>
	 *
	 * where ^ is an autocomplete call. Prefix in this case will be 'org.'
	 *
	 * @param repo
	 *            repository URL
	 * @param prefix
	 *            A prefix used to narrow down the match list
	 * @return A list of IUs whose id starts with 'prefix'
	 */
	public static List<IVersionedId> getUnitsByPrefix(String repo, String prefix) {
		Map<String, List<IVersionedId>> allUnits = fetchP2UnitsFromRepos(List.of(repo));
		return allUnits.values().stream().flatMap(List::stream) //
				.filter(unit -> unit.getId().startsWith(prefix)).toList();
	}

	/**
	 *
	 * Method used to narrow down proposals in case a prefix is provided.
	 * Example:
	 *
	 * <pre>
	 *  &lt;unit id="eclipse^
	 * </pre>
	 *
	 * where ^ is an autocomplete call. Search term in this case will be
	 * '*eclipse*'
	 *
	 * @param repo
	 *            repository URL
	 * @param searchTerm
	 *            A prefix used to narrow down the match list
	 * @return A list of IUs whose id contains 'searchTerm'
	 */
	public static List<IVersionedId> getUnitsBySearchTerm(String repo, String searchTerm) {
		Map<String, List<IVersionedId>> allUnits = fetchP2UnitsFromRepos(List.of(repo));
		return allUnits.values().stream().flatMap(List::stream) //
				.filter(unit -> unit.getId().contains(searchTerm)).toList();
	}

	/**
	 * Classic cache up-to-date check.
	 *
	 * @param repo
	 *            repository URL
	 * @return whether the cache is up to date for this repo
	 */
	public static boolean isUpToDate(String repo) {
		return CACHE.get(URI.create(repo)) != null;
	}
}
