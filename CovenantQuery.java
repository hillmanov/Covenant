package com.covenant;

public class CovenantQuery {

	private String fromTable;
	private String where;
	private String orderBy;
	private String groupBy;
	private String having;
	private String limit;
	private boolean distinct;
	
	public CovenantQuery() {
		this.fromTable = null;
		this.where = null;
		this.orderBy = null;
		this.limit = null;
		this.distinct = false;
	}
	
	public CovenantQuery setFromTable(String fromTable) {
		this.fromTable = fromTable;
		return this;
	}
	
	public CovenantQuery setWhere(String where) {
		this.where = where;
		return this;
	}
	
	public CovenantQuery setLimit(int limit) {
		this.limit = Integer.toString(limit);
		return this;
	}
	
	public CovenantQuery setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}
	
	public CovenantQuery setGroupBy(String groupBy) {
		this.groupBy = groupBy;
		return this;
	}
	
	public CovenantQuery setHaving(String having) {
		this.having = having;
		return this;
	}
	
	public CovenantQuery setDistinct(boolean distinct) {
		this.distinct = distinct;
		return this;
	}

	public String getFromTable() {
		return fromTable;
	}

	public String getWhere() {
		return where;
	}
	
	public String getGroupBy() {
		return groupBy;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public String getLimit() {
		return limit;
	}
	
	public String getHaving() {
		return having;
	}

	public boolean getDistinct() {
		return distinct;
	}
	
}
