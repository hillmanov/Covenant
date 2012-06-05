package com.covenant;

import android.app.Activity;
import android.os.Bundle;

import com.covenant.models.Person;

public class Covenant extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        EntityManager.init(new DbManager(getBaseContext()));
		EntityManager em = EntityManager.getInstance();
		
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
		
    }
}