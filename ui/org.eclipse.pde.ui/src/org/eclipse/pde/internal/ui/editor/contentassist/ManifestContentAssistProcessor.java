/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au) - bug 197410
 *     Benjamin Cabe (benjamin.cabe@anyware-tech.com) - bug 253692
 *     Alex Blewitt (alex.blewitt@gmail.com) - bug 469803
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.util.HeaderMap;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.util.ImageOverlayIcon;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.*;

public class ManifestContentAssistProcessor extends TypePackageCompletionProcessor implements ICompletionListener {

	protected PDESourcePage fSourcePage;
	private IJavaProject fJP;

	// if we order the headers alphabetically in the array, there is no need to sort and we can save time
	private static final String[] fHeader = {Constants.BUNDLE_ACTIVATIONPOLICY, Constants.BUNDLE_ACTIVATOR, Constants.BUNDLE_CATEGORY, Constants.BUNDLE_CLASSPATH, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_COPYRIGHT, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_DOCURL, Constants.BUNDLE_LOCALIZATION, Constants.BUNDLE_MANIFESTVERSION, Constants.BUNDLE_NAME, Constants.BUNDLE_NATIVECODE, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_UPDATELOCATION, Constants.BUNDLE_VENDOR, Constants.BUNDLE_VERSION, Constants.DYNAMICIMPORT_PACKAGE, ICoreConstants.ECLIPSE_BUDDY_POLICY, ICoreConstants.ECLIPSE_BUNDLE_SHAPE, ICoreConstants.ECLIPSE_GENERIC_CAPABILITY, ICoreConstants.ECLIPSE_GENERIC_REQUIRED, ICoreConstants.ECLIPSE_LAZYSTART,
			ICoreConstants.PLATFORM_FILTER, ICoreConstants.ECLIPSE_REGISTER_BUDDY, ICoreConstants.ECLIPSE_SOURCE_REFERENCES, Constants.EXPORT_PACKAGE, ICoreConstants.EXPORT_SERVICE, Constants.FRAGMENT_HOST, Constants.IMPORT_PACKAGE, ICoreConstants.IMPORT_SERVICE, Constants.PROVIDE_CAPABILITY, Constants.REQUIRE_BUNDLE, Constants.REQUIRE_CAPABILITY};


	private static final char[] fAutoActivationChars = { ':', ' ', ',', '.', ';', '=' };

	protected static final short F_TYPE_HEADER = 0, // header proposal
			F_TYPE_PKG = 1, // package proposal
			F_TYPE_BUNDLE = 2, // bundle proposal
			F_TYPE_CLASS = 3, // class proposal
			F_TYPE_DIRECTIVE = 4, // directive proposal
			F_TYPE_ATTRIBUTE = 5, // attribute proposal
			F_TYPE_VALUE = 6, // value of attribute or directive proposal
			F_TYPE_EXEC_ENV = 7, // value of execution env., added since we use a unique icon for exec envs.

			F_TOTAL_TYPES = 8;

	private final Image[] fImages = new Image[F_TOTAL_TYPES];

	private static final String[] fExecEnvs;
	static {
		IExecutionEnvironment[] envs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		fExecEnvs = new String[envs.length];
		for (int i = 0; i < envs.length; i++)
			fExecEnvs[i] = envs[i].getId();
		Arrays.sort(fExecEnvs, (o1, o2) -> o1.compareToIgnoreCase(o2));
	}

	/**
	 * Maps string header names to either a Set of Strings or an array of {@link ManifestElement}s.  See {@link #isHeader(IDocument, int, int)}
	 */
	Map<String, Object> fHeaders;

