package $packageName$;

import java.util.Set;

public class DictionaryImpl implements Dictionary {

	private final Set<String> fWords = Set.of("$word1$", "$word2$", "$word3$");
	private final String fLanguage = "$language$";

	public String getLanguage() {
		return fLanguage;
	}

	public boolean check(String word) {
		return fWords.contains(word);
	}

	public String toString() {
		return fLanguage;
	}

}
