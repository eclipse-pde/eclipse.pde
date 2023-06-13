package $packageName$;

import java.util.Set;

public class DictionaryImpl implements Dictionary {

	private static final Set<String> WORDS = Set.of("$word1$", "$word2$", "$word3$");
	private static final String LANGUAGE = "$language$";

	public String getLanguage() {
		return LANGUAGE;
	}

	public boolean check(String word) {
		return WORDS.contains(word);
	}

	public String toString() {
		return LANGUAGE;
	}

}
