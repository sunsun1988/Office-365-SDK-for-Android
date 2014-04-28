package com.microsoft.mailservice;

import microsoft.exchange.services.odata.model.Contact;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.microsoft.mailservice.tasks.CRUDContactTask;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class ContactActivity extends Activity{

	Contact mContact;
	String mAction;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String data = bundle.getString("data");

			if (data != null) {
				try {
					JSONObject payload = new JSONObject(data);
					Gson gson = new Gson();

					mAction = payload.getString("action");
					boolean editMode = payload.getBoolean("editmode");
					
					if(mAction != "create"){
						mContact = gson.fromJson(payload.getString("contact"), Contact.class);

						((EditText) findViewById(R.id.contact_display_name)).setText(mContact.getDisplayName());
						((EditText) findViewById(R.id.contact_given_name)).setText(mContact.getGivenName());
						((EditText) findViewById(R.id.contact_middle_name)).setText(mContact.getMiddleName());
						((EditText) findViewById(R.id.contact_surname)).setText(mContact.getSurname());
						((EditText) findViewById(R.id.contact_job_tittle)).setText(mContact.getJobTitle());
						((EditText) findViewById(R.id.contact_email_address)).setText(mContact.getEmailAddress1());
						((EditText) findViewById(R.id.contact_nick_name)).setText(mContact.getNickName());
						((EditText) findViewById(R.id.contact_company_name)).setText(mContact.getCompanyName());
						((EditText) findViewById(R.id.contact_department)).setText(mContact.getDepartment());			
					}

					setEditMode(editMode);
				}
				catch (Exception e) {
					Toast.makeText(this,"Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.contact, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_contact_accept:
			new CRUDContactTask(ContactActivity.this,Authentication.getCurrentCredentials(), 
					new String[]{mAction, "Creating Contact","Error on Creating contact", "Contact Created"})
			.execute(createContact());
			return true;
		case R.id.menu_contact_edit:
			setEditMode(true);
			return true;
		case R.id.menu_contact_delete:
			new CRUDContactTask(ContactActivity.this,Authentication.getCurrentCredentials(), 
					new String[]{"delete", "Deleting Contact","Error on Creating contact", "Contact Deleted"})
			.execute(createContact());
			return true;
		default:
			return false;
		}
	}

	private void setEditMode(boolean eneabled) {
		((EditText) findViewById(R.id.contact_display_name)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_given_name)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_middle_name)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_surname)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_job_tittle)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_email_address)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_nick_name)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_company_name)).setEnabled(eneabled);
		((EditText) findViewById(R.id.contact_department)).setEnabled(eneabled);		
	}

	public Contact createContact(){
		Contact contact = new Contact();
		contact.setId(mContact.getId());
		contact.setDisplayName(((EditText) findViewById(R.id.contact_display_name)).getText().toString());
		contact.setGivenName(((EditText) findViewById(R.id.contact_given_name)).getText().toString());
		contact.setMiddleName(((EditText) findViewById(R.id.contact_middle_name)).getText().toString());
		contact.setSurname(((EditText) findViewById(R.id.contact_surname)).getText().toString());
		contact.setJobTitle(((EditText) findViewById(R.id.contact_job_tittle)).getText().toString());
		contact.setEmailAddress1(((EditText) findViewById(R.id.contact_email_address)).getText().toString());
		contact.setNickName(((EditText) findViewById(R.id.contact_nick_name)).getText().toString());
		contact.setCompanyName(((EditText) findViewById(R.id.contact_company_name)).getText().toString());
		contact.setDepartment(((EditText) findViewById(R.id.contact_department)).getText().toString());

		return contact;
	}
}