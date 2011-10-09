package com.covenant;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Pair;

public class EntityManager {

	private static EntityManager _instance;
	private DbManager _dbManager;

	private EntityManager() {}

	public static void init(DbManager dbManager) {
		EntityManager em = EntityManager.getInstance();
		em._setDbManager(dbManager);
	}

	public static EntityManager getInstance() {
		if (_instance == null) {
			_instance = new EntityManager();
		}
		return _instance;
	}

	private void _setDbManager(DbManager dbManager) {
		this._dbManager = dbManager;
	}

	public boolean save(Object entity) {
		// Get the class of the entity and verify that it is a persistable entity
		Class<? extends Object> entityClassType = entity.getClass();
		_verifyIsEntity(entityClassType);

		String table = entityClassType.getAnnotation(Entity.class).table();
		ContentValues values;

		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClassType);
		Field pkColumn = this._getPK(fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		values = new ContentValues();
		// Get all of the fields that are persistable EntityFields
		Field field;
		Column column;
		for (Pair<Field, Column> fieldColumnPair : fieldColumnPairs) {
			field = fieldColumnPair.first;
			column = fieldColumnPair.second;
			try {
				if (field.getType() == String.class) {
					values.put(column.name(), field.get(entity).toString());
				} else if (field.getType() == int.class) {
					values.put(column.name(), field.getInt(entity));
				} else if (field.getType() == long.class) {
					values.put(column.name(), field.getLong(entity));
				}

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		// If the primary key is 0, insert
		if (values.getAsLong(pkColumnName) == 0) {
			values.remove(pkColumnName);
			long pkValue = _dbManager.getDb().insert(table, null, values);
			try {
				pkColumn.setLong(entity, pkValue);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return pkValue > 0;
		}
		// Else, update
		else {
			long pkValue = values.getAsLong(pkColumnName);
			values.remove(pkColumnName);
			return _dbManager.getDb().update(table, values, pkColumnName + "=" + pkValue, null) > 0;
		}
	}
	
	public <T> int delete(Object entity) {
		Class<? extends Object> entityClassType = entity.getClass();
		_verifyIsEntity(entityClassType);
		
		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClassType);
		Field pkColumn = this._getPK(fieldColumnPairs);
		
		try {
			return this.deleteByPk(entity, pkColumn.getLong(entity));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public void execSQL(String sql) {
		this._dbManager.getDb().execSQL(sql);
	}
	
	public <T> T fetchOneBy(T entity, String whereClause) {
		List<T> entityList = this.fetchBy(entity, whereClause);
		T firstEntity = null;
		if (entityList.size() > 0) {
			firstEntity = entityList.get(0);
		}
		return firstEntity;
	}

	public <T> T fetchOneByPK(T entity, long pkValue) {
		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entity.getClass());
		Field pkColumn = this._getPK(fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		return this.fetchOneBy(entity, pkColumnName + " = " + pkValue);
	}

	public <T> List<T> fetchAll(T entityClass) {
		_verifyIsEntity(entityClass.getClass());
		String table = entityClass.getClass().getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(table, new String[] { "*" }, null, null, null, null, null);
		
		return fetchFromCursor(entityClass, c);
	}
	
	public <T> List<T> fetchBy(T entityClass, String whereClause) {
		_verifyIsEntity(entityClass.getClass());
		String table = entityClass.getClass().getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(table, new String[] { "*" }, whereClause, null, null, null, null);

		return fetchFromCursor(entityClass, c);
	}

	public <T> List<T> fetchBy(T entityClass, CovenantQuery covenantQuery) {
		_verifyIsEntity(entityClass.getClass());
		String table = entityClass.getClass().getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(covenantQuery.getDistinct(), table, null, covenantQuery.getWhere(), null, covenantQuery.getGroupBy(), covenantQuery.getHaving(), covenantQuery.getOrderBy(), covenantQuery.getLimit());
	
		return fetchFromCursor(entityClass, c);
	}
	
	public <T> int deleteByPk(T entityClass, long pkValue) {
		_verifyIsEntity(entityClass.getClass());
		String table = entityClass.getClass().getAnnotation(Entity.class).table();
		
		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass.getClass());
		Field pkColumn = this._getPK(fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		return this._dbManager.getDb().delete(table, pkColumnName + " =?", new String[] { Long.toString(pkValue) });
	}
	
	public <T> int deleteWhere(T entityClass, String whereClause) {
		_verifyIsEntity(entityClass.getClass());
		String table = entityClass.getClass().getAnnotation(Entity.class).table();

		return this._dbManager.getDb().delete(table, whereClause, null);
	}
		
	public void startTransaction() {
		this._dbManager.getDb().beginTransaction();
	}

	public void endTransaction() {
		this._dbManager.getDb().setTransactionSuccessful();
		this._dbManager.getDb().endTransaction();
	}

	private Field _getPK(List<Pair<Field, Column>> fieldColumnPairs) {
		Field pkColumn = null;
		for (Pair<Field, Column> fieldColumnPair : fieldColumnPairs) {
			fieldColumnPair.first.setAccessible(true);
			Column column = fieldColumnPair.first.getAnnotation(Column.class); 
			if (column != null) {
				if (column.PK()) {
					pkColumn = fieldColumnPair.first;
					break;
				}
			}
		}
		return pkColumn;
	}
	
	private boolean _verifyIsEntity(Class<? extends Object> entityClassType) {
		return entityClassType.isAnnotationPresent(Entity.class);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> fetchFromCursor(T entityClass, Cursor c) {
		List<T> returnList = new ArrayList<T>();
		try {
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						T current = (T) entityClass.getClass().newInstance();
						Field[] fields = entityClass.getClass().getDeclaredFields();
						for (Field field : fields) {
							field.setAccessible(true);
							Column column = field.getAnnotation(Column.class); 
							if (column != null) {
								if (field.getType() == String.class) {
									field.set(current, c.getString(c.getColumnIndex(column.name())));
								} else if (field.getType() == int.class) {
									field.setInt(current, c.getInt(c.getColumnIndex(column.name())));
								} else if (field.getType() == long.class) {
									field.setLong(current, c.getLong(c.getColumnIndex(column.name())));
								}
							}
						}
						returnList.add(current);
					} while (c.moveToNext());
					c.close();
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		finally {
			c.close();
		}

		return returnList;
	}
	
	private List<Pair<Field, Column>> _getColumns(Class<? extends Object> entityClassType) {
		List<Pair<Field, Column>> fieldCoumnPairs  = new ArrayList<Pair<Field, Column>>();

		Field[] fields = entityClassType.getDeclaredFields();

		for (Field field : fields) {
			field.setAccessible(true);
			Column column = field.getAnnotation(Column.class); 
			if (column != null) {
				field.setAccessible(true);
				fieldCoumnPairs.add(new Pair<Field, Column>(field, column));
			}
		}
		return fieldCoumnPairs;
	}
}
