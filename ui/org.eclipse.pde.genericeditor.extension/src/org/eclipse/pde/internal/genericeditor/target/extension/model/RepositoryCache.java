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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.genericeditor.target.extension.p2.Messages;
import org.eclipse.pde.internal.genericeditor.target.extension.p2.P2Fetcher;
import org.eclipse.pde.internal.genericeditor.target.extension.p2.P2Fetcher.RepositoryContent;

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

	private static record RepositoryMetadata(Map<String, List<IVersionedId>> units, List<URI> children) {
	}

	private static final Map<URI, CompletableFuture<RepositoryMetadata>> CACHE = new ConcurrentHashMap<>();

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
			return getFutureValue(fetchP2DataOfRepo(repositories.get(0)), RepositoryMetadata::units, Map.of());
		}
		var repos = repositories.stream().map(RepositoryCache::fetchP2DataOfRepo).toList();
		// Fetch all repos at once to await pending metadata in parallel
		return toSortedMap(repos.stream()
				.map(r -> getFutureValue(r, RepositoryMetadata::units, Map.<String, List<IVersionedId>>of()))
				.map(Map::values).flatMap(Collection::stream).flatMap(List::stream));
	}

	public static List<URI> fetchChildrenOfRepo(String repository) {
		return getFutureValue(fetchP2DataOfRepo(repository), RepositoryMetadata::children, List.of());
	}

	public static void prefetchP2MetadataOfRepository(String repository) {
		fetchP2DataOfRepo(repository);
	}

	private static Future<RepositoryMetadata> fetchP2DataOfRepo(String repository) {
		URI location;
		try { // always have a trailing slash to avoid duplicated cache entries
			location = new URI(repository + (repository.endsWith("/") ? "" : "/"));
		} catch (URISyntaxException e) {
			return CompletableFuture.failedFuture(e);
		}
		return CACHE.compute(location, (repo, f) -> {
			if (f != null && (!f.isDone() || !f.isCompletedExceptionally() && !f.isCancelled())) {
				return f; // computation is running or has succeeded
			}
			CompletableFuture<RepositoryMetadata> future = new CompletableFuture<>();
			// Fetching P2 repository information is a costly operation
			// time-wise. Thus it is done in a job.
			Job job = Job.create(NLS.bind(Messages.UpdateJob_P2DataFetch, repo), m -> {
				try {
					RepositoryContent content = P2Fetcher.fetchAvailableUnits(repo, m);
					Map<String, List<IVersionedId>> units = toSortedMap(
							content.units().stream().map(iu -> new VersionedId(iu.getId(), iu.getVersion())));
					future.complete(new RepositoryMetadata(units, content.children()));
				} catch (Throwable e) {
					future.completeExceptionally(e);
					// Only log the failure, don't open an error-dialog.
					ILog.get().warn(e.getMessage(), e);
				}
			});
			job.setUser(true);
			job.schedule();
			return future;
		});
	}

	private static final Comparator<IVersionedId> BY_ID_FIRST_THEN_DESCENDING_VERSION = Comparator
			.comparing(IVersionedId::getId, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(IVersionedId::getVersion, Comparator.reverseOrder());

	private static Map<String, List<IVersionedId>> toSortedMap(Stream<IVersionedId> units) {
		return units.sorted(BY_ID_FIRST_THEN_DESCENDING_VERSION).collect(
				Collectors.groupingBy(IVersionedId::getId, LinkedHashMap::new, Collectors.toUnmodifiableList()));
	}

	private static <T> T getFutureValue(Future<RepositoryMetadata> future, Function<RepositoryMetadata, T> getter,
			T defaultValue) {
		try {
			return getter.apply(future.get());
		} catch (Exception e) { // interrupted, canceled or execution failure
			return defaultValue;
		}
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
}
