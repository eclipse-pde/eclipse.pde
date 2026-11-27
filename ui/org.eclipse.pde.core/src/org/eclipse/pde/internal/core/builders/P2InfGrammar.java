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

/**
 * The key grammar of a p2.inf (p2 advice) file as understood by p2's
 * <code>AdviceFileParser</code>. The grammar is a tree of key segments. A
 * segment is either a fixed name, a numeric index (<code>requires.0</code>) or
 * a wildcard (<code>instructions.&lt;phase&gt;</code>).
 * <p>
 * The grammar is intentionally lenient, so that valid files are never flagged.
 * </p>
 */
public final class P2InfGrammar {

	/** Result of {@link P2InfGrammar#check(String)}. */
	public record KeyProblem(Kind kind, String key) {
		public enum Kind {
			UNKNOWN_KEY,
			/** A numeric index is missing, e.g. <code>requires.namespace</code> */
			MISSING_INDEX
		}
	}

	public static final class Node {
		private final String name;
		private final boolean index;
		private final boolean terminal;
		/**
		 * The well known names a wildcard stands for, <code>null</code> if this
		 * is not a wildcard
		 */
		private final List<String> wildcardNames;
		private final List<Node> children = new ArrayList<>();

		private Node(String name, boolean index, boolean terminal, List<String> wildcardNames) {
			this.name = name;
			this.index = index;
			this.terminal = terminal;
			this.wildcardNames = wildcardNames;
		}

		Node add(Node child) {
			children.add(child);
			return this;
		}

		Node leaf(String... names) {
			for (String n : names) {
				children.add(new Node(n, false, true, null));
			}
			return this;
		}

		private boolean isWildcard() {
			return wildcardNames != null;
		}

		public List<Node> getChildren() {
			return children;
		}

		public boolean isIndex() {
			return index;
		}

		/**
		 * @return <code>true</code> if a key may end with this segment
		 */
		public boolean isTerminal() {
			return terminal;
		}

		/**
		 * @return the concrete names this segment stands for. This is the name
		 *         itself or, for a wildcard, the well known names that match it
		 */
		public List<String> getNames() {
			return isWildcard() ? wildcardNames : List.of(name);
		}

		/**
		 * @return the child that represents a numeric index or
		 *         <code>null</code>
		 */
		public Node indexChild() {
			for (Node child : children) {
				if (child.index) {
					return child;
				}
			}
			return null;
		}

		/**
		 * @return the non-index child matching the given segment or
		 *         <code>null</code>
		 */
		public Node child(String segment) {
			for (Node child : children) {
				if (!child.index && (child.isWildcard() || child.name.equals(segment))) {
					return child;
				}
			}
			return null;
		}
	}

	private static final List<String> PHASES = List.of("install", "configure", "unconfigure", "uninstall"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private static final Node ROOT = buildGrammar();

	private P2InfGrammar() {
	}

	public static Node getRoot() {
		return ROOT;
	}

	private static Node named(String name) {
		return new Node(name, false, false, null);
	}

	private static Node index() {
		return new Node("{#}", true, false, null); //$NON-NLS-1$
	}

	private static Node indexed(String name, String... attributes) {
		Node index = index();
		index.leaf(attributes);
		return named(name).add(index);
	}

	private static Node instructions() {
		Node phase = new Node("<phase>", false, true, PHASES); //$NON-NLS-1$
		phase.leaf("import"); //$NON-NLS-1$
		return named("instructions").add(phase); //$NON-NLS-1$
	}

	private static Node update(String... attributes) {
		return named("update").leaf(attributes); //$NON-NLS-1$
	}

	private static Node buildGrammar() {
		Node root = named("root"); //$NON-NLS-1$
		root.add(named("advice").leaf("version")); //$NON-NLS-1$ //$NON-NLS-2$
		addCommonKeys(root);
		root.add(unit("units")); //$NON-NLS-1$
		root.add(unit("iu")); //$NON-NLS-1$
		return root;
	}

	/**
	 * Adds the keys that are valid at the top level of the file as well as
	 * inside a unit (<code>units.N</code> / <code>iu.N</code>).
	 */
	private static void addCommonKeys(Node parent) {
		parent.add(indexed("provides", "namespace", "name", "version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		parent.add(requirements("requires")); //$NON-NLS-1$
		parent.add(requirements("metaRequirements")); //$NON-NLS-1$
		parent.add(indexed("properties", "name", "value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		parent.add(update("id", "range", "severity", "description", "match", "matchExp")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		parent.add(instructions());
	}

	private static Node requirements(String name) {
		return indexed(name, "namespace", "name", "range", "matchExp", "greedy", "optional", "multiple", "filter", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
				"min", "max"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static Node unit(String name) {
		Node unit = named(name);
		Node index = index();
		unit.add(index);
		index.leaf("id", "version", "singleton", "filter"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(named("copyright").leaf("location", "body", "url")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(named("touchpoint").leaf("id", "version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		index.add(indexed("artifacts", "id", "version", "classifier")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(indexed("licenses", "location", "url", "body")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		index.add(requirements("hostRequirements")); //$NON-NLS-1$
		addCommonKeys(index);
		return unit;
	}

	/**
	 * Checks the spelling of the given p2.inf key and that numeric indices are
	 * present where required. Only the first problem of a key is reported.
	 * Incomplete keys (e.g. <code>requires.0</code>) are not a problem here.
	 *
	 * @return the problem found or <code>null</code> if the key is fine
	 */
	public static KeyProblem check(String key) {
		Node node = ROOT;
		for (String segment : key.split("\\.", -1)) { //$NON-NLS-1$
			Node index = node.indexChild();
			if (index != null) {
				if (isNumeric(segment)) {
					node = index;
					continue;
				}
				return new KeyProblem(KeyProblem.Kind.MISSING_INDEX, key);
			}
			Node child = node.child(segment);
			if (child == null) {
				return new KeyProblem(KeyProblem.Kind.UNKNOWN_KEY, key);
			}
			node = child;
		}
		return null;
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
}
