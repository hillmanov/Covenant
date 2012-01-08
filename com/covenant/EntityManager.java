package com.covenant;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.covenant.models.CovenantAggregate;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Pair;

public class EntityManager {

	private static EntityManager _instance;
	private DbManager _dbManager;

	private Map<Class<? extends Object>, String> tableNameCache;
	private Map<Class<? extends Object>, List<Pair<Field, Column>>> fieldColumnPairCache;
	private Map<Class<? extends Object>, Field> pkColumnCache;

	private EntityManager() {
		tableNameCache = new HashMap<Class<? extends Object>, String>();
		fieldColumnPairCache = new HashMap<Class<? extends Object>, List<Pair<Field, Column>>>();
		pkColumnCache = new HashMap<Class<? extends Object>, Field>();
	}

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
	
	public CovenantAggregate fetchAggregate(String aggregate, Class<?> entityClass, String column, String whereClause)
	{
		String table = tableNameCache.get(entityClass);
		if (table == null) {
			table = entityClass.getAnnotation(Entity.class).table();
			tableNameCache.put(entityClass, table);
		}
		
		StringBuilder aggregateQuery = new StringBuilder();
		
		aggregateQuery.append("SELECT " + aggregate + "('" + column + "') as '" + aggregate + "' from '" + table + "'");
		if (whereClause != null)
		{
			aggregateQuery.append(" WHERE " + whereClause);
		}
		
		String sql = aggregateQuery.toString();
		
		Cursor c = this._dbManager.getDb().rawQuery(sql, null);
		
		List<CovenantAggregate> covenantAggregates = fetchFromCursor(CovenantAggregate.class, c); 
		
		if (covenantAggregates.size() > 0) {
			return covenantAggregates.get(0);
		}
		
		return null;
	}
	

	public boolean save(Object entity) {
		// Get the class of the entity and verify that it is persistable
		Class<? extends Object> entityClass = entity.getClass();
		_verifyIsEntity(entityClass);

		String table = tableNameCache.get(entityClass);
		if (table == null) {
			table = entityClass.getAnnotation(Entity.class).table();
			tableNameCache.put(entityClass, table);
		}

		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass);
		Field pkColumn = this._getPK(entityClass, fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		ContentValues values;
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

	public void createTables(boolean dropIfExists, Class<?>... entityClasses) {
		for (Class<? extends Object> entityClass : entityClasses) {
			// Get the class of the entity and verify that it is persistable
			_verifyIsEntity(entityClass);

			String table = entityClass.getAnnotation(Entity.class).table();

			List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass);

			StringBuilder createQueryBuilder = new StringBuilder();
			Column column = null;
			Field field = null;
			int count = fieldColumnPairs.size();
			int index = 0;
			if (dropIfExists) {
				this._dbManager.getDb().execSQL("DROP TABLE IF EXISTS " + table);
			}
			createQueryBuilder.append("CREATE TABLE " + table + " ( ");
			for (Pair<Field, Column> fieldColumnPair : fieldColumnPairs) {
				field = fieldColumnPair.first;
				column = fieldColumnPair.second;
				if (column.PK() == true) {
					createQueryBuilder.append(column.name() + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL");
				} else {
					if (field.getType() == String.class) {
						createQueryBuilder.append(column.name() + " VARCHAR");
					}
					else if (field.getType() == int.class) {
						createQueryBuilder.append(column.name() + " INTEGER");
					}
					else {
						createQueryBuilder.append(column.name() + " VARCHAR");
					}
				}
				if (++index < count) {
					createQueryBuilder.append(",");
				}
			}
			createQueryBuilder.append(")");

			String createQuery = createQueryBuilder.toString();

			this._dbManager.getDb().execSQL(createQuery);
		}
	}

	public <T> int delete(Object entity) {
		Class<? extends Object> entityClass = entity.getClass();
		_verifyIsEntity(entityClass);

		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass);
		Field pkColumn = this._getPK(entityClass, fieldColumnPairs);

		try {
			return this.deleteByPk(entityClass, pkColumn.getLong(entity));
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

	public <T> T fetchOneBy(Class<?> entityClass, String whereClause) {

		List<T> entityList = this.fetchBy(entityClass, whereClause);
		T firstEntity = null;
		if (entityList.size() > 0) {
			firstEntity = entityList.get(0);
		}
		return firstEntity;
	}

	public <T> T fetchOneByPK(Class<?> entityClass, long pkValue) {
		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass);
		Field pkColumn = this._getPK(entityClass, fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		return this.fetchOneBy(entityClass, pkColumnName + " = " + pkValue);
	}

	public <T> List<T> fetchAll(Class<? extends Object> entityClass) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(table, new String[] { "*" }, null, null, null, null, null);

		return fetchFromCursor(entityClass, c);
	}

	public <T> List<T> fetchBy(Class<? extends Object> entityClass, String whereClause) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(table, new String[] { "*" }, whereClause, null, null, null, null);

		return fetchFromCursor(entityClass, c);
	}

