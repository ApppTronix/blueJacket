package com.apptronix.bluealert;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class IceContacts extends AppCompatActivity {

    ListView listView;
    TextView emptyView;
    SharedPreferences sharedPreferences;
    public final String key= "ice_contacts_list";


    private String[] getIceContacts () {

        String contactsList = sharedPreferences.getString(key,null);

        if(contactsList==null) return null;

        return contactsList.split(";");
    }

    private void addContact(String contact){

        String contactsList = sharedPreferences.getString(key,null);
        boolean success;
        if(contactsList==null) success=sharedPreferences.edit().putString(key,contact).commit();
                else success=sharedPreferences.edit().putString(key,contactsList+";"+contact).commit();
        if(success){
            Toast.makeText(getApplicationContext(),"Contact added!",Toast.LENGTH_LONG).show();
            refreshList();
        } else {
            Toast.makeText(getApplicationContext(),"Couldn't add contact",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icecontacts);

        listView=(ListView)findViewById(R.id.ListView);
        emptyView=(TextView)findViewById(R.id.empty_view);

        sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);

        refreshList();

    }

    public void refreshList(){
        String[] iceContacts = getIceContacts();
        if(iceContacts!=null) {
            ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,iceContacts);
            listView.setAdapter(listAdapter);
            emptyView.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);
        }
    }


    public void clearContacts(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Remove Contacts")
                .setMessage("Clear all ICE contacts?")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putString(key,null).commit();
                        refreshList();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.action_clear){
            clearContacts();
            return true;
        }

        return false;
    }

    public void onFabClicked (View v){

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Add ICE Contact")
                .setView(input)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        if(m_Text.length()!=10){
                            Toast.makeText(getApplicationContext(),"Please enter a valid phone number",Toast.LENGTH_LONG).show();
                            return;
                        }
                        addContact(m_Text);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();

    }


}
