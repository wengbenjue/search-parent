package com.aug3.storage.mongoclient;

public enum Operator {
	NEAR("$near"),
	NEAR_SPHERE("$nearSphere"),
	WITHIN("$within"),
	WITHIN_CIRCLE("$center"),
	WITHIN_CIRCLE_SPHERE("$centerSphere"),
	WITHIN_BOX("$box"),
	EQUAL("$eq"),
	GREATER_THAN("$gt"),
	GREATER_THAN_OR_EQUAL("$gte"),
	LESS_THAN("$lt"),
	LESS_THAN_OR_EQUAL("$lte"),
	EXISTS("$exists"),
	TYPE("$type"),
	NOT("$not"),
	MOD("$mod"),
	SIZE("$size"),
	IN("$in"),
	NOT_IN("$nin"),
	ALL("$all"),
	ELEMENT_MATCH("$elemMatch"),
	NOT_EQUAL("$ne"),
	WHERE("$where");
	
	private String value;
	private Operator(String val) {
		value = val;
	}
	
	private boolean equals(String val) {
		return value.equals(val);
	}

	public String val() { return value;}
	
	public static Operator fromString(String val) {
		for (int i = 0; i < values().length; i++) {
			Operator fo = values()[i];
			if(fo.equals(val)) return fo;
		}
		return null;
	}
}