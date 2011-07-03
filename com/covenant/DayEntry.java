package com.covenant;

@Entity(tableName = "day_entry")
public class DayEntry {
	
	@EntityField(PK = true, column = "id")
	private long id;
	
	@EntityField(column = "date")
	private String date;
	
	@EntityField(column = "steps")
	private int steps;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getSteps() {
		return steps;
	}

	public void setSteps(int steps) {
		this.steps = steps;
	}
	
	public String toString() {
		return "ID: " + this.id + " Date: " + this.date + " Steps: " + this.steps;
	}
	
	
}