	public <T> List<T> fetchBy(Class<? extends Object> entityClass, CovenantQuery covenantQuery) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(covenantQuery.getDistinct(), table, null, covenantQuery.getWhere(), null,
				covenantQuery.getGroupBy(), covenantQuery.getHaving(), covenantQuery.getOrderBy(), covenantQuery.getLimit());

		return fetchFromCursor(entityClass, c);
	}

	public <T> int deleteByPk(Class<? extends Object> entityClass, long pkValue) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();

		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass.getClass());
		Field pkColumn = this._getPK(entityClass, fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		return this._dbManager.getDb().delete(table, pkColumnName + " =?", new String[] { Long.toString(pkValue) });
	}

	public <T> int deleteWhere(Class<? extends Object> entityClass, String whereClause) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();

		return this._dbManager.getDb().delete(table, whereClause, null);
	}

	public void startTransaction() {
		this._dbManager.getDb().beginTransaction();
	}

	public void endTransaction() {
		this._dbManager.getDb().setTransactionSuccessful();
		this._dbManager.getDb().endTransaction();
	}

	private Field _getPK(Class<? extends Object> entityClass, List<Pair<Field, Column>> fieldColumnPairs) {
		Field pkColumn = pkColumnCache.get(entityClass);

		if (pkColumn == null) {
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
			pkColumnCache.put(entityClass, pkColumn);
		}
		return pkColumn;
	}

	private boolean _verifyIsEntity(Class<? extends Object> entityClass) {
		return entityClass.isAnnotationPresent(Entity.class);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> fetchFromCursor(Class<? extends Object> entityClass, Cursor c) {
		List<T> returnList = new ArrayList<T>();
		try {
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						T current = (T) entityClass.newInstance();
						Field[] fields = entityClass.getDeclaredFields();
						for (Field field : fields) {
							field.setAccessible(true);
							Column column = field.getAnnotation(Column.class);
							if (column != null && (c.getColumnIndex(column.name()) >= 0)) {
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
		} finally {
			c.close();
		}

		return returnList;
	}

	private List<Pair<Field, Column>> _getColumns(Class<? extends Object> entityClass) {
		List<Pair<Field, Column>> fieldCoumnPairs = fieldColumnPairCache.get(entityClass);
		if (fieldCoumnPairs == null) {
			fieldCoumnPairs = new ArrayList<Pair<Field, Column>>();

			Field[] fields = entityClass.getDeclaredFields();

			for (Field field : fields) {
				field.setAccessible(true);
				Column column = field.getAnnotation(Column.class);
				if (column != null) {
					field.setAccessible(true);
					fieldCoumnPairs.add(new Pair<Field, Column>(field, column));
				}
			}
			fieldColumnPairCache.put(entityClass, fieldCoumnPairs);
		}
		return fieldCoumnPairs;
	}
}
