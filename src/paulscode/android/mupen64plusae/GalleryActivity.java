/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ChangeLog;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog.ScanRomsDialogListener;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.input.DiagnosticActivity;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask.CacheRomInfoListener;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.task.ComputeMd5Task.ComputeMd5Listener;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.MenuListView;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v4.view.GravityCompat;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnClickListener;

import android.support.v7.app.ActionBar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.content.res.Configuration;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
import android.preference.Preference;

public class GalleryActivity extends ActionBarActivity implements ComputeMd5Listener, CacheRomInfoListener
{
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    // Widgets
    private RecyclerView mGridView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MenuListView mDrawerList;
    private ImageButton mActionButton;
    private MenuItem mSearchItem;
    private MenuItem mRefreshItem;
    private ScrollView mDrawerInfoScroll;
    private LinearLayout mDrawerOptionsLayout;
    private LinearLayout mDrawerInfoLayout;
    private ImageView mDrawerInfoArt;
    private LinearLayout mDrawerImageLayout;
    private TextView mDrawerGameTitle;
    private TextView mDrawerInfoTitle;
    
    // Searching
    private SearchView mSearchView;
    private String mSearchQuery = "";
    
    // Resizable gallery thumbnails
    public int galleryWidth;
    public int galleryMaxWidth;
    public int galleryHalfSpacing;
    public int galleryColumns = 2;
    public float galleryAspectRatio;
    
    // Background tasks
    private CacheRomInfoTask mCacheRomInfoTask = null;
    