	public ManifestContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		if (fHeaders == null) {
			parseDocument(doc);
		}
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			return computeCompletionProposals(doc, lineStart, offset);
		} catch (BadLocationException e) {
		}
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return fAutoActivationChars;
	}

	protected final void parseDocument(IDocument doc) {
		fHeaders = new HeaderMap<>();
		int numLines = doc.getNumberOfLines();
		int offset = 0;
		// since we are searching the line after the current line to peak ahead to see if it is a new header, start with the index of 1
		for (int i = 1; i < numLines; i++) {
			try {
				IRegion nextLine = doc.getLineInformation(i);
				// see if the next line contains a new header.  If so, we know we found the end of the current header
				if (containsNewHeader(doc.get(nextLine.getOffset(), nextLine.getLength())) || i == (numLines - 1)) {
					// if the next line contains a header, then get the text up to the character before the next line's offset
					String value = doc.get(offset, nextLine.getOffset() - offset - 1).trim();
					int index = value.indexOf(':');
					String header = (index == -1) ? value : value.substring(0, index);
					try {
						if (value.endsWith(",")) //$NON-NLS-1$
							value = value.substring(0, value.length() - 1);
						ManifestElement[] elems = ManifestElement.parseHeader(header, value.substring(index + 1));
						if (shouldStoreSet(header)) {
							HashSet<String> set = new HashSet<>((4 / 3) * elems.length + 1);
							for (ManifestElement elem : elems)
								set.add(elem.getValue());
							fHeaders.put(header, set);
						} else
							fHeaders.put(header, elems);
					} catch (BundleException e) {
					}
					offset = nextLine.getOffset();
				}
			} catch (BadLocationException e) {
			}
		}
	}

	private boolean containsNewHeader(String text) {
		int length = text.length();
		// blank lines represent a new header as defined by the Java Manifest Specification
		if (length == 0)
			return true;
		int index = text.indexOf(':');
		while (index != -1) {
			// if we are at end of the line, assume it the colon is defining a new header.
			// If the next character is an '=', the colon is part of a directive so we should continue looking
			if ((index == length - 1) || (text.charAt(index + 1) != '='))
				return true;
			index = text.indexOf(':', index + 1);
		}
		return false;
	}

	protected final boolean shouldStoreSet(String header) {
		return header.equalsIgnoreCase(Constants.IMPORT_PACKAGE) || header.equalsIgnoreCase(Constants.EXPORT_PACKAGE) || header.equalsIgnoreCase(Constants.REQUIRE_BUNDLE) || header.equalsIgnoreCase(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

	protected ICompletionProposal[] computeCompletionProposals(IDocument doc, int startOffset, int offset) {
		try {
			if (!isHeader(doc, startOffset, offset))
				return computeValue(doc, startOffset, offset);
			return computeHeader(doc.get(startOffset, offset - startOffset), startOffset, offset);
		} catch (BadLocationException e) {
		}
		return new ICompletionProposal[0];
	}

	protected final boolean isHeader(IDocument doc, int startOffset, int offset) throws BadLocationException {
		String value = doc.get(startOffset, offset - startOffset);
		if (value.indexOf(':') != -1)
			return false;
		for (--startOffset; startOffset >= 0; --startOffset) {
			char ch = doc.getChar(startOffset);
			if (!Character.isWhitespace(ch))
				return ch != ',' && ch != ':' && ch != ';';
		}
		return true;
	}

	protected ICompletionProposal[] computeHeader(String currentValue, int startOffset, int offset) {
		ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
		for (String element : fHeader) {
			if (element.regionMatches(true, 0, currentValue, 0, currentValue.length()) && fHeaders.get(element) == null) {
				TypeCompletionProposal proposal = new TypeCompletionProposal(element + ": ", getImage(F_TYPE_HEADER), //$NON-NLS-1$
						element, startOffset, currentValue.length());
				proposal.setAdditionalProposalInfo(getJavaDoc(element));
				completions.add(proposal);
			}
		}
		return completions.toArray(new ICompletionProposal[completions.size()]);
	}

	protected ICompletionProposal[] computeValue(IDocument doc, int startOffset, int offset) throws BadLocationException {
		String value = doc.get(startOffset, offset - startOffset);
		int lineNum = doc.getLineOfOffset(startOffset) - 1;
		int index;
		while ((index = value.indexOf(':')) == -1 || ((value.length() - 1 != index) && (value.charAt(index + 1) == '='))) {
			int startLine = doc.getLineOffset(lineNum);
			value = doc.get(startLine, offset - startLine);
			lineNum--;
		}

		int length = value.length();
		if (value.regionMatches(true, 0, Constants.IMPORT_PACKAGE, 0, Math.min(length, Constants.IMPORT_PACKAGE.length())))
			return handleImportPackageCompletion(value.substring(Constants.IMPORT_PACKAGE.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.FRAGMENT_HOST, 0, Math.min(length, Constants.FRAGMENT_HOST.length())))
			return handleFragmentHostCompletion(value.substring(Constants.FRAGMENT_HOST.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.REQUIRE_BUNDLE, 0, Math.min(length, Constants.REQUIRE_BUNDLE.length())))
			return handleRequireBundleCompletion(value.substring(Constants.REQUIRE_BUNDLE.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.EXPORT_PACKAGE, 0, Math.min(length, Constants.EXPORT_PACKAGE.length())))
			return handleExportPackageCompletion(value.substring(Constants.EXPORT_PACKAGE.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.BUNDLE_ACTIVATOR, 0, Math.min(length, Constants.BUNDLE_ACTIVATOR.length())))
			return handleBundleActivatorCompletion(removeLeadingSpaces(value.substring(Constants.BUNDLE_ACTIVATOR.length() + 1)), offset);
		if (value.regionMatches(true, 0, Constants.BUNDLE_SYMBOLICNAME, 0, Math.min(length, Constants.BUNDLE_SYMBOLICNAME.length())))
			return handleBundleSymbolicNameCompletion(value.substring(Constants.BUNDLE_SYMBOLICNAME.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, 0, Math.min(length, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.length())))
			return handleRequiredExecEnv(value.substring(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.length() + 1), offset);
		if (value.regionMatches(true, 0, ICoreConstants.ECLIPSE_LAZYSTART, 0, Math.min(length, ICoreConstants.ECLIPSE_LAZYSTART.length())))
			return handleTrueFalseValue(value.substring(ICoreConstants.ECLIPSE_LAZYSTART.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.BUNDLE_NAME, 0, Math.min(length, Constants.BUNDLE_NAME.length())))
			return handleBundleNameCompletion(value.substring(Constants.BUNDLE_NAME.length() + 1), offset);
		if (value.regionMatches(true, 0, Constants.BUNDLE_ACTIVATIONPOLICY, 0, Math.min(length, Constants.BUNDLE_ACTIVATIONPOLICY.length())))
			return handleBundleActivationPolicyCompletion(value.substring(Constants.BUNDLE_ACTIVATIONPOLICY.length() + 1), offset);
		if (value.regionMatches(true, 0, ICoreConstants.ECLIPSE_BUDDY_POLICY, 0, Math.min(length, ICoreConstants.ECLIPSE_BUDDY_POLICY.length())))
			return handleBuddyPolicyCompletion(value.substring(ICoreConstants.ECLIPSE_BUDDY_POLICY.length() + 1), offset);
		if (value.regionMatches(true, 0, ICoreConstants.ECLIPSE_BUNDLE_SHAPE, 0, Math.min(length, ICoreConstants.ECLIPSE_BUNDLE_SHAPE.length())))
			return handleEclipseBundleShape(value.substring(ICoreConstants.ECLIPSE_BUNDLE_SHAPE.length() + 1), offset);
		return new ICompletionProposal[0];
	}

	/*
	 * Easter Egg
	 */
	protected ICompletionProposal[] handleBundleNameCompletion(String currentValue, int offset) {
		currentValue = removeLeadingSpaces(currentValue);
		return new ICompletionProposal[0];
	}

	protected ICompletionProposal[] handleImportPackageCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue;
		if (comma > semicolon || comma == semicolon) {
			@SuppressWarnings("unchecked")
			HashSet<String> set = (HashSet<String>) fHeaders.get(Constants.IMPORT_PACKAGE);
			if (set == null)
				set = parseHeaderForValues(currentValue, offset);
			@SuppressWarnings("unchecked")
			HashSet<String> importedBundles = (HashSet<String>) fHeaders.get(Constants.REQUIRE_BUNDLE);
			if (importedBundles == null)
				importedBundles = new HashSet<>(0);
			value = removeLeadingSpaces(value);
			int length = value.length();
			set.remove(value);
			ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
			IPluginModelBase[] bases = PluginRegistry.getActiveModels();

			for (IPluginModelBase base : bases) { // Remove any packages already imported through Require-Bundle
				BundleDescription desc = base.getBundleDescription();
				if (desc == null || importedBundles.contains(desc.getSymbolicName()))
					continue;
				ExportPackageDescription[] expPkgs = desc.getExportPackages();
				for (ExportPackageDescription expPkg : expPkgs) {
					String pkgName = expPkg.getName();
					if (pkgName.regionMatches(true, 0, value, 0, length) && !set.contains(pkgName)) {
						completions.add(new TypeCompletionProposal(pkgName, getImage(F_TYPE_PKG), pkgName, offset - length, length));
						set.add(pkgName);
					}
				}
			}
			ICompletionProposal[] proposals = completions.toArray(new ICompletionProposal[completions.size()]);
			sortCompletions(proposals);
			return proposals;
		}
		int equals = currentValue.lastIndexOf('=');
		if (equals == -1 || semicolon > equals) {
			String[] validAtts = new String[] {Constants.RESOLUTION_DIRECTIVE, Constants.VERSION_ATTRIBUTE};
			Integer[] validTypes = new Integer[] {Integer.valueOf(F_TYPE_DIRECTIVE), Integer.valueOf(F_TYPE_ATTRIBUTE)};
			return handleAttrsAndDirectives(value, initializeNewList(validAtts), initializeNewList(validTypes), offset);
		}
		String attributeValue = removeLeadingSpaces(currentValue.substring(semicolon + 1));
		if (Constants.RESOLUTION_DIRECTIVE.regionMatches(true, 0, attributeValue, 0, Constants.RESOLUTION_DIRECTIVE.length()))
			return matchValueCompletion(currentValue.substring(equals + 1), new String[] {Constants.RESOLUTION_MANDATORY, Constants.RESOLUTION_OPTIONAL}, new int[] {F_TYPE_VALUE, F_TYPE_VALUE}, offset, "RESOLUTION_"); //$NON-NLS-1$
		if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, attributeValue, 0, Constants.VERSION_ATTRIBUTE.length())) {
			value = removeLeadingSpaces(currentValue.substring(equals + 1));
			if (value.length() == 0)
				return new ICompletionProposal[] {new TypeCompletionProposal("\"\"", getImage(F_TYPE_VALUE), "\"\"", offset, 0)}; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new ICompletionProposal[0];
	}

	private ICompletionProposal[] handleXFriendsCompletion(String value, final int offset) {
		ManifestElement[] elems = (ManifestElement[]) fHeaders.get(Constants.BUNDLE_SYMBOLICNAME);
		HashSet<String> set = new HashSet<>();
		if (elems != null && elems.length > 0)
			set.add(elems[0].getValue());
		value = removeLeadingSpaces(value);
		if (value.length() == 0)
			return new ICompletionProposal[] {new TypeCompletionProposal("\"\"", getImage(F_TYPE_VALUE), "\"\"", offset, 0)}; //$NON-NLS-1$ //$NON-NLS-2$
		if (value.charAt(0) == '"')
			value = value.substring(1);
		int index = value.lastIndexOf(',');
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
			set.add(tokenizer.nextToken());
		return handleBundleCompletions(value.substring((index == -1) ? 0 : index + 1), set, F_TYPE_VALUE, offset, true);
	}

	protected ICompletionProposal[] handleFragmentHostCompletion(String currentValue, int offset) {
		int index = currentValue.lastIndexOf(';');
		if (index == -1) {
			HashMap<String, TypeCompletionProposal> completions = new HashMap<>();
			IPluginModelBase base = PluginRegistry.findModel(((ManifestEditor) fSourcePage.getEditor()).getCommonProject());
			BundleDescription desc = base.getBundleDescription();
			String currentId = desc != null ? desc.getSymbolicName() : null;

			String pluginStart = removeLeadingSpaces(currentValue);
			int length = pluginStart.length();
			IPluginModelBase[] bases = PluginRegistry.getActiveModels();
			for (IPluginModelBase base2 : bases) {
				desc = base2.getBundleDescription();
				if (desc != null && desc.getHost() == null) {
					String pluginID = base2.getBundleDescription().getSymbolicName();
					if (!completions.containsKey(pluginID) && pluginID.regionMatches(true, 0, pluginStart, 0, length) && !pluginID.equals(currentId))
						completions.put(pluginID, new TypeCompletionProposal(pluginID, getImage(F_TYPE_BUNDLE), pluginID, offset - length, length));
				}
			}
			return completions.values().toArray(new ICompletionProposal[completions.size()]);
		}
		int equals = currentValue.lastIndexOf('=');
		if (equals == -1 || index > equals)
			return matchValueCompletion(removeLeadingSpaces(currentValue.substring(index + 1)), new String[] {Constants.BUNDLE_VERSION_ATTRIBUTE}, new int[] {F_TYPE_ATTRIBUTE}, offset);
		String attributeValue = removeLeadingSpaces(currentValue.substring(index + 1));
		if (Constants.BUNDLE_VERSION_ATTRIBUTE.regionMatches(true, 0, attributeValue, 0, Constants.BUNDLE_VERSION_ATTRIBUTE.length())) {
			return getBundleVersionCompletions(currentValue.substring(0, index).trim(), removeLeadingSpaces(currentValue.substring(equals + 1)), offset);
		}
		return new ICompletionProposal[0];
	}

	protected ICompletionProposal[] handleRequireBundleCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue;
		if (comma > semicolon || comma == semicolon) {
			@SuppressWarnings("unchecked")
			HashSet<String> set = (HashSet<String>) fHeaders.get(Constants.REQUIRE_BUNDLE);
			if (set == null)
				set = parseHeaderForValues(currentValue, offset);
			return handleBundleCompletions(value, set, F_TYPE_BUNDLE, offset, false);
		}
		int equals = currentValue.lastIndexOf('=');
		if (equals == -1 || semicolon > equals) {
			String[] validAttrs = new String[] {Constants.BUNDLE_VERSION_ATTRIBUTE, Constants.RESOLUTION_DIRECTIVE, Constants.VISIBILITY_DIRECTIVE};
			Integer[] validTypes = new Integer[] {Integer.valueOf(F_TYPE_ATTRIBUTE), Integer.valueOf(F_TYPE_DIRECTIVE), Integer.valueOf(F_TYPE_DIRECTIVE)};
			return handleAttrsAndDirectives(value, initializeNewList(validAttrs), initializeNewList(validTypes), offset);
		}
		String attributeValue = removeLeadingSpaces(currentValue.substring(semicolon + 1));
		if (Constants.VISIBILITY_DIRECTIVE.regionMatches(true, 0, attributeValue, 0, Constants.VISIBILITY_DIRECTIVE.length()))
			return matchValueCompletion(currentValue.substring(equals + 1), new String[] {Constants.VISIBILITY_PRIVATE, Constants.VISIBILITY_REEXPORT}, new int[] {F_TYPE_VALUE, F_TYPE_VALUE}, offset, "VISIBILITY_"); //$NON-NLS-1$
		if (Constants.RESOLUTION_DIRECTIVE.regionMatches(true, 0, attributeValue, 0, Constants.RESOLUTION_DIRECTIVE.length()))
			return matchValueCompletion(currentValue.substring(equals + 1), new String[] {Constants.RESOLUTION_MANDATORY, Constants.RESOLUTION_OPTIONAL}, new int[] {F_TYPE_VALUE, F_TYPE_VALUE}, offset, "RESOLUTION_"); //$NON-NLS-1$
		if (Constants.BUNDLE_VERSION_ATTRIBUTE.regionMatches(true, 0, attributeValue, 0, Constants.RESOLUTION_DIRECTIVE.length())) {
			String pluginId = removeLeadingSpaces(currentValue.substring((comma == -1) ? 0 : comma + 1, semicolon));
			return getBundleVersionCompletions(pluginId, removeLeadingSpaces(currentValue.substring(equals + 1)), offset);
		}
		return new ICompletionProposal[0];
	}

	private ICompletionProposal[] getBundleVersionCompletions(String pluginID, String existingValue, int offset) {
		ModelEntry entry = PluginRegistry.findEntry(pluginID);
		if (entry != null) {
			IPluginModelBase[] hosts = entry.getActiveModels();
			ArrayList<TypeCompletionProposal> proposals = new ArrayList<>(hosts.length);
			for (IPluginModelBase host : hosts) {
				String proposalValue = getVersionProposal(host);
				if (proposalValue.regionMatches(0, existingValue, 0, existingValue.length()))
					proposals.add(new TypeCompletionProposal(proposalValue.substring(existingValue.length()), getImage(F_TYPE_VALUE), proposalValue, offset, 0));
			}
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		} else if (existingValue.length() == 0)
			return new ICompletionProposal[] {new TypeCompletionProposal("\"\"", getImage(F_TYPE_VALUE), "\"\"", offset, 0)}; //$NON-NLS-1$ //$NON-NLS-2$
		return new ICompletionProposal[0];
	}

	private String getVersionProposal(IPluginModelBase base) {
		StringBuilder buffer = new StringBuilder("\""); //$NON-NLS-1$
		BundleDescription desc = base.getBundleDescription();
		if (desc != null) {
			Version version = desc.getVersion();
			buffer.append(version.getMajor());
			buffer.append('.');
			buffer.append(version.getMinor());
			buffer.append('.');
			buffer.append(version.getMicro());
		} else {
			char[] chars = base.getPluginBase().getVersion().toCharArray();
			int periodCount = 0;
			for (char c : chars) {
				if (c == '.') {
					if (periodCount == 2)
						break;
					++periodCount;
				}
				buffer.append(c);
			}
		}
		return buffer.append('\"').toString();
	}

	private ICompletionProposal[] handleBundleCompletions(String value, Collection<String> doNotInclude, int type, int offset, boolean includeFragments) {
		value = removeLeadingSpaces(value);
		int length = value.length();
		doNotInclude.remove(value);
		ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
		IPluginModelBase[] bases = PluginRegistry.getActiveModels();
		for (IPluginModelBase base : bases) {
			BundleDescription desc = base.getBundleDescription();
			if (desc != null) {
				if (!includeFragments && desc.getHost() != null)
					continue;
				String bundleId = desc.getSymbolicName();
				if (bundleId.regionMatches(true, 0, value, 0, value.length()) && !doNotInclude.contains(bundleId))
					completions.add(new TypeCompletionProposal(bundleId, getImage(type), bundleId, offset - length, length));
			}
		}
		return completions.toArray(new ICompletionProposal[completions.size()]);
	}

	protected ICompletionProposal[] handleExportPackageCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		ArrayList<TypeCompletionProposal> list = new ArrayList<>();
		if (!insideQuotes(currentValue) && comma > semicolon || comma == semicolon) {
			String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue;
			@SuppressWarnings("unchecked")
			HashSet<String> set = (HashSet<String>) fHeaders.get(Constants.EXPORT_PACKAGE);
			if (set == null)
				set = parseHeaderForValues(currentValue, offset);
			value = removeLeadingSpaces(value);
			addPackageCompletions(value, set, offset, list);
		} else {
			String value = currentValue;
			if (comma > 0) {
				do {
					String prefix = currentValue.substring(0, comma);
					if (!insideQuotes(prefix)) {
						value = currentValue.substring(comma + 1);
						break;
					}
					comma = currentValue.lastIndexOf(',', comma - 1);
				} while (comma > 0);
			}
			int equals = currentValue.lastIndexOf('=');
			if (equals == -1 || semicolon > equals) {
				String[] validAttrs = new String[] {Constants.VERSION_ATTRIBUTE, ICoreConstants.INTERNAL_DIRECTIVE, ICoreConstants.FRIENDS_DIRECTIVE};
				Integer[] validTypes = new Integer[] {Integer.valueOf(F_TYPE_ATTRIBUTE), Integer.valueOf(F_TYPE_DIRECTIVE), Integer.valueOf(F_TYPE_DIRECTIVE)};
				return handleAttrsAndDirectives(value, initializeNewList(validAttrs), initializeNewList(validTypes), offset);
			}
			String attributeValue = removeLeadingSpaces(currentValue.substring(semicolon + 1));
			if (ICoreConstants.FRIENDS_DIRECTIVE.regionMatches(true, 0, attributeValue, 0, ICoreConstants.FRIENDS_DIRECTIVE.length()))
				return handleXFriendsCompletion(currentValue.substring(equals + 1), offset);
			if (ICoreConstants.INTERNAL_DIRECTIVE.regionMatches(true, 0, attributeValue, 0, ICoreConstants.INTERNAL_DIRECTIVE.length()))
				return handleTrueFalseValue(currentValue.substring(equals + 1), offset);
			if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, attributeValue, 0, Constants.VERSION_ATTRIBUTE.length())) {
				value = removeLeadingSpaces(currentValue.substring(equals + 1));
				if (value.length() == 0)
					return new ICompletionProposal[] {new TypeCompletionProposal("\"\"", getImage(F_TYPE_VALUE), "\"\"", offset, 0)}; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return list.toArray(new ICompletionProposal[list.size()]);
	}

	protected ICompletionProposal[] handleBundleActivatorCompletion(final String currentValue, final int offset) {
		ArrayList<Object> completions = new ArrayList<>();
		IProject project = ((PDEFormEditor) fSourcePage.getEditor()).getCommonProject();
		int startOffset = offset - currentValue.length();
		generateTypePackageProposals(currentValue, project, completions, startOffset, IJavaSearchConstants.CLASS);
		ICompletionProposal[] proposals = completions.toArray(new ICompletionProposal[completions.size()]);
		sortCompletions(proposals);
		return proposals;
	}

	protected ICompletionProposal[] handleBundleSymbolicNameCompletion(String currentValue, int offset) {
		int semicolon = currentValue.indexOf(';');
		if (semicolon != -1) {
			int equals = currentValue.indexOf('=');
			if (equals == -1) {
				String attribute = currentValue.substring(semicolon + 1);
				attribute = removeLeadingSpaces(attribute);
				Object o = fHeaders.get(Constants.BUNDLE_MANIFESTVERSION);
				int type = (o == null || o.toString().equals("1")) ? F_TYPE_ATTRIBUTE : F_TYPE_DIRECTIVE;//$NON-NLS-1$
				if (Constants.SINGLETON_DIRECTIVE.regionMatches(true, 0, attribute, 0, attribute.length())) {
					int length = attribute.length();
					TypeCompletionProposal proposal = new TypeCompletionProposal(Constants.SINGLETON_DIRECTIVE + ":=", //$NON-NLS-1$
							getImage(type), Constants.SINGLETON_DIRECTIVE, offset - length, length);
					proposal.setAdditionalProposalInfo(getJavaDoc("SINGLETON_DIRECTIVE")); //$NON-NLS-1$
					return new ICompletionProposal[] {proposal};
				}
			} else if (equals > semicolon)
				return handleTrueFalseValue(currentValue.substring(equals + 1), offset);
		}
		return new ICompletionProposal[0];
	}

	protected ICompletionProposal[] handleBundleActivationPolicyCompletion(final String currentValue, final int offset) {
		int semicolon = currentValue.lastIndexOf(';');
		if (semicolon == -1) {
			// we know there are no directives, therefore we are looking for a header value
			String value = removeLeadingSpaces(currentValue);
			String lazyValue = Constants.ACTIVATION_LAZY;
			int length = value.length();
			if (lazyValue.regionMatches(0, value, 0, length))
				return new ICompletionProposal[] {new TypeCompletionProposal(lazyValue, null, lazyValue, offset - length, length)};
		} else {
			int equals = currentValue.lastIndexOf('=');
			if (semicolon > equals) {
				// we know we are looking for a directive
				String[] validDirectives = new String[] {Constants.EXCLUDE_DIRECTIVE, Constants.INCLUDE_DIRECTIVE};
				Integer[] validTypes = new Integer[] {Integer.valueOf(F_TYPE_DIRECTIVE), Integer.valueOf(F_TYPE_DIRECTIVE)};
				return handleAttrsAndDirectives(currentValue, initializeNewList(validDirectives), initializeNewList(validTypes), offset);
			}
			int quote = currentValue.lastIndexOf('"');
			if (!insideQuotes(currentValue)) {
				// if quote > equals, that means the cursor is after a closing quote for the directive
				if (equals > quote)
					return new ICompletionProposal[] {new TypeCompletionProposal("\"\"", getImage(F_TYPE_VALUE), "\"\"", offset, 0)}; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				// We know we are looking for a completion inside the quote of a directive
				String value = currentValue.substring(quote + 1);
				// find existing packages
				StringTokenizer parser = new StringTokenizer(value, ","); //$NON-NLS-1$
				HashSet<String> set = new HashSet<>();
				while (parser.hasMoreTokens()) {
					set.add(parser.nextToken().trim());
				}
				// find the value of the package we are trying to find the completion for
				int comma = value.lastIndexOf(',');
				if (comma > -1)
					value = removeLeadingSpaces(value.substring(comma + 1));
				// find proposals
				ArrayList<TypeCompletionProposal> proposals = new ArrayList<>();
				addPackageCompletions(value, set, offset, proposals);
				return proposals.toArray(new ICompletionProposal[proposals.size()]);
			}
		}
		return new ICompletionProposal[0];
	}

	protected ICompletionProposal[] handleBuddyPolicyCompletion(String currentValue, int offset) {
		String[] validValues = new String[] {"dependent", "global", "registered", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"app", "ext", "boot", "parent"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		int comma = currentValue.lastIndexOf(',');
		if (comma != -1)
			currentValue = currentValue.substring(comma + 1);
		currentValue = removeLeadingSpaces(currentValue);
		ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
		ManifestElement[] elems = (ManifestElement[]) fHeaders.get(ICoreConstants.ECLIPSE_BUDDY_POLICY);
		HashSet<String> set = new HashSet<>();
		if (elems != null)
			for (ManifestElement elem : elems)
				set.add(elem.getValue());
		int length = currentValue.length();
		for (int i = 0; i < validValues.length; i++)
			if (validValues[i].regionMatches(true, 0, currentValue, 0, length) && !set.contains(validValues[i]))
				completions.add(new TypeCompletionProposal(validValues[i], getImage(F_TYPE_VALUE), validValues[i], offset - length, length));
		return completions.toArray(new ICompletionProposal[completions.size()]);
	}

	protected ICompletionProposal[] handleRequiredExecEnv(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		if (comma != -1)
			currentValue = currentValue.substring(comma + 1);
		currentValue = removeLeadingSpaces(currentValue);
		ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
		@SuppressWarnings("unchecked")
		HashSet<String> set = (HashSet<String>) fHeaders.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (set == null)
			set = new HashSet<>(0);
		int length = currentValue.length();
		for (int i = 0; i < fExecEnvs.length; i++)
			if (fExecEnvs[i].regionMatches(true, 0, currentValue, 0, length) && !set.contains(fExecEnvs[i]))
				completions.add(new TypeCompletionProposal(fExecEnvs[i], getImage(F_TYPE_EXEC_ENV), fExecEnvs[i], offset - length, length));
		return completions.toArray(new ICompletionProposal[completions.size()]);
	}

	protected ICompletionProposal[] handleTrueFalseValue(String currentValue, int offset) {
		currentValue = removeLeadingSpaces(currentValue);
		int length = currentValue.length();
		if (length == 0)
			return new ICompletionProposal[] {new TypeCompletionProposal("true", getImage(F_TYPE_VALUE), "true", offset, 0), //$NON-NLS-1$ //$NON-NLS-2$
					new TypeCompletionProposal("false", getImage(F_TYPE_VALUE), "false", offset, 0) //$NON-NLS-1$ //$NON-NLS-2$
			};
		else if (length < 5 && "true".regionMatches(true, 0, currentValue, 0, length)) //$NON-NLS-1$
			return new ICompletionProposal[] {new TypeCompletionProposal("true", getImage(F_TYPE_VALUE), "true", offset - length, length) //$NON-NLS-1$ //$NON-NLS-2$
			};
		else if (length < 6 && "false".regionMatches(true, 0, currentValue, 0, length)) //$NON-NLS-1$
			return new ICompletionProposal[] {new TypeCompletionProposal("false", getImage(F_TYPE_VALUE), "false", offset - length, length) //$NON-NLS-1$ //$NON-NLS-2$
			};
		return new ICompletionProposal[0];
	}

	protected ICompletionProposal[] handleEclipseBundleShape(String currentValue, int offset) {
		currentValue = removeLeadingSpaces(currentValue);
		ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
		int length = currentValue.length();
		String[] values = ICoreConstants.SHAPE_VALUES;
		for (int i = 0; i < values.length; i++)
			if (values[i].regionMatches(true, 0, currentValue, 0, length) && !currentValue.equals(values[i]))
				completions.add(new TypeCompletionProposal(values[i], getImage(F_TYPE_VALUE), values[i], offset - length, length));
		return completions.toArray(new ICompletionProposal[completions.size()]);
	}

	protected ICompletionProposal[] matchValueCompletion(String value, String[] attrs, int[] types, int offset) {
		return matchValueCompletion(value, attrs, types, offset, ""); //$NON-NLS-1$
	}

	protected ICompletionProposal[] matchValueCompletion(String value, String[] attrs, int[] types, int offset, String prefixCostant) {
		ArrayList<TypeCompletionProposal> list = new ArrayList<>();
		int length = value.length();
		TypeCompletionProposal proposal = null;
		for (int i = 0; i < attrs.length; i++)
			if (attrs[i].regionMatches(true, 0, value, 0, length)) {
				if (types[i] == F_TYPE_ATTRIBUTE) {
					proposal = new TypeCompletionProposal(attrs[i] + "=", getImage(F_TYPE_ATTRIBUTE), attrs[i], offset - length, length); //$NON-NLS-1$
					proposal.setAdditionalProposalInfo(getJavaDoc(attrs[i] + "_ATTRIBUTE")); //$NON-NLS-1$
				} else if (types[i] == F_TYPE_DIRECTIVE) {
					proposal = new TypeCompletionProposal(attrs[i] + ":=", getImage(F_TYPE_DIRECTIVE), attrs[i], offset - length, length); //$NON-NLS-1$
					proposal.setAdditionalProposalInfo(getJavaDoc(attrs[i] + "_DIRECTIVE")); //$NON-NLS-1$
				} else {
					proposal = new TypeCompletionProposal(attrs[i], getImage(types[i]), attrs[i], offset - length, length);
					proposal.setAdditionalProposalInfo(getJavaDoc(prefixCostant + attrs[i]));
				}
				list.add(proposal);
			}
		return list.toArray(new ICompletionProposal[list.size()]);
	}

	protected ICompletionProposal[] handleAttrsAndDirectives(String value, ArrayList<Object> attrs, ArrayList<Object> types, int offset) {
		String fullValue = findFullLine(value, offset, false);
		int semicolon = value.lastIndexOf(';');
		value = removeLeadingSpaces(value.substring(semicolon + 1));
		StringTokenizer tokenizer = new StringTokenizer(fullValue, ";"); //$NON-NLS-1$
		// remove value part of the String, the rest of the tokens will be attributes/directives
		tokenizer.nextToken();
		while (tokenizer.hasMoreTokens()) {
			String tokenValue = removeLeadingSpaces(tokenizer.nextToken());
			int index = tokenValue.indexOf('=');
			if (index == -1)
				continue;
			if (tokenValue.charAt(index - 1) == ':')
				--index;
			tokenValue = tokenValue.substring(0, index);
			int indexOfObject = attrs.indexOf(tokenValue);
			if (indexOfObject >= 0) {
				attrs.remove(indexOfObject);
				types.remove(indexOfObject);
			}
		}
		return matchValueCompletion(value, attrs.toArray(new String[attrs.size()]), toIntArray(types), offset);
	}

	/**
	 * Adds completions to the proposals list which represent packages in source folders found in the current project
	 *
	 * @param value the current incomplete package value (without any leading spaces)
	 * @param currentPackages a Set containing the packages already specified by the Manifest Header
	 * @param offset the offset of the current completion proposal
	 * @param proposals the list to which new completion proposals will be added
	 */
	private void addPackageCompletions(String value, Set<String> currentPackages, int offset, ArrayList<TypeCompletionProposal> proposals) {
		int length = value.length();
		IProject proj = ((PDEFormEditor) fSourcePage.getEditor()).getCommonProject();
		if (proj != null) {
			IJavaProject jp = JavaCore.create(proj);
			IPackageFragment[] frags = PDEJavaHelper.getPackageFragments(jp, currentPackages, false);
			for (IPackageFragment frag : frags) {
				String name = frag.getElementName();
				if (name.regionMatches(true, 0, value, 0, length))
					proposals.add(new TypeCompletionProposal(name, getImage(F_TYPE_PKG), name, offset - length, length));
			}
		}
	}

	private HashSet<String> parseHeaderForValues(String currentValue, int offset) {
		HashSet<String> set = new HashSet<>();
		String fullValue = findFullLine(currentValue, offset, true);
		StringTokenizer tokenizer = new StringTokenizer(fullValue, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String pkgValue = tokenizer.nextToken();
			int index = pkgValue.indexOf(';');
			set.add(index == -1 ? pkgValue.trim() : pkgValue.substring(0, index).trim());
		}
		return set;
	}

	private String findFullLine(String value, int offset, boolean entireHeader) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		try {
			int line = doc.getLineOfOffset(offset);
			String newValue = ""; //$NON-NLS-1$
			int startOfLine = 0;
			int colon = -1;
			do {
				startOfLine = doc.getLineOffset(line);
				newValue = doc.get(offset, doc.getLineLength(line) - offset + startOfLine);
				++line;
				colon = newValue.lastIndexOf(':');
			} while ((colon == -1 || (newValue.length() > colon && newValue.charAt(colon + 1) == '=')) && (entireHeader || newValue.indexOf(',') == -1) && !(doc.getNumberOfLines() == line));

			if (colon > 0 && newValue.charAt(colon + 1) != '=') {
				newValue = doc.get(offset, startOfLine - 1 - offset);
			} else {
				// break on the comma to find our element, but only break on a comma that is not enclosed in parenthesis
				int comma = newValue.indexOf(',');
				int parenthesis = newValue.indexOf('"');
				if (!(parenthesis < comma && newValue.indexOf('"', parenthesis + 1) > comma))
					newValue = (comma != -1) ? newValue.substring(0, comma) : newValue;
			}
			return value.concat(newValue);
		} catch (BadLocationException e) {
		}
		return ""; //$NON-NLS-1$
	}

	private int[] toIntArray(ArrayList<Object> list) {
		int[] result = new int[list.size()];
		int i = -1;
		while (++i < result.length) {
			Object o = list.get(i);
			if (!(o instanceof Integer))
				return new int[0];
			result[i] = ((Integer) o).intValue();
		}
		return result;
	}

	// if you use java.util.Arrays.asList(), we get an UnsupportedOperation later in the code
	protected final ArrayList<Object> initializeNewList(Object[] values) {
		ArrayList<Object> list = new ArrayList<>(values.length);
		for (Object value : values)
			list.add(value);
		return list;
	}

	private boolean insideQuotes(String value) {
		char[] chars = value.toCharArray();
		int numOfQuotes = 0;
		for (char c : chars)
			if (c == '\"')
				++numOfQuotes;
		int j = numOfQuotes % 2;
		return j == 1;
	}

	@Override
	public void assistSessionEnded(ContentAssistEvent event) {
		fHeaders = null;
	}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) {
	}

	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
	}

	public Image getImage(int type) {
		if (type >= 0 && type < F_TOTAL_TYPES)
			if (fImages[type] == null) {
				switch (type) {
					case F_TYPE_HEADER :
						return fImages[type] = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
					case F_TYPE_PKG :
						return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_PACKAGE);
					case F_TYPE_BUNDLE :
						return fImages[type] = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
					case F_TYPE_CLASS :
						return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_CLASS);
					case F_TYPE_ATTRIBUTE :
						return fImages[type] = PDEPluginImages.DESC_ATT_URI_OBJ.createImage();
					case F_TYPE_DIRECTIVE :
						fImages[F_TYPE_ATTRIBUTE] = PDEPluginImages.DESC_ATT_URI_OBJ.createImage();
						ImageOverlayIcon icon = new ImageOverlayIcon(fImages[F_TYPE_ATTRIBUTE], new ImageDescriptor[][] {new ImageDescriptor[] {PDEPluginImages.DESC_DOC_CO}, null, null, null});
						return fImages[type] = icon.createImage();
					case F_TYPE_EXEC_ENV :
						return fImages[type] = PDEPluginImages.DESC_JAVA_LIB_OBJ.createImage();
					case F_TYPE_VALUE :
						return null;
				}
			} else
				return fImages[type];
		return null;
	}

	public void dispose() {
		for (int i = 0; i < fImages.length; i++)
			if (fImages[i] != null && !fImages[i].isDisposed())
				fImages[i].dispose();
	}

	private String getJavaDoc(String constant) {
		if (fJP == null) {
			IProject project = ((PDEFormEditor) fSourcePage.getEditor()).getCommonProject();
			fJP = JavaCore.create(project);
		}
		return PDEJavaHelperUI.getOSGIConstantJavaDoc(constant, fJP);
	}

}
