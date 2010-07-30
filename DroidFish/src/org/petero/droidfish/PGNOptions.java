package org.petero.droidfish;

/** Settings controlling PGN import/export */
public class PGNOptions {
	public static class Export {
		public boolean variations;
		public boolean comments;
		public boolean nag;
		public boolean userCmd;
		public boolean clockInfo;
	}
	public static class Import {
		public boolean variations;
		public boolean comments;
		public boolean nag;
	}

	public Export exp;
	public Import imp;

	PGNOptions() {
		exp = new Export();
		imp = new Import();
	}
}
