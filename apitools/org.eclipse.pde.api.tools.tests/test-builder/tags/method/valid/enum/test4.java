package a.b.c;


/**
 * Test supported @noreference tag on methods in outer / inner enums
 */
public enum test4 {
	A;
	enum inner {
		A;
		/**
		 * @noreference
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noreference
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		enum inner2 {
			A;
			/**
			 * @noreference
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noreference
			 * @return
			 */
			public final char m2() {
				return 's';
			}
		}
	}
}

enum outer {
	A;
	/**
	 * @noreference
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noreference
	 * @return
	 */
	public final char m2() {
		return 's';
	}
}
