package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;

/**
 * General utility class.
 */
public final class Utils implements IPDECoreConstants {

/**
 * Convert a list of tokens into an array. The list separator has to be specified.
 */
public static String[] getArrayFromString(String list, String separator) {
	if (list == null || list.trim().equals(""))
		return new String[0];
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return (String[]) result.toArray(new String[result.size()]);
}

/**
 * convert a list of comma-separated tokens into an array
 */
public static String[] getArrayFromString(String list) {
	return getArrayFromString(list, ",");
}

/**
 * Substitutes a word in a sentence.
 */
public static String substituteWord(String sentence, String oldWord, String newWord) {
	int index = sentence.indexOf(oldWord);
	if (index == -1)
		return sentence;
	StringBuffer sb = new StringBuffer();
	sb.append(sentence.substring(0, index));
	sb.append(newWord);
	sb.append(sentence.substring(index + oldWord.length()));
	return sb.toString();
}

/**
 * Finds out if an status has the given severity. In case of a multi status,
 * its children are also included.
 */
public static boolean contains(IStatus status, int severity) {
	if (status.matches(severity))
		return true;
	if (status.isMultiStatus()) {
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++)
			if (contains(children[i], severity))
				return true;
	}
	return false;
}

/**
 * Converts an array of strings into an array of URLs.
 */
public static URL[] asURL(String[] target) throws CoreException {
	if (target == null)
		return null;
	try {
		URL[] result = new URL[target.length];
		for (int i = 0; i < target.length; i++)
			result[i] = new URL(target[i]);
		return result;
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_MALFORMED_URL, e.getMessage(), e));
	}
}
}
