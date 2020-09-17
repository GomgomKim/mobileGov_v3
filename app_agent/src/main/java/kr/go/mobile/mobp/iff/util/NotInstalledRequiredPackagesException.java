package kr.go.mobile.mobp.iff.util;

public class NotInstalledRequiredPackagesException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private final String label;
	private final String pkgname;
	
	public NotInstalledRequiredPackagesException(String label, String pkgname) {
		super();
		this.label = label;
		this.pkgname = pkgname;
	}
	
	public String getPackageLabel() {
		return this.label;
	}
	
	public String getNotInstalledPackageName() {
		return this.pkgname;
	}

}
