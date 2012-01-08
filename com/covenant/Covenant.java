package com.covenant;

import com.covenant.models.CovenantAggregate;
import com.covenant.models.Person;

import android.app.Activity;
import android.os.Bundle;

public class Covenant extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        EntityManager.init(new DbManager(getBaseContext()));
		EntityManager em = EntityManager.getInstance();
		em.createTables(true, Person.class);
		
		Person scott = new Person();
		scott.setFirstName("Scott");
		scott.setLastName("Hillman");
		em.save(scott);

		Person esther = new Person();
		esther.setFirstName("Esther");
		esther.setLastName("Hillman");
		em.save(esther);
		
		Person darcy = new Person();
		darcy.setFirstName("Darcy");
		darcy.setLastName("Hillman");
		em.save(darcy);

		Person adrie = new Person();
		adrie.setFirstName("Adrie");
		adrie.setLastName("Hillman");
		em.save(adrie);
		
		CovenantAggregate personIdSum = em.fetchAggregate(CovenantAggregate.AVG, Person.class, "person_id", null);
		System.out.println(personIdSum.getAvg());
		
		CovenantAggregate personCount = em.fetchAggregate(CovenantAggregate.COUNT, Person.class, "person_id", null);
		System.out.println(personCount.getCount());
		
		CovenantAggregate personIdMin = em.fetchAggregate(CovenantAggregate.MIN, Person.class, "person_id", null);
		System.out.println(personIdMin.getMin());

		CovenantAggregate personIdMax = em.fetchAggregate(CovenantAggregate.MAX, Person.class, "person_id", null);
		System.out.println(personIdMax.getMax());

		
    }
}