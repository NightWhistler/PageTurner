package net.nightwhistler.pageturner.library;

public class Author {

	private String firstName;
	private String lastName;
	
	private String authorKey;
	
	public Author(String firstName, String lastName ) {
		this.firstName = firstName;
		this.lastName = lastName;
		
		this.authorKey = firstName.toLowerCase() + "_" + lastName.toLowerCase(); 
	}
	
	public String getAuthorKey() {
		return authorKey;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
}
