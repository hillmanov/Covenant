package com.covenant;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;

public class Covenant extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        EntityManager.init(new DbManager(getBaseContext(), "data.db"));
		EntityManager em = EntityManager.getInstance();

		DayEntry de = new DayEntry();
		de.setDate("2011-05-21");
		de.setSteps(13432);
		em.save(de);
		
		DayEntry de2 = new DayEntry();
		de2.setDate("2011-05-27");
		de2.setSteps(3500);
		em.save(de2);
		
		em.startTransaction();
		long startTime = System.currentTimeMillis();
		DayEntry current = null;
		for (int i = 0; i < 100; i++) {
			current = new DayEntry();
			current.setDate("2011-06-24");
			current.setSteps(1);
			em.save(current);
		}
		long endTime = System.currentTimeMillis();
		em.endTransaction();
		System.out.println("Total time is :"+ (endTime-startTime));
		
		List<DayEntry> dayEntries = em.fetchBy(new DayEntry(), "steps > 4500 AND steps < 100000");
		DayEntry dayEntry = em.fetchOneByPK(new DayEntry(), 1);
		
		DayEntry dayEntry2 = em.fetchOneByPK(new DayEntry(), 2);
		
		System.out.println(dayEntries);
		System.out.println(dayEntry);
		System.out.println(dayEntry2);
		
		startTime = System.currentTimeMillis();
		List<DayEntry> dayEntries2 = em.fetchBy(new DayEntry(), "id > 0");
		endTime = System.currentTimeMillis();
		System.out.println("Total time is :"+ (endTime-startTime));
		
		System.out.println(dayEntries2);
    }
}