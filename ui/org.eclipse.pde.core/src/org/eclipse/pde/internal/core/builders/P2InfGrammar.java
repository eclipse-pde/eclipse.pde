/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The key grammar of a p2.inf (p2 advice) file as understood by p2's
 * <code>AdviceFileParser</code>. The grammar is a tree of key segments. A
 * segment is either a fixed name, a numeric index (<code>requires.0</code>) or
 * a wildcard (<code>instructions.&lt;phase&gt;</code>).
 * <p>
 * The grammar is intentionally lenient: it is a union of the keys supported by
 * p2 and the keys offered by the p2.inf content assist, so that valid files are
 * never flagged.
 * </p>
 */
public final class P2InfGrammar {

	/** Result of {@link P2InfGrammar#check(String)}. */
	public record KeyProblem(Kind kind, String key, String suggestion) {
		public enum Kind {
			/** A segment of the key is not part of the grammar */
			UNKNOWN_KEY,
			/** A numeric index is missing, e.g. <code>requires.namespace</code> */
			MISSING_INDEX
		}
	}

	private static final class Node {
		private final String name;
		private final boolean index;
		private final boolean wildcard;
		private final List<Node> children = new ArrayList<>();

		Node(String name, boolean index, boolean wildcard) {
			this.name = name;
			this.index = index;
			this.wildcard = wildcard;
		}

		Node add(Node child) {
			children.add(child);
			return this;
		}

		Node leaf(String... names) {
			for (String n : names) {
				children.add(new Node(n, false, false));
			}
			return this;
		}

		Node indexChild() {
			for (Node child : children) {
				if (child.index) {
					return child;
				}
			}
			return null;
		}

		Node child(String segment) {
			for (Node child : children) {
				if (!child.index && (child.wildcard || child.name.equals(segment))) {
					return child;
				}
			}
			return null;
		}
	}

	private static final Node ROOT = buildGrammar();

	private P2InfGrammar() {
	}

	private static Node named(String name) {
		return new Node(name, false, false);
	}

	private static Node indexed(String name, String... attributes) {
		Node index = new Node("{#}", true, false); //$NON-NLS-1$
		index.leaf(attributes);
		return named(name).add(index);
	}

	private static Node instructions() {
		Node phase = new Node("<phase>", false, true); //$NON-NLS-1$
		phase.leaf("import"); //$NON-NLS-1$
		return named("instructions").add(phase); //$NON-NLS-1$
	}

	private static Node update(String... attributes) {
		return named("update").leaf(attributes); //$NON-NLS-1$
	}

	private static Node buildGrammar() {
		Node root = named("root"); //$NON-NLS-1$
		root.leaf("advice.version"); //$NON-NLS-1$
		root.add(indexed("provides", "namespace", "name", "version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		root.add(requirements("requires")); //$NON-NLS-1$
		root.add(requirements("metaRequirements")); //$NON-NLS-1$
		root.add(indexed("properties", "name", "value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		root.add(update("id", "range", "severity", "description", "match", "matchExp")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		root.add(instructions());
		root.add(unit("units")); //$NON-NLS-1$
		root.add(unit("iu")); //$NON-NLS-1$
		return root;
	}

	private static Node requirements(String name) {
		return indexed(name, "namespace", "name", "range", "matchExp", "greedy", "optional", "multiple", "filter", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
				"min", "max"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static Node unit(String name) {
		Node unit = named(name);
		Node index = new Node("{#}", true, false); //$NON-NLS-1$
		unit.add(index);
		index.leaf("id", "version", "singleton", "filter"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(named("copyright").leaf("location", "body", "url")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(named("touchpoint").leaf("id", "version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		index.add(update("id", "range", "severity", "description", "match", "matchExp")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		index.add(indexed("artifacts", "id", "version", "classifier")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(indexed("licenses", "location", "url", "body")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(requirements("requires")); //$NON-NLS-1$
		index.add(requirements("metaRequirements")); //$NON-NLS-1$
		index.add(requirements("hostRequirements")); //$NON-NLS-1$
		index.add(indexed("provides", "namespace", "name", "version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(indexed("properties", "name", "value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		index.add(instructions());
		return unit;
	}

	/**
	 * Checks the spelling of the given p2.inf key and that numeric indices are
	 * present where required. Only the first problem of a key is reported.
	 * Incomplete keys (e.g. <code>requires.0</code>) are not a problem here.
	 *
	 * @param key
	 *            the complete key of a p2.inf entry
	 * @return the problem found or <code>null</code> if the key is fine
	 */
	public static KeyProblem check(String key) {
		String[] segments = key.split("\\.", -1); //$NON-NLS-1$
		Node node = ROOT;
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			Node index = node.indexChild();
			if (index != null) {
				if (isNumeric(segment)) {
					node = index;
					continue;
				}
				// the index is missing, check the following segment against the
				// attributes behind the index
				String attribute = findName(index, segment);
				String fixed = attribute != null ? attribute : segment;
				return new KeyProblem(KeyProblem.Kind.MISSING_INDEX, key,
						replace(segments, i, "0", i, fixed)); //$NON-NLS-1$
			}
			Node child = node.child(segment);
			if (child == null) {
				String similar = findName(node, segment);
				return new KeyProblem(KeyProblem.Kind.UNKNOWN_KEY, key,
						similar != null ? replace(segments, i, similar, -1, null) : null);
			}
			node = child;
		}
		return null;
	}

	/**
	 * Builds a suggestion where the segment at <code>position</code> is replaced
	 * by <code>replacement</code>. If <code>insertAt</code> is not negative,
	 * <code>replacement</code> is inserted before that segment instead and the
	 * segment itself is replaced by <code>fixed</code>.
	 */
	private static String replace(String[] segments, int position, String replacement, int insertAt, String fixed) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < segments.length; i++) {
			if (i == insertAt) {
				result.add(replacement);
				result.add(fixed);
			} else if (i == position && insertAt < 0) {
				result.add(replacement);
			} else {
				result.add(segments[i]);
			}
		}
		return String.join(".", result); //$NON-NLS-1$
	}

	private static boolean isNumeric(String segment) {
		if (segment.isEmpty()) {
			return false;
		}
		for (int i = 0; i < segment.length(); i++) {
			if (!Character.isDigit(segment.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the name of the child of the given node that is most similar to
	 *         the segment or <code>null</code> if there is none that is close
	 *         enough
	 */
	private static String findName(Node node, String segment) {
		String lower = segment.toLowerCase(Locale.ROOT);
		int max = segment.length() >= 5 ? 2 : 1;
		String best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Node child : node.children) {
			if (child.index || child.wildcard) {
				continue;
			}
			int distance = distance(lower, child.name.toLowerCase(Locale.ROOT));
			if (distance <= max && distance < bestDistance) {
				best = child.name;
				bestDistance = distance;
			}
		}
		return best;
	}

	/** Levenshtein distance */
	private static int distance(String a, String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[b.length()];
	}
}
