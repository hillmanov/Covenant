package com.covenant;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Pair;

public class EntityManager {

	private static EntityManager _instance;
	private DbManager _dbManager;

	private Map<Class<?>, String> tableNameCache;
	private Map<Class<?>, List<Pair<Field, Column>>> fieldColumnPairCache;
	private Map<Class<?>, Field> pkColumnCache;

	private EntityManager() {
		tableNameCache = new HashMap<Class<?>, String>();
		fieldColumnPairCache = new HashMap<Class<?>, List<Pair<Field, Column>>>();
		pkColumnCache = new HashMap<Class<?>, Field>();
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

	public boolean save(Object entity) {
		// Get the class of the entity and verify that it is persistable
		Class<?> entityClass = entity.getClass();
		_verifyIsEntity(entityClass);

		String tableName = tableNameCache.get(entityClass);
		if (tableName == null) {
			tableName = entityClass.getAnnotation(Entity.class).table();
			tableNameCache.put(entityClass, tableName);

			// We will only need to do this the first time as well
			if (this._dbManager.getMode() == DbManager.AUTO) {
				createTable(tableName, entity);
			}
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
			long pkValue = _dbManager.getDb().insert(tableName, null, values);
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
			return _dbManager.getDb().update(tableName, values, pkColumnName + "=" + pkValue, null) > 0;
		}
	}

	private void createTable(String table, Object entity) {
		Cursor c = _dbManager.getDb().rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '" + table + "'", null);
		if (c.getCount() == 0) {
			createTables(false, entity.getClass());
		}
	}

	public void createTables(boolean dropIfExists, Class<?>... entityClasses) {
		for (Class<?> entityClass : entityClasses) {
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
					createQueryBuilder.append("_id" + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL");
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
		Class<?> entityClass = entity.getClass();
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

	public <T> T fetchOneBy(Class<T> entityClass, String whereClause) {

		List<T> entityList = this.fetchBy(entityClass, whereClause);
		T firstEntity = null;
		if (entityList.size() > 0) {
			firstEntity = entityList.get(0);
		}
		return firstEntity;
	}

	public <T> T fetchOneByPK(Class<T> entityClass, long pkValue) {
		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass);
		Field pkColumn = this._getPK(entityClass, fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		return this.fetchOneBy(entityClass, pkColumnName + " = " + pkValue);
	}

	public <T> List<T> fetchAll(Class<T> entityClass) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(table, new String[] { "*" }, null, null, null, null, null);

		return inflateFromCursor(entityClass, c, c.getCount());
	}

	public <T> List<T> fetchBy(Class<T> entityClass, String whereClause) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(table, new String[] { "*" }, whereClause, null, null, null, null);

		return inflateFromCursor(entityClass, c, c.getCount());
	}

	public <T> List<T> fetchBy(Class<T> entityClass, CovenantQuery covenantQuery) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();
		Cursor c = this._dbManager.getDb().query(covenantQuery.getDistinct(), table, null, covenantQuery.getWhere(), null,
				covenantQuery.getGroupBy(), covenantQuery.getHaving(), covenantQuery.getOrderBy(), covenantQuery.getLimit());

		return inflateFromCursor(entityClass, c, c.getCount());
	}

	public <T> int deleteByPk(Class<?> entityClass, long pkValue) {
		_verifyIsEntity(entityClass);
		String table = entityClass.getAnnotation(Entity.class).table();

		List<Pair<Field, Column>> fieldColumnPairs = this._getColumns(entityClass.getClass());
		Field pkColumn = this._getPK(entityClass, fieldColumnPairs);
		String pkColumnName = pkColumn.getAnnotation(Column.class).name().toString();

		return this._dbManager.getDb().delete(table, pkColumnName + " =?", new String[] { Long.toString(pkValue) });
	}

	public <T> int deleteWhere(Class<?> entityClass, String whereClause) {
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

	private Field _getPK(Class<?> entityClass, List<Pair<Field, Column>> fieldColumnPairs) {
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

	private boolean _verifyIsEntity(Class<?> entityClass) {
		return entityClass.isAnnotationPresent(Entity.class);
	}

	private static <T> ArrayList<T> inflateFromCursor(Class<T> entityClass, Cursor c, int limit) {
		ArrayList<T> returnList = new ArrayList<T>();
		int i = 0;
		try {
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						T current = entityClass.newInstance();
						Field[] fields = entityClass.getDeclaredFields();
						for (Field field : fields) {
							field.setAccessible(true);
							Column column = field.getAnnotation(Column.class);
							if (column != null && (c.getColumnIndex(column.name()) >= 0)) {
								if (field.getType() == String.class) {
									field.set(current, c.getString(c.getColumnIndex(column.name())));
								} else if (field.getType() == int.class) {
									field.setInt(current, c.getInt(c.getColumnIndex(column.name())));
								} else if (field.getType() == double.class) {
									field.setDouble(current, c.getDouble(c.getColumnIndex(column.name())));
								} else if (field.getType() == long.class) {
									field.setLong(current, c.getLong(c.getColumnIndex(column.name())));
								}
							}
						}
						returnList.add(current);
					} while (c.moveToNext() && ++i < limit);
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

	private List<Pair<Field, Column>> _getColumns(Class<?> entityClass) {
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
