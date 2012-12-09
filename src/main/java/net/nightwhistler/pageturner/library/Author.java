/*
 * Copyright (C) 2011 Alex Kuiper
 * 
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.nightwhistler.pageturner.library;

import java.io.Serializable;
import java.util.Locale;

public class Author implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9027442126212861173L;
	
	private String firstName;
	private String lastName;
	
	private String authorKey;
	
	public Author(String firstName, String lastName ) {
		this.firstName = firstName;
		this.lastName = lastName;
		
		this.authorKey = firstName.toLowerCase(Locale.US) + "_" + lastName.toLowerCase(Locale.US); 
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
