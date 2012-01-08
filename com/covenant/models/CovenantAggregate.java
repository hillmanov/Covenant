package com.covenant.models;

import com.covenant.Column;

public class CovenantAggregate {
	
	public static String SUM = "sum";
	public static String COUNT = "count";
	public static String MIN = "min";
	public static String MAX = "max";
	public static String AVG = "avg";
	
	@Column(name="sum")
	private int sum;
	
	@Column(name="count")
	private int count;
	
	@Column(name="min")
	private int min;
	
	@Column(name="max")
	private int max;
	
	@Column(name="avg")
	private int avg;

	public int getSum() {
		return sum;
	}

	public int getCount() {
		return count;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public int getAvg() {
		return avg;
	}
}