    @Override
    protected void onNewIntent( Intent intent )
    {
        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, PlayMenuActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance. Currently, the only info that may be passed via the intent
        // is the selected game path, so we only need to refresh that aspect of the UI.  This will
        // happen anyhow in onResume(), so we don't really need to do much here.
        super.onNewIntent( intent );
        
        // Only remember the last intent used
        setIntent( intent );
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.setTheme( android.support.v7.appcompat.R.style.Theme_AppCompat_NoActionBar );
        super.onCreate( savedInstanceState );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        int lastVer = mAppData.getLastAppVersionCode();
        int currVer = mAppData.appVersionCode;
        if( lastVer != currVer )
        {
            // First run after install/update, greet user with changelog, then help dialog
            popupFaq();
            ChangeLog log = new ChangeLog( getAssets() );
            if( log.show( this, lastVer + 1, currVer ) )
            {
                mAppData.putLastAppVersionCode( currVer );
            }
        }
        
        // Get the ROM path if it was passed from another activity/app
        Bundle extras = getIntent().getExtras();
        if( extras != null )
        {
            String givenRomPath = extras.getString( Keys.Extras.ROM_PATH );
            if( !TextUtils.isEmpty( givenRomPath ) )
                launchPlayMenuActivity( givenRomPath );
        }
        
        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = (RecyclerView) findViewById( R.id.gridview );
        
        // Configure the Floating Action Button to add files or folders to the library
        mActionButton = (ImageButton) findViewById( R.id.add );
        mActionButton.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                promptSearchPath( null );
            }
        });
        
        // Show and hide the Floating Action Button while scrolling
        // (Android does not have built-in support for FABs, believe it or not)
        mGridView.setOnScrollListener( new OnScrollListener()
        {
            public void onScrolled( RecyclerView recyclerView, int dx, int dy )
            {
                if ( dy < 0 )
                    showActionButton();
                else if (dy > 0)
                    hideActionButton();
            }
            
            public void onScrollStateChanged( RecyclerView recyclerView, int newState )
            {
            }
        });
        
        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        toolbar.setTitle( R.string.app_name );
        setSupportActionBar( toolbar );
        
        // Configure the navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawerLayout );
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout, toolbar, 0, 0 )
        {
            @Override
            public void onDrawerClosed( View drawerView )
            {
                // Hide the game information sidebar
                mDrawerList.setVisibility( View.VISIBLE );
                mDrawerInfoScroll.setVisibility( View.GONE );
                
                invalidateOptionsMenu();
                showActionButton();
                super.onDrawerClosed( drawerView );
            }

            @Override
            public void onDrawerOpened( View drawerView )
            {
                invalidateOptionsMenu();
                hideActionButton();
                super.onDrawerOpened( drawerView );
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle );
        
        // Configure the list in the navigation drawer
        final Activity activity = this;
        mDrawerList = (MenuListView) findViewById( R.id.drawerNavigation );
        mDrawerList.setMenuResource( R.menu.gallery_drawer );
        
        // Select the Library section
        mDrawerList.getMenu().getItem( 0 ).setChecked( true );
        
        // Handle menu item selections
        mDrawerList.setOnClickListener( new MenuListView.OnClickListener()
        {
            @Override
            public void onClick( MenuItem menuItem )
            {
                switch( menuItem.getItemId() )
                {
                    case R.id.menuItem_library:
                        mDrawerLayout.closeDrawer( GravityCompat.START );
                        break;
                    case R.id.menuItem_globalSettings:
                        startActivity( new Intent( activity, SettingsGlobalActivity.class ) );
                        break;
                    case R.id.menuItem_emulationProfiles:
                        startActivity( new Intent( activity, ManageEmulationProfilesActivity.class ) );
                        break;
                    case R.id.menuItem_touchscreenProfiles:
                        startActivity( new Intent( activity, ManageTouchscreenProfilesActivity.class ) );
                        break;
                    case R.id.menuItem_controllerProfiles:
                        startActivity( new Intent( activity, ManageControllerProfilesActivity.class ) );
                        break;
                    case R.id.menuItem_faq:
                        popupFaq();
                        break;
                    case R.id.menuItem_helpForum:
                        Utility.launchUri( activity, R.string.uri_forum );
                        break;
                    case R.id.menuItem_controllerDiagnostics:
                        startActivity( new Intent( activity, DiagnosticActivity.class ) );
                        break;
                    case R.id.menuItem_reportBug:
                        Utility.launchUri( activity, R.string.uri_bugReport );
                        break;
                    case R.id.menuItem_appVersion:
                        popupAppVersion();
                        break;
                    case R.id.menuItem_changelog:
                        new ChangeLog( getAssets() ).show( activity, 0, mAppData.appVersionCode );
                        break;
                    case R.id.menuItem_logcat:
                        popupLogcat();
                        break;
                    case R.id.menuItem_hardwareInfo:
                        popupHardwareInfo();
                        break;
                    case R.id.menuItem_credits:
                        Utility.launchUri( activity, R.string.uri_credits );
                        break;
                    case R.id.menuItem_localeOverride:
                        mUserPrefs.changeLocale( activity );
                        break;
                }
            }
        });
        
        // Configure the game information drawer
        mDrawerInfoScroll = (ScrollView) findViewById( R.id.drawerInfoScroll );
        mDrawerOptionsLayout = (LinearLayout) findViewById( R.id.drawerOptionsLayout );
        mDrawerInfoLayout = (LinearLayout) findViewById( R.id.drawerInfoLayout );
        mDrawerInfoArt = (ImageView) findViewById( R.id.imageArt );
        mDrawerImageLayout = (LinearLayout) findViewById( R.id.imageLayout );
        mDrawerGameTitle = (TextView) findViewById( R.id.gameTitle );
        mDrawerInfoTitle = (TextView) findViewById( R.id.drawerInfoTitle );
        
        // Have the game cover art scroll at half the speed as the rest of the content
        mDrawerInfoScroll.getViewTreeObserver().addOnScrollChangedListener(new OnScrollChangedListener()
        {
            @Override
            public void onScrollChanged()
            {
                int scrollY = mDrawerInfoScroll.getScrollY();
                mDrawerImageLayout.setPadding( 0, scrollY/2, 0, 0 );
            }
        });
        
        // Load some values used to define the grid view layout
        galleryMaxWidth = (int) getResources().getDimension( R.dimen.galleryImageWidth );
        galleryHalfSpacing = (int) getResources().getDimension( R.dimen.galleryHalfSpacing );
        galleryAspectRatio = galleryMaxWidth * 1.0f/getResources().getDimension( R.dimen.galleryImageHeight );
        
        // Update the grid size and spacing whenever the layout changes
        mGridView.addOnLayoutChangeListener( new OnLayoutChangeListener()
        {
            public void onLayoutChange( View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom )
            {
                // Update the grid layout
                int width = right - left - galleryHalfSpacing * 2;
                galleryColumns = (int) Math.ceil(width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
                galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;
                
                GridLayoutManager layoutManager = (GridLayoutManager) mGridView.getLayoutManager();
                layoutManager.setSpanCount( galleryColumns );
                mGridView.getAdapter().notifyDataSetChanged();
            }
        });
        
        // Populate the gallery with the games
        refreshGrid( new ConfigFile( mUserPrefs.romInfoCache_cfg ) );
        
        // Pop up a warning if the installation appears to be corrupt
        if( !mAppData.isValidInstallation )
        {
            CharSequence title = getText( R.string.invalidInstall_title );
            CharSequence message = getText( R.string.invalidInstall_message );
            new Builder( this ).setTitle( title ).setMessage( message ).create().show();
        }
    }
    
    public void hideActionButton()
    {
        if ( mActionButton.getVisibility() != View.VISIBLE || mActionButton.getAnimation() != null )
            return;
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;
        
        ScaleAnimation anim = new ScaleAnimation( 1.0f, 0.0f, 1.0f, 0.0f, 56/2 * logicalDensity, 56/2 * logicalDensity );
        anim.setDuration(150);
        anim.setAnimationListener( new AnimationListener()
        {
            public void onAnimationStart( Animation anim )
            {
            }
            
            public void onAnimationRepeat( Animation anim )
            {
            }
            
            public void onAnimationEnd( Animation anim )
            {
                mActionButton.setVisibility( View.GONE );
            }
        });
        mActionButton.startAnimation( anim );
    }
    
    public void showActionButton()
    {
        if ( mActionButton.getVisibility() == View.VISIBLE || mActionButton.getAnimation() != null )
            return;
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;
        
        ScaleAnimation anim = new ScaleAnimation( 0.0f, 1.0f, 0.0f, 1.0f, 56/2 * logicalDensity, 56/2 * logicalDensity );
        anim.setDuration(150);
        anim.setAnimationListener( new AnimationListener()
        {
            public void onAnimationStart( Animation anim )
            {
                mActionButton.setVisibility( View.VISIBLE );
            }
            
            public void onAnimationRepeat( Animation anim )
            {
            }
            
            public void onAnimationEnd( Animation anim )
            {
            }
        });
        mActionButton.startAnimation( anim );
    }
    
    protected void onStop()
    {
        super.onStop();
        
        // Cancel long-running background tasks
        if( mCacheRomInfoTask != null )
        {
            mCacheRomInfoTask.cancel( false );
            mCacheRomInfoTask = null;
        }
    }
    
    @Override
    protected void onPostCreate( Bundle savedInstanceState )
    {
        super.onPostCreate( savedInstanceState );
        mDrawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
        mDrawerToggle.onConfigurationChanged( newConfig );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        // Remove the toolbar items when the navigation drawer is open
        if ( mDrawerLayout != null && mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            return super.onCreateOptionsMenu( menu );
        
        getMenuInflater().inflate( R.menu.gallery_activity, menu );
        
        mRefreshItem = menu.findItem( R.id.menuItem_refreshRoms );
        mSearchItem = menu.findItem( R.id.menuItem_search );
        MenuItemCompat.setOnActionExpandListener( mSearchItem, new OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse( MenuItem item )
            {
                mSearchQuery = "";
                refreshGrid( new ConfigFile( mUserPrefs.romInfoCache_cfg ) );
                return true;
            }
            
            @Override
            public boolean onMenuItemActionExpand( MenuItem item )
            {
                
                return true;
            }
        });
        
        mSearchView = (SearchView) MenuItemCompat.getActionView( mSearchItem );
        mSearchView.setOnQueryTextListener( new OnQueryTextListener()
        {
            public boolean onQueryTextSubmit( String query )
            {
                
                return false;
            }
            
            public boolean onQueryTextChange( String query )
            {
                mSearchQuery = query;
                refreshGrid( new ConfigFile( mUserPrefs.romInfoCache_cfg ) );
                return false;
            }
        });
        
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem menuItem )
    {
        switch( menuItem.getItemId() )
        {
            case R.id.menuItem_refreshRoms:
                refreshRoms( null );
                return true;
            default:
                return super.onOptionsItemSelected( menuItem );
        }
    }
    
    public void onGalleryItemClick( GalleryItem item, View parentView )
    {
        if( item == null )
            Log.e( "GalleryActivity", "No item selected" );
        else if( item.romFile == null )
            Log.e( "GalleryActivity", "No ROM file available" );
        else
        {
            PlayMenuActivity.action = PlayMenuActivity.ACTION_RESUME;
            launchPlayMenuActivity( item.romFile.getAbsolutePath(), item.md5 );
        }
    }
    
    public void addGameOption( LinearLayout layout, GalleryItem useItem, int icon, String title, String summary, String useKey )
    {
        // Unfortunately there is no way to include a layout AND set the values for it from XML, so do it programmatically
        // http://stackoverflow.com/questions/9013298/setting-attribute-of-child-element-of-included-layout
        LayoutInflater inflater = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view = inflater.inflate( R.layout.list_item_two_text_icon, null );
        
        ImageView iconView = (ImageView) view.findViewById( R.id.icon );
        TextView text1 = (TextView) view.findViewById( R.id.text1 );
        TextView text2 = (TextView) view.findViewById( R.id.text2 );
        iconView.setImageResource( icon );
        text1.setText( title );
        text2.setText( summary );
        
        layout.addView( view );
        
        if ( useKey == null ) return;
        
        // Pass the key and item to the listener
        final String key = useKey;
        final GalleryItem item = useItem;
        final Context context = (Context) this;
        
        view.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                if( key.equals( PlayMenuActivity.ACTION_RESUME ) )
                {
                    PlayMenuActivity.action = PlayMenuActivity.ACTION_RESUME;
                    launchPlayMenuActivity( item.romFile.getAbsolutePath(), item.md5 );
                }
                else if( key.equals( PlayMenuActivity.ACTION_RESTART ) )
                {
                    CharSequence title = getText( R.string.confirm_title );
                    CharSequence message = getText( R.string.confirmResetGame_message );
                    Prompt.promptConfirm( context, title, message, new PromptConfirmListener()
                    {
                        @Override
                        public void onConfirm()
                        {
                            PlayMenuActivity.action = PlayMenuActivity.ACTION_RESTART;
                            launchPlayMenuActivity( item.romFile.getAbsolutePath(), item.md5 );
                        }
                    });
                }
                else if( key.equals( PlayMenuActivity.ACTION_SETTINGS ) )
                {
                    PlayMenuActivity.action = null;
                    launchPlayMenuActivity( item.romFile.getAbsolutePath(), item.md5 );
                }
            }
        });
    }
    
    public void onGalleryItemLongClick( GalleryItem item, View parentView )
    {
        // Show the game info sidebar
        mDrawerList.setVisibility( View.GONE );
        mDrawerInfoScroll.setVisibility( View.VISIBLE );
        mDrawerInfoScroll.scrollTo( 0, 0 );
        
        // Set the cover art in the sidebar
        item.loadBitmap();
        if( item.artBitmap != null )
            mDrawerInfoArt.setImageDrawable( item.artBitmap );
        else
           mDrawerInfoArt.setImageResource( R.drawable.default_coverart );
        
        // Set the game title
        mDrawerGameTitle.setText( item.baseName );
        
        // Set the game options
        mDrawerOptionsLayout.removeAllViews();
        
        addGameOption( mDrawerOptionsLayout, item, R.drawable.ic_play,
            getString( R.string.actionResume_title ),
            getString( R.string.actionResume_summary ),
            PlayMenuActivity.ACTION_RESUME );
        
        addGameOption( mDrawerOptionsLayout, item, R.drawable.ic_undo,
            getString( R.string.actionRestart_title ),
            getString( R.string.actionRestart_summary ),
            PlayMenuActivity.ACTION_RESTART );
        
        addGameOption( mDrawerOptionsLayout, item, R.drawable.ic_settings,
            getString( R.string.screenGameSettings_title ),
            getString( R.string.screenGameSettings_summary ),
            PlayMenuActivity.ACTION_SETTINGS );
        
        
        // Add a section explaining the region and dump information for the ROM
        // http://forums.emulator-zone.com/archive/index.php/t-5533.html
        mDrawerInfoLayout.removeAllViews();
        
        // There's probably some clever regex to do this, but use basic string functions to parse out the dump info
        List<Preference> prefs = new ArrayList<Preference>();
        int index = 0, length = item.goodName.length();
        
        while ( index < length )
        {
            int startIndex = length, endIndex = length;
            int paren = item.goodName.indexOf( "(", index );
            int bracket = item.goodName.indexOf( "[", index );
            if ( paren > -1 && paren < startIndex ) startIndex = paren + 1;
            if ( bracket > -1 && bracket < startIndex ) startIndex = bracket + 1;
            if ( startIndex >= length ) break;
            
            paren = item.goodName.indexOf( ")", startIndex );
            bracket = item.goodName.indexOf( "]", startIndex );
            if ( paren > -1 && paren < endIndex ) endIndex = paren;
            if ( bracket > -1 && bracket < endIndex ) endIndex = bracket;
            if ( endIndex >= length ) break;
            
            // parse out the tokens between startIndex and endIndex
            String code = item.goodName.substring( startIndex, endIndex );
            
            Preference info = new Preference( this );
            
            if ( code.length() <= 2 )
            {
                if ( code.startsWith( "a" ) )
                {
                    // a# = alternate
                    info.setTitle( getString( R.string.infoAlternate_title ) );
                    info.setSummary( getString( R.string.infoAlternate_summary ) );
                }
                else if ( code.startsWith( "b" ) )
                {
                    // b# = bad dump
                    info.setTitle( getString( R.string.infoBad_title ) );
                    info.setSummary( getString( R.string.infoBad_summary ) );
                }
                else if ( code.startsWith( "t" ) )
                {
                    // t# = trained
                    info.setTitle( getString( R.string.infoTrained_title ) );
                    info.setSummary( getString( R.string.infoTrained_summary ) );
                }
                else if ( code.startsWith( "f" ) )
                {
                    // f# = fix
                    info.setTitle( getString( R.string.infoFixed_title ) );
                    info.setSummary( getString( R.string.infoFixed_summary ) );
                }
                else if ( code.startsWith( "h" ) )
                {
                    // h# = hack
                    info.setTitle( getString( R.string.infoHack_title ) );
                    info.setSummary( getString( R.string.infoHack_summary ) );
                }
                else if ( code.startsWith( "o" ) )
                {
                    // o# = overdump
                    info.setTitle( getString( R.string.infoOverdump_title ) );
                    info.setSummary( getString( R.string.infoOverdump_summary ) );
                }
                else if ( code.equals( "!" ) )
                {
                    // ! = good dump
                    info.setTitle( getString( R.string.infoVerified_title ) );
                    info.setSummary( getString( R.string.infoVerified_summary ) );
                }
                else if ( code.equals( "A" ) )
                {
                    // A = Australia
                    info.setTitle( getString( R.string.infoAustralia_title ) );
                    info.setSummary( getString( R.string.infoAustralia_summary ) );
                }
                else if ( code.equals( "U" ) )
                {
                    // U = USA
                    info.setTitle( getString( R.string.infoUSA_title ) );
                    info.setSummary( getString( R.string.infoUSA_summary ) );
                }
                else if ( code.equals( "J" ) )
                {
                    // J = Japan
                    info.setTitle( getString( R.string.infoJapan_title ) );
                    info.setSummary( getString( R.string.infoJapan_summary ) );
                }
                else if ( code.equals( "JU" ) )
                {
                    // JU = Japan and USA
                    info.setTitle( getString( R.string.infoJapanUSA_title ) );
                    info.setSummary( getString( R.string.infoJapanUSA_summary ) );
                }
                else if ( code.equals( "E" ) )
                {
                    // E = Europe
                    info.setTitle( getString( R.string.infoEurope_title ) );
                    info.setSummary( getString( R.string.infoEurope_summary ) );
                }
                else if ( code.equals( "G" ) )
                {
                    // G = Germany
                    info.setTitle( getString( R.string.infoGermany_title ) );
                    info.setSummary( getString( R.string.infoGermany_summary ) );
                }
                else if ( code.equals( "F" ) )
                {
                    // F = France
                    info.setTitle( getString( R.string.infoFrance_title ) );
                    info.setSummary( getString( R.string.infoFrance_summary ) );
                }
                else if ( code.equals( "S" ) )
                {
                    // S = Spain
                    info.setTitle( getString( R.string.infoSpain_title ) );
                    info.setSummary( getString( R.string.infoSpain_summary ) );
                }
                else if ( code.equals( "I" ) )
                {
                    // I = Italy
                    info.setTitle( getString( R.string.infoItaly_title ) );
                    info.setSummary( getString( R.string.infoItaly_summary ) );
                }
                else if ( code.equals( "PD" ) )
                {
                    // PD = public domain
                    info.setTitle( getString( R.string.infoPublicDomain_title ) );
                    info.setSummary( getString( R.string.infoPublicDomain_summary ) );
                }
                else if ( code.startsWith( "M" ) )
                {
                    // M# = multi-language
                    info.setTitle( getString( R.string.infoLanguage_title, code.substring( 1 ) ) );
                    info.setSummary( getString( R.string.infoLanguage_summary ) );
                }
                else
                {
                    // ignore this code
                    info = null;
                }
            }
            else if ( code.startsWith( "T+" ) )
            {
                // T+* = translated
                info.setTitle( getString( R.string.infoTranslated_title ) );
                info.setSummary( getString( R.string.infoTranslated_summary ) );
            }
            else if ( code.startsWith( "T-" ) )
            {
                // T-* = translated
                info.setTitle( getString( R.string.infoTranslated_title ) );
                info.setSummary( getString( R.string.infoTranslated_summary ) );
            }
            else if ( code.startsWith( "V" ) && code.length() <= 6 )
            {
                // V = version code
                info.setTitle( getString( R.string.infoVersion_title, code.substring(1) ) );
                info.setSummary( getString( R.string.infoVersion_summary ) );
            }
            else if ( code.startsWith( "PAL" ) )
            {
                // PAL = PAL version
                info.setTitle( getString( R.string.infoPAL_title ) );
                info.setSummary( getString( R.string.infoPAL_summary ) );
            }
            else if ( code.startsWith( "PAL-NTSC" ) )
            {
                // PAL-NTSC = PAL and NTSC compatible
                info.setTitle( getString( R.string.infoPALNTSC_title ) );
                info.setSummary( getString( R.string.infoPALNTSC_summary ) );
            }
            else if ( code.startsWith( "NTSC" ) )
            {
                // NTSC = NTSC version
                info.setTitle( getString( R.string.infoNTSC_title ) );
                info.setSummary( getString( R.string.infoNTSC_summary ) );
            }
            else
            {
                // Everything else is listed raw and treated as a hack
                info.setTitle( code );
                info.setSummary( getString( R.string.infoHack_summary ) );
            }
            
            if ( info != null )
                prefs.add( info );
            
            index = endIndex + 1;
        }
        
        if ( prefs.size() > 0 )
        {
            mDrawerInfoTitle.setVisibility( View.VISIBLE );
            
            for ( Preference pref : prefs )
                addGameOption( mDrawerInfoLayout, item, 0, pref.getTitle().toString(), pref.getSummary().toString(), null );
        }
        else
        {
            mDrawerInfoTitle.setVisibility( View.GONE );
        }
        
        // Open the navigation drawer
        mDrawerLayout.openDrawer( GravityCompat.START );
    }
    
    private void launchPlayMenuActivity( final String romPath )
    {
        // Asynchronously compute MD5 and launch play menu when finished
        Notifier.showToast( this, String.format( getString( R.string.toast_loadingGameInfo ) ) );
        new ComputeMd5Task( new File( romPath ), this ).execute();
    }
    
    // Show the navigation drawer when the user presses the Menu button
    // http://stackoverflow.com/questions/22220275/show-navigation-drawer-on-physical-menu-button
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if ( keyCode == KeyEvent.KEYCODE_MENU )
        {
            if ( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                mDrawerLayout.closeDrawer( GravityCompat.START );
            else
                mDrawerLayout.openDrawer( GravityCompat.START );
            return true;
        }
        return super.onKeyDown( keyCode, event );
    }
    
    @Override
    public void onBackPressed()
    {
        if ( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            mDrawerLayout.closeDrawer( GravityCompat.START );
        else
            super.onBackPressed();
    }
    
    @Override
    public void onComputeMd5Finished( File file, String md5 )
    {
        launchPlayMenuActivity( file.getAbsolutePath(), md5 );
    }
    
    private void launchPlayMenuActivity( String romPath, String md5 )
    {
        if( !TextUtils.isEmpty( romPath ) && !TextUtils.isEmpty( md5 ) )
        {
            Intent intent = new Intent( GalleryActivity.this, PlayMenuActivity.class );
            intent.putExtra( Keys.Extras.ROM_PATH, romPath );
            intent.putExtra( Keys.Extras.ROM_MD5, md5 );
            startActivity( intent );
        }
    }
    
    private void promptSearchPath( File startDir )
    {
        // Prompt for search path, then asynchronously search for ROMs
        if( startDir == null || !startDir.exists() )
            startDir = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        
        ScanRomsDialog dialog = new ScanRomsDialog( this, startDir, false, new ScanRomsDialogListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    // Add this directory to the list of ROMs folders to search
                    mUserPrefs.addRomsFolder( file.getAbsolutePath() );
                    
                    // Search this folder for ROMs
                    refreshRoms( file );
                }
                else if( file != null )
                {
                    if( file.isDirectory() )
                    {
                        promptSearchPath( file );
                    }
                    else
                    {
                        // The user selected an individual file
                        refreshRoms( file );
                    }
                }
            }
        });
        
        dialog.show();
    }
    
    private void refreshRoms( final File startDir )
    {
        // Asynchronously search for ROMs
        mCacheRomInfoTask = new CacheRomInfoTask( this, startDir,
                mAppData.mupen64plus_ini, mUserPrefs, this );
        mCacheRomInfoTask.execute();
    }
    
    @Override
    public void onCacheRomInfoProgress( ConfigSection section )
    {
    }
    
    @Override
    public void onCacheRomInfoFinished( ConfigFile config, boolean canceled )
    {
        mCacheRomInfoTask = null;
        refreshGrid( config );
    }
    
    private void refreshGrid( ConfigFile config )
    {
        String query = mSearchQuery.toLowerCase();
        String[] searches = null;
        if ( query.length() > 0 )
            searches = query.split(" ");
        
        List<GalleryItem> items = new ArrayList<GalleryItem>();
        List<GalleryItem> recentItems = null;
        int currentTime = 0;
        final boolean showRecentlyPlayed = mUserPrefs.getShowRecentlyPlayed();
        if ( showRecentlyPlayed )
        {
            recentItems = new ArrayList<GalleryItem>();
            currentTime = (int) ( new Date().getTime()/1000 );
        }
        
        final boolean showFullNames = mUserPrefs.getShowFullNames();
        
        for( String md5 : config.keySet() )
        {
            if( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) )
            {
                String goodName = config.get( md5, "goodName" );
                String baseName = null;
                if ( showFullNames )
                {
                    baseName = goodName;
                }
                else
                {
                    // Strip the region and dump information
                    baseName = goodName.split( " \\(" )[0].trim();
                }
                
                boolean matchesSearch = true;
                if ( searches != null && searches.length > 0 )
                {
                    // Make sure the ROM name contains every token in the query
                    String lowerName = baseName.toLowerCase();
                    for( String search : searches )
                    {
                        if ( search.length() > 0 && !lowerName.contains( search ) )
                        {
                            matchesSearch = false;
                            break;
                        }
                    }
                }
                
                if ( matchesSearch )
                {
                    String romPath = config.get( md5, "romPath" );
                    String artPath = config.get( md5, "artPath" );
                    String lastPlayedStr = config.get( md5, "lastPlayed" );
                    int lastPlayed = 0;
                    if ( lastPlayedStr != null )
                        lastPlayed = Integer.parseInt( lastPlayedStr );
                    
                    GalleryItem item = new GalleryItem( this, md5, goodName, baseName, romPath, artPath, lastPlayed );
                    items.add( item );
                    if ( showRecentlyPlayed && currentTime - item.lastPlayed <= 60 * 60 * 24 * 7 ) // 7 days
                        recentItems.add( item );
                }
            }
        }
        Collections.sort( items, new GalleryItem.NameComparator() );
        if ( recentItems != null )
            Collections.sort( recentItems, new GalleryItem.RecentlyPlayedComparator() );
        
        List<GalleryItem> combinedItems = items;
        if ( showRecentlyPlayed && recentItems.size() > 0 )
        {
            combinedItems = new ArrayList<GalleryItem>();
            
            combinedItems.add( new GalleryItem( this, getString( R.string.galleryRecentlyPlayed ) ) );
            combinedItems.addAll( recentItems );
            
            combinedItems.add( new GalleryItem( this, getString( R.string.galleryLibrary ) ) );
            combinedItems.addAll( items );
            
            items = combinedItems;
        }
        
        mGridView.setAdapter( new GalleryItem.Adapter( this, items ) );
        
        // Allow the headings to take up the entire width of the layout
        final List<GalleryItem> finalItems = items;
        GridLayoutManager layoutManager = new GridLayoutManager( this, galleryColumns );
        layoutManager.setSpanSizeLookup( new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize( int position )
            {
                if ( finalItems.get( position ).isHeading )
                    return galleryColumns;
                return 1;
            }
        });
        
        mGridView.setLayoutManager( layoutManager );
    }
    
    private void popupFaq()
    {
        CharSequence title = getText( R.string.menuItem_faq );
        CharSequence message = getText( R.string.popup_faq );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupLogcat()
    {
        String title = getString( R.string.menuItem_logcat );
        String message = DeviceUtil.getLogCat();
        popupShareableText( title, message );
    }
    
    private void popupHardwareInfo()
    {
        String title = getString( R.string.menuItem_hardwareInfo );
        String axisInfo = DeviceUtil.getAxisInfo();
        String peripheralInfo = DeviceUtil.getPeripheralInfo();
        String cpuInfo = DeviceUtil.getCpuInfo();
        String message = axisInfo + peripheralInfo + cpuInfo;
        popupShareableText( title, message );
    }
    
    private void popupShareableText( String title, final String message )
    {
        // Set up click handler to share text with a user-selected app (email, clipboard, etc.)
        DialogInterface.OnClickListener shareHandler = new DialogInterface.OnClickListener()
        {
            @SuppressLint( "InlinedApi" )
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                // See http://android-developers.blogspot.com/2012/02/share-with-intents.html
                Intent intent = new Intent( android.content.Intent.ACTION_SEND );
                intent.setType( "text/plain" );
                intent.addFlags( Intent.FLAG_ACTIVITY_NEW_DOCUMENT );
                intent.putExtra( Intent.EXTRA_TEXT, message );
                // intent.putExtra( Intent.EXTRA_SUBJECT, subject );
                // intent.putExtra( Intent.EXTRA_EMAIL, new String[] { emailTo } );
                startActivity( Intent.createChooser( intent, getText( R.string.actionShare_title ) ) );
            }
        };
        
        new Builder( this ).setTitle( title ).setMessage( message.toString() )
                .setNeutralButton( R.string.actionShare_title, shareHandler ).create().show();
    }
    
    private void popupAppVersion()
    {
        String title = getString( R.string.menuItem_appVersion );
        String message = getString( R.string.popup_version, mAppData.appVersion, mAppData.appVersionCode );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        refreshViews();
    }
    
    @TargetApi( 11 )
    private void refreshViews()
    {
        // Refresh the preferences object in case another activity changed the data
        mUserPrefs = new UserPrefs( this );
        
        // Refresh the action bar
        if( AppData.IS_HONEYCOMB )
            invalidateOptionsMenu();
        
        // Refresh the gallery
        refreshGrid( new ConfigFile( mUserPrefs.romInfoCache_cfg ) );
    }
}
