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
	private Map<String, List<Pair<Field, EntityField>>> _entityFieldCache;
	private Map<String, Field> _entityPKFieldCache; 
	
	private EntityManager() {
		_entityFieldCache = new HashMap<String, List<Pair<Field, EntityField>>>();
		_entityPKFieldCache = new HashMap<String, Field>();
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
		// Get the class of the entity and verify that it is a persistable Entity
		Class<? extends Object> entityClassType = entity.getClass();
		_verifyIsEntity(entityClassType);

		// Get entity fields
		String mappedTable = entityClassType.getAnnotation(Entity.class).tableName();
		ContentValues values;
		
		List<Pair<Field, EntityField>> entityFields = this._getEntityFields(entityClassType, mappedTable);
		Field pkField = this._getPK(entityFields, mappedTable);
		String pkFieldName = pkField.getAnnotation(EntityField.class).column().toString();
		
		values = new ContentValues();
		// Get all of the fields that are persistable EntityFields
		Field field;
		EntityField entityField;
		for (Pair<Field, EntityField> fieldPair : entityFields) {
			field = fieldPair.first;
			entityField = fieldPair.second;
			try {
				if (field.getType() == String.class) {
					values.put(entityField.column(), field.get(entity).toString());
				}
				else if (field.getType() == int.class) {
					values.put(entityField.column(), field.getInt(entity));
				}
				else if (field.getType() == long.class) {
					values.put(entityField.column(), field.getLong(entity));
				}
				
			}
			catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		// If the primary key is 0, than it is a new object
		if (values.getAsLong(pkFieldName) == 0) {
			values.remove(pkFieldName);
			long pkValue = _dbManager.getDb().insert(mappedTable, null, values);
			try {
				pkField.setLong(entity, pkValue);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		// If the primary key is not equal to zero, it is an existing object to be updated
		else {
			long pkValue = values.getAsLong(pkFieldName);
			values.remove(pkFieldName);
			_dbManager.getDb().update(mappedTable, values, pkFieldName + "=" + pkValue, null);
		}
		
		return false;
	}
	
	private Field _getPK(List<Pair<Field, EntityField>> entityFields, String mappedTable) {
		Field pkField = this._entityPKFieldCache.get(mappedTable);
		if (pkField != null) {
			return pkField;
		}
		else {
			for (Pair<Field, EntityField> fieldPair : entityFields) {
				fieldPair.first.setAccessible(true);
				EntityField entityField = fieldPair.first.getAnnotation(EntityField.class); // Checking to see if the field is persistable
				if (entityField != null) {
					if (entityField.PK()) {
						pkField = fieldPair.first;
						break;
					}
				}
			}

			this._entityPKFieldCache.put(mappedTable, pkField);
			return pkField;
		}
	}

	private List<Pair<Field, EntityField>> _getEntityFields(Class<? extends Object> entityClassType, String mappedTable)  {
		List<Pair<Field, EntityField>> entityFields = _entityFieldCache.get(mappedTable);

		if (entityFields != null) {
			return entityFields;
		}
		else {
			entityFields = new ArrayList<Pair<Field, EntityField>>();
			
			Field[] fields = entityClassType.getDeclaredFields();
			
			for (Field field : fields) {
				field.setAccessible(true);
				EntityField entityField = field.getAnnotation(EntityField.class); // Checking to see if the field is persistable
				if (entityField != null) {
					field.setAccessible(true);
					entityFields.add(new Pair<Field, EntityField>(field, entityField));
					
					
				}
			}
			_entityFieldCache.put(mappedTable, entityFields);
			return entityFields;
		}
	}
	
	public <T> T fetchOneBy(T entity, String whereClause) {
		List<T> entityList = this.fetchBy(entity, whereClause);
		T firstEntity = null;
		if (entityList.size() > 0) {
			firstEntity = entityList.get(0);
		}
		return firstEntity;
	}

	
	public <T> T fetchOneByPK(T entity, int pkValue) {
		String mappedTable = entity.getClass().getAnnotation(Entity.class).tableName();
		
		List<Pair<Field, EntityField>> entityFields = this._getEntityFields(entity.getClass(), mappedTable);
		Field pkField = this._getPK(entityFields, mappedTable);
		String pkFieldName = pkField.getAnnotation(EntityField.class).column().toString();
		
		return this.fetchOneBy(entity, pkFieldName + " = " + pkValue);
	}
	

	@SuppressWarnings("unchecked")
	public <T> List<T> fetchBy(T entityClass, String whereClause) {
		_verifyIsEntity(entityClass.getClass());
		String mappedTable = entityClass.getClass().getAnnotation(Entity.class).tableName();

		Cursor c = this._dbManager.getDb().query(mappedTable, new String[] {"*"}, whereClause, null, null, null, null);
		List<T> returnList = new ArrayList<T>();
		// Start Try
		try {
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						T current = (T) entityClass.getClass().newInstance();
						Field[] fields = entityClass.getClass().getDeclaredFields();
						for (Field field : fields) {
							field.setAccessible(true);
			
							EntityField entityField = field.getAnnotation(EntityField.class); // Checking to see if the field is persistable
							if (entityField != null) {
								if (field.getType() == String.class) {
									field.set(current, c.getString(c.getColumnIndex(entityField.column())));
								}
								else if (field.getType() == int.class) {
									field.setInt(current, c.getInt(c.getColumnIndex(entityField.column())));
								}
								else if (field.getType() == long.class) {
									field.setLong(current, c.getLong(c.getColumnIndex(entityField.column())));
								}
							}
						}
						returnList.add(current);
					} while (c.moveToNext());	
					c.close();
				}
			}
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		
		return returnList;
	}
	
	private void _verifyIsEntity(Class<? extends Object> entityClassType) {
		if (entityClassType.getAnnotation(Entity.class) == null) {
			System.out.println("No an entity!");
		}
	}

	public void startTransaction() {
		this._dbManager.getDb().beginTransaction();
	}
	
	public void endTransaction() {
		this._dbManager.getDb().setTransactionSuccessful();
		this._dbManager.getDb().endTransaction();
	}

	public void dumpDb() {
		Cursor cursor = _dbManager.getDb().rawQuery("SELECT * FROM day_entry;", null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					System.out.println("ID: " + cursor.getLong(cursor.getColumnIndex("id")) + " Date: " + cursor.getString(cursor.getColumnIndex("date")) + " Steps: " + cursor.getInt(cursor.getColumnIndex("steps")));
				} while (cursor.moveToNext());
			}
		}
		cursor.close();
		
	}
}
