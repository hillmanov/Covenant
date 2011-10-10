# Covenant - Simple ORM/DBAL for Android #

By simple, I mean really simple. 

### Here is how it works: ###
* Create your SQLite database schema first. Sorry, you need to do this by hand. No magical db creation for you (yet)
* Create your classes and annotate them. 3 supported types: long, int, String. You need at least 1 primary key column.
* Persist your objects with the EntityManager. 
* Retrieve your objects with the EntityManager.

### Why? What are you trying to solve with this? ###

I like working with objects when I can. If I am going to pull stuff out of a database, I would rather be working with obejcts than with cursors. The other options out there were either too heavy, or too much work to setup. Also, I wanted to learn about Java annotations and this was the perfect project for just that. 

### Enough talk. Show some code ###

Fine. But first, a story. Not long ago a company I was working for participated in the Global Corporate Challenge - an event where you keep track of how many steps you take a day by wearing a pedometer every day for several weeks/months. You are supposed to record them on their site so you can see how your team/company stacks up against others. I wanted to create an application that would make it easy to record this information on my Android phone. Here are the steps you would take to configure Covenant to do your persisting/retrieving of objects:

1) Create the db schema. Here is the schema I came up with:

    CREATE TABLE day_entry ( 
        id    INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        date  VARCHAR,
        steps INTEGER 
    );

Super simple. An primary key field, a date, and field for the amount of steps taken on the given day. Create a database, add this table, name it "data.db" __then put it in your "assets" folder in your application directory__. 

2) Next, create your class that will relate to this table in the db.  Here is an example:

    @Entity(table = "day_entry")
    public class DayEntry {
    	
    	@Column(PK = true, name = "id")
    	private long id;
    	
    	@Column(name = "date")
    	private String date;
    	
    	@Column(name = "steps")
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


### Notice a few things here: ###

* The class has the 'Entity' annotation, and specifies which table to map this class to. Without this, it won't work.
* The primary key property (in this case, it is simply 'id') is of type 'long'. This is to match the return type of the insert method of the SQLiteDatabase class that Covenant uses behind the scenes. Notice also that 'PK' is set to true, and the name of the column is also passed in. This must be explicity set. 
* All other properties you wish to be perisited must be annotated with the 'Column' annotation and specify which column they map to. 

Now, lets get to creating the EntityManager, which does all the hard work. Here is an example:

    EntityManager.init(new DbManager(getBaseContext(), "data.db"));
    EntityManager em = EntityManager.getInstance();

First, you initializ the EntityManager by passing in a DbManager instance. The DbManager takes two parameters: the application context, and the name of the database. The DbManager copies the database to where your application's databases need to be stored in order for you app to use them, unless the database already exists. 

Second, you get the instance of the EntityManager to use. It will be used to persist to and retrieve items from your database. 

Quick example:

    EntityManager.init(new DbManager(getBaseContext(), "data.db"));
    EntityManager em = EntityManager.getInstance();

    DayEntry de = new DayEntry();
    de.setDate("2011-05-21");
    de.setSteps(13432);
    em.save(de);
		
That's it. em.save returns true on successful insertion, or false if it failed.

## Want to retrieve some items? You have a few options: ##

### By PK Key ###
    DayEntry dayEntry = em.fetchOneByPK(new DayEntry(), 2);
    
### By a custom where clause ###
    List<DayEntry> dayEntries = em.fetchBy(new DayEntry(), "steps > 4500 AND steps < 100000");
    
### Need more control? Like, ORDER BY, LIMIT, etc.? Use the CovenantQuery object ###
    CovenantQuery customCQ = new CovenantQuery();
	unseenCQ.setWhere("steps = " + stepCount + " AND date > '12/01/2010' AND date <= '12/25/2010");
	unseenCQ.setOrderBy("RANDOM()");
	unseenCQ.setLimit(limit);
	List<DayEntry> entries = em.fetchBy(new DayEntry(), customCQ);

## Want to delete some items?\ ##

### Pass in the object you want to delete ###
    em.delete(dayEntry);

### By PK Key ###
    em.deleteByPK(new DayEntry(), 1);
    
### By a custom where clause ###
    em.deleteBy(new DayEntry(), "steps > 4500 AND steps < 100000");
    
### What does it NOT do? ###

Lots. It is meant to be simple wihthout lots of bells and whistles. However, the biggest missing piece is:

* Relationships. No joins, nothing. I am trying to keep the code and implementation simple, that would make it not simple. 

### Like the idea, but don't like my code? ###

Fine. I don't blame you. I created this project for fun - I am by no means claiming that I follow the best coding standards (I am using a singleton, after all...) but I wrote it to get something done. It has made my life easier when working with databases on Android, and figured it could help others out there too. If something should be fixed, let me know or even better, fix it yourself and send me a pull request


    
    



    


