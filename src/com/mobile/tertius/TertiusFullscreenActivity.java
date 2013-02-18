package com.mobile.tertius;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.mobile.tertius.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class TertiusFullscreenActivity extends Activity {

	static final int							GET_PICTURE							= 1;

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean	AUTO_HIDE								= true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after user
	 * interaction before hiding the system UI.
	 */
	private static final int			AUTO_HIDE_DELAY_MILLIS	= 1500;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean	TOGGLE_ON_CLICK					= true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int			HIDER_FLAGS							= SystemUiHider.FLAG_HIDE_NAVIGATION;

	private static final String		FILE_PREFIX					= "great_pic";

	private static final String		FILE_SUFFIX							= ".jpg";

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider					mSystemUiHider;

	private View									getPicture, sendPicture;

	private String								fileName;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_tertius_fullscreen );

		final View controlsView = findViewById( R.id.fullscreen_content_controls );
		final View contentView = findViewById( R.id.imageView1 );

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance( this, contentView, HIDER_FLAGS );
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener( new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int	mControlsHeight;
					int	mShortAnimTime;

					@Override
					@TargetApi( Build.VERSION_CODES.HONEYCOMB_MR2 )
					public void onVisibilityChange( boolean visible ) {
						if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 ) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if ( mControlsHeight == 0 ) {
								mControlsHeight = controlsView.getHeight();
							}
							if ( mShortAnimTime == 0 ) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime );
							}
							controlsView.animate()
									.translationY( visible ? 0 : mControlsHeight )
									.setDuration( mShortAnimTime );
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility( visible ? View.VISIBLE : View.GONE );
						}

						if ( visible && AUTO_HIDE ) {
							// Schedule a hide().
							delayedHide( AUTO_HIDE_DELAY_MILLIS );
						}
					}
				} );

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View view ) {
				if ( TOGGLE_ON_CLICK ) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		} );

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById( R.id.take_picture ).setOnTouchListener(
				mDelayHideTouchListener );

		getPicture = findViewById( R.id.take_picture );
		sendPicture = findViewById( R.id.post_picture );

		if( savedInstanceState != null ){
			fileName = savedInstanceState.getString( FILE_PREFIX );
		}
		
		setClickListeners();
	}

	@Override
	protected void onPostCreate( Bundle savedInstanceState ) {
		super.onPostCreate( savedInstanceState );

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide( 100 );
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the system
	 * UI. This is to prevent the jarring behavior of controls going away while
	 * interacting with activity UI.
	 */
	View.OnTouchListener	mDelayHideTouchListener	= new View.OnTouchListener() {
																									@Override
																									public boolean onTouch(
																											View view,
																											MotionEvent motionEvent ) {
																										if ( AUTO_HIDE ) {
																											delayedHide( AUTO_HIDE_DELAY_MILLIS );
																										}
																										return false;
																									}
																								};

	Handler								mHideHandler						= new Handler();
	Runnable							mHideRunnable						= new Runnable() {
																									@Override
																									public void run() {
																										mSystemUiHider.hide();
																									}
																								};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide( int delayMillis ) {
		mHideHandler.removeCallbacks( mHideRunnable );
		mHideHandler.postDelayed( mHideRunnable, delayMillis );
	}

	@Override
	public void onSaveInstanceState( Bundle bundle ) {
		super.onSaveInstanceState( bundle );
		bundle.putString( FILE_PREFIX, fileName );
	}

	private void setClickListeners() {

		getPicture.setOnClickListener( new OnClickListener() {
			public void onClick( View v ) {
				getPicture();
			}
		} );

		sendPicture.setOnClickListener( new OnClickListener() {
			public void onClick( View v ) {
				postPicture();
			}
		} );
	}

	private void getPicture() {
		Intent picturer = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );

		PackageManager packageManager = getPackageManager();
		List< ResolveInfo > activities = packageManager.queryIntentActivities(
				picturer, 0 );
		boolean isIntentSafe = activities.size() > 0;

		if ( isIntentSafe ) {
			
			if ( canUseExternalStorage() ) {
				try {
					File f = createImageFile();
					
					picturer.putExtra( MediaStore.EXTRA_OUTPUT, Uri.fromFile( f ) );
					startActivityForResult( picturer, GET_PICTURE );
				}
				catch ( IOException ex ) {
					System.err.println( "Error creating temp image" );
				}
			} else {
				System.err.println( "No external storage access" );
			}

		} else {
			System.err.println( "No camera availible" );
		}

	}

	private boolean canUseExternalStorage() {
		String state = Environment.getExternalStorageState();
		if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
			return true;
		} else {
			return false;
		}

	}

	private File createImageFile() throws IOException {
		File path = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES );
		path.mkdir();

		// Create an image file name
		String timeStamp = new SimpleDateFormat( "yyyyMMdd_HHmmss" )
				.format( new Date() );
		String imageFileName = FILE_PREFIX + timeStamp + "_";

		File image = File.createTempFile( imageFileName, FILE_SUFFIX, path );
		
		fileName = image.getPath();
		
		return image;
	}

	private void postPicture() {

	}

	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		if ( requestCode == GET_PICTURE ) {
			if ( resultCode == RESULT_OK ) {
				Bitmap image = BitmapFactory.decodeFile( fileName );
				
				ImageView iv = (ImageView) findViewById( R.id.imageView1 );
				iv.setImageBitmap( image );
			}
		}
	}

}
