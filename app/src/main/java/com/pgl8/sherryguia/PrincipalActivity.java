package com.pgl8.sherryguia;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class PrincipalActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "PrincipalActivity";
    private static final int RC_SIGN_IN = 9001;
    private ProgressDialog mProgressDialog;
    private boolean mConnected;
	private TextView mTextView;
	private ImageView mImageView;
    private String accountName;
    private String accountPhoto;
    private Location mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

		mTextView = (TextView) findViewById(R.id.textView);
	    mImageView = (ImageView) findViewById(R.id.vinoImage);

        // Button listeners
        this.findViewById(R.id.sign_in_button).setOnClickListener(this);


        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

	    if(!isNetworkAvailable()){
		    Toast.makeText(this, "No hay conexión a internet, por favor actívala.", Toast.LENGTH_LONG).show();
	    }

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        GPSTracker gps = new GPSTracker(this);
        if(gps.canGetLocation()){
            mLocation = gps.loc;
        }
        if(accountName != null /*&& accountPhoto != null*/) {

            sendUserJSON(accountName, accountPhoto);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(view.getContext(), ExtendedTrackingActivity.class);
                intent.putExtra("accountName", accountName);
                intent.putExtra("accountPhoto", accountPhoto);
                startActivity(intent);

            }
        });
        fab.setVisibility(View.INVISIBLE);

        //SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);

    }

    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);

            GPSTracker gps = new GPSTracker(this);
            if(gps.canGetLocation()){
                mLocation = gps.loc;
            }
            if(accountName != null /*&& accountPhoto != null*/) {

                sendUserJSON(accountName, accountPhoto);
            }

        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
// [END onActivityResult]

	// Check if there is network available
	private boolean isNetworkAvailable(){
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}


	private void showProgressDialog() {
        Log.d(TAG, "Inside showProgressDialog()");
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        Log.d(TAG, "Inside hideProgressDialog()");
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }


    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            accountName = acct.getDisplayName();
            accountPhoto = (acct.getPhotoUrl() != null) ? acct.getPhotoUrl().toString() : "http://92.222.216.247/sherryadmin/assets/dist/img/launcher.png";

	        mTextView.setText(acct.getDisplayName());
	        Picasso.with(this).load(accountPhoto).transform(new CropCircleTransformation()).into(mImageView);
            mConnected = true;
            updateUI(true);

	        //sendUserJSON(accountName, accountPhoto);

        } else {
            mConnected = false;

            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void updateUI(boolean signedIn) {
        Log.d(TAG, "Inside updateUI()");
        if (signedIn) {
            Log.d(TAG, "Inside updateUI() signedIn");
            findViewById(R.id.sign_in_button).setVisibility(View.INVISIBLE);
	        mImageView.setVisibility(View.VISIBLE);
            findViewById(R.id.fab).setVisibility(View.VISIBLE);

        } else {
            Log.d(TAG, "Inside updateUI() not signedIn");
            mTextView.setText(R.string.inicia);
	        mImageView.setVisibility(View.INVISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.fab).setVisibility(View.INVISIBLE);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_principal, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_disconnect) {
            signOut();
            //revokeAccess();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.sign_in_button:
                Log.d(TAG, "Inside onClick() sign in button");
                signIn();
                break;
        }
    }

    // [START signIn]
    private void signIn() {
        Log.d(TAG, "Inside signIn()");
        mConnected = true;
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        Log.d(TAG, "Inside signOut()");
        if(mConnected) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            mConnected = false;
                            // [START_EXCLUDE]
                            updateUI(false);
                            // [END_EXCLUDE]
                        }
                    });
        }else{
            Toast.makeText(this, "Ya se encuentra desconectado", Toast.LENGTH_SHORT).show();
        }
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        Log.d(TAG, "Inside revokeAccess()");
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    protected void onPause(){
        Log.v(TAG, "onPause");
        super.onPause();
    }

    protected void onDestroy(){
        Log.v(TAG, "onDestroy");
        super.onDestroy();
        if(mProgressDialog != null && mProgressDialog.isShowing())  mProgressDialog.dismiss();
    }

    protected void onResume(){
        Log.v(TAG, "onResume");
        super.onResume();
    }

    public void sendUserJSON(String accountName, String accountPhoto) {
        JSONObject json = new JSONObject();
        if(accountPhoto == null){
            accountPhoto = "http://92.222.216.247/sherryadmin/assets/dist/img/launcher.png";
        }


        try {

            json.put("usuario", accountName);
            json.put("imagen", accountPhoto);
            if(mLocation == null){
                json.put("localizacion", "36.7025701,-6.1233665");
            }else{
                json.put("localizacion", mLocation.getLatitude()+","+mLocation.getLongitude());
            }
            Log.d(TAG, "sendUserJSON: "+json);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (json.length() > 0) {
	        // Llamada a la clase para mandar el post
            new SendJsonDataToServer().execute(String.valueOf(json));
        }
    }

    private class SendJsonDataToServer extends AsyncTask<String, Void, String> {

        private String urlPost = "http://92.222.216.247:8080/conexiondb/demo/vinoService/usuario";
	    private String jsonResponse;

        @Override
        protected String doInBackground(String... params) {
            try{
                URL url = new URL(urlPost);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setUseCaches(false);
                con.setDoOutput(true);
                con.setDoInput(true);

                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestMethod("POST");

	            Log.d(TAG, "doInBackground: "+params[0]);

                byte[] sendBytes = params[0].getBytes("UTF-8");
                con.setFixedLengthStreamingMode(sendBytes.length);

                OutputStream outputStream = con.getOutputStream();
                outputStream.write(sendBytes);

                int httpResponse = con.getResponseCode();

	            Log.d(TAG, "doInBackground: httpResponse: "+httpResponse);
	            if (httpResponse >= HttpURLConnection.HTTP_OK
                        && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                    Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");
                    jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    scanner.close();
                } else {
                    Scanner scanner = new Scanner(con.getErrorStream(), "UTF-8");
                    jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    scanner.close();
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
	        return jsonResponse;

        }
    }
}

