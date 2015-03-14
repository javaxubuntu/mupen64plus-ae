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
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.preference.RomsFolder;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask.CacheRomInfoListener;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.task.ComputeMd5Task.ComputeMd5Listener;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomHeader;
import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.GalleryView;
import paulscode.android.mupen64plusae.GameSidebar;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v4.internal.view.SupportMenuItem;
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
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
import android.view.inputmethod.InputMethodManager;

import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.cheat.CheatFile;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;

public class GalleryActivity extends ActionBarActivity implements ComputeMd5Listener, CacheRomInfoListener
{
    // Saved instance states
    public static final String STATE_QUERY = "query";
    public static final String STATE_SIDEBAR = "sidebar";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    // Widgets
    private GalleryView mGridView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MenuListView mDrawerList;
    private ImageButton mActionButton;
    private SupportMenuItem mSearchItem;
    private GameSidebar mGameSidebar;
    
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
    
    // Misc.
    private List<GalleryItem> mGalleryItems = null;
    private GalleryItem mSelectedItem = null;
    private boolean mDragging = false;
    private GamePrefs mGamePrefs = null;
    private RomHeader mRomHeader = null;
    private boolean mShowCheats = false;
    private boolean mShowGamepads = false;
    
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
        mGridView = (GalleryView) findViewById( R.id.gridview );
        
        // Configure the Floating Action Button to add files or folders to the library
        mActionButton = (ImageButton) findViewById( R.id.add );
        mActionButton.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                promptSearchPath( null, true );
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
            public void onDrawerStateChanged( int newState )
            {
                // Intercepting the drawer open animation and re-closing it causes onDrawerClosed to not fire,
                // So detect when this happens and wait until the drawer closes to handle it manually
                if ( newState == DrawerLayout.STATE_DRAGGING )
                {
                    // INTERCEPTED!
                    mDragging = true;
                    hideSoftKeyboard();
                }
                else if ( newState == DrawerLayout.STATE_IDLE )
                {
                    if ( mDragging && !mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                    {
                        // onDrawerClosed from dragging it
                        mDragging = false;
                        mDrawerList.setVisibility( View.VISIBLE );
                        mGameSidebar.setVisibility( View.GONE );
                        mSelectedItem = null;
                    }
                }
            }
            
            @Override
            public void onDrawerClosed( View drawerView )
            {
                // Hide the game information sidebar
                mDrawerList.setVisibility( View.VISIBLE );
                mGameSidebar.setVisibility( View.GONE );
                mSelectedItem = null;
                
                showActionButton();
                super.onDrawerClosed( drawerView );
            }

            @Override
            public void onDrawerOpened( View drawerView )
            {
                hideSoftKeyboard();
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
        mGameSidebar = (GameSidebar) findViewById( R.id.gameSidebar );
        
        // Load some values used to define the grid view layout
        galleryMaxWidth = (int) getResources().getDimension( R.dimen.galleryImageWidth );
        galleryHalfSpacing = (int) getResources().getDimension( R.dimen.galleryHalfSpacing );
        galleryAspectRatio = galleryMaxWidth * 1.0f/getResources().getDimension( R.dimen.galleryImageHeight );
        
        // Update the grid size and spacing whenever the layout changes
        mGridView.setOnLayoutChangeListener( new GalleryView.OnLayoutChangeListener()
        {
            public void onLayoutChange( View view, int left, int top, int right, int bottom )
            {
                // Update the grid layout
                int width = right - left - galleryHalfSpacing * 2;
                galleryColumns = (int) Math.ceil(width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
                galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;
                
                GridLayoutManager layoutManager = (GridLayoutManager) mGridView.getLayoutManager();
                layoutManager.setSpanCount( galleryColumns );
            }
        });
        
        // Populate the gallery with the games
        refreshGrid();
        
        // Pop up a warning if the installation appears to be corrupt
        if( !mAppData.isValidInstallation )
        {
            CharSequence title = getText( R.string.invalidInstall_title );
            CharSequence message = getText( R.string.invalidInstall_message );
            new Builder( this ).setTitle( title ).setMessage( message ).create().show();
        }
        
        if ( savedInstanceState != null )
        {
            mSelectedItem = null;
            String md5 = savedInstanceState.getString( STATE_SIDEBAR );
            if ( md5 != null )
            {
                // Repopulate the game sidebar
                for ( GalleryItem item : mGalleryItems )
                {
                    if ( md5.equals( item.md5 ) )
                    {
                        onGalleryItemClick( item, null );
                        break;
                    }
                }
            }
            
            String query = savedInstanceState.getString( STATE_QUERY );
            if ( query != null )
                mSearchQuery = query;
        }
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        savedInstanceState.putString( STATE_QUERY, mSearchView.getQuery().toString() );
        if ( mSelectedItem != null )
            savedInstanceState.putString( STATE_SIDEBAR, mSelectedItem.md5 );
        else
            savedInstanceState.putString( STATE_SIDEBAR, null );
        
        super.onSaveInstanceState(savedInstanceState);
    }
    
    public void hideSoftKeyboard()
    {
        // Hide the soft keyboard if needed
        if ( mSearchView == null )
            return;
        
        InputMethodManager imm = (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE );
        imm.hideSoftInputFromWindow( mSearchView.getWindowToken(), 0 );
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
        getMenuInflater().inflate( R.menu.gallery_activity, menu );
        
        SearchManager searchManager = (SearchManager) this.getSystemService( Context.SEARCH_SERVICE );
        mSearchItem = (SupportMenuItem) menu.findItem( R.id.menuItem_search );
        SearchView searchView = (SearchView) mSearchItem.getActionView();
        searchView.setSearchableInfo( searchManager.getSearchableInfo( this.getComponentName() ) );
        mSearchItem.setSupportOnActionExpandListener(new MenuItemCompat.OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse( MenuItem item )
            {
                mSearchQuery = "";
                refreshGrid();
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
                refreshGrid();
                return false;
            }
        });
        
        if ( ! "".equals( mSearchQuery ) )
        {
            String query = mSearchQuery;
            MenuItemCompat.expandActionView( mSearchItem );
            mSearchView.setQuery( query, true );
        }
        
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        MenuItem sortItem = menu.findItem( R.id.menuItem_sort );
        if ( mUserPrefs.getGallerySortReverse() )
            sortItem.setIcon( getResources().getDrawable( R.drawable.ic_sort_desc ) );
        else
            sortItem.setIcon( getResources().getDrawable( R.drawable.ic_sort_asc ) );
        
        SubMenu submenu = sortItem.getSubMenu();
        if ( mUserPrefs.SORT_BY_DATE.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_date ).setChecked( true );
        else if ( mUserPrefs.SORT_BY_DEVELOPER.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_developer ).setChecked( true );
        else if ( mUserPrefs.SORT_BY_LAST_PLAYED.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_lastPlayed ).setChecked( true );
        else if ( mUserPrefs.SORT_BY_PUBLISHER.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_publisher ).setChecked( true );
        else if ( mUserPrefs.SORT_BY_GENRE.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_genre ).setChecked( true );
        else if ( mUserPrefs.SORT_BY_ESRB.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_esrb ).setChecked( true );
        else if ( mUserPrefs.SORT_BY_PLAYERS.equals( mUserPrefs.getGallerySort() ) )
            submenu.findItem( R.id.menuItem_players ).setChecked( true );
        else
            submenu.findItem( R.id.menuItem_name ).setChecked( true );
        
        return super.onPrepareOptionsMenu( menu );
    }
    
    private boolean setSortType( String sortType )
    {
        if ( sortType.equals( mUserPrefs.getGallerySort() ) )
            mUserPrefs.putGallerySortReverse( !mUserPrefs.getGallerySortReverse() );
        
        mUserPrefs.putGallerySort( sortType );
        invalidateOptionsMenu();
        refreshGrid();
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem menuItem )
    {
        switch( menuItem.getItemId() )
        {
            case R.id.menuItem_refreshRoms:
                refreshRoms( null );
                return true;
            case R.id.menuItem_name:
                return setSortType( mUserPrefs.SORT_BY_NAME );
            case R.id.menuItem_date:
                return setSortType( mUserPrefs.SORT_BY_DATE );
            case R.id.menuItem_developer:
                return setSortType( mUserPrefs.SORT_BY_DEVELOPER );
            case R.id.menuItem_publisher:
                return setSortType( mUserPrefs.SORT_BY_PUBLISHER );
            case R.id.menuItem_genre:
                return setSortType( mUserPrefs.SORT_BY_GENRE );
            case R.id.menuItem_esrb:
                return setSortType( mUserPrefs.SORT_BY_ESRB );
            case R.id.menuItem_players:
                return setSortType( mUserPrefs.SORT_BY_PLAYERS );
            case R.id.menuItem_lastPlayed:
                return setSortType( mUserPrefs.SORT_BY_LAST_PLAYED );
            default:
                return super.onOptionsItemSelected( menuItem );
        }
    }
    
    public void onGalleryItemLongClick( GalleryItem item, View parentView )
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
    
    public void addProfiles( int icon, int title, int indentation, Profile profile, String builtinPath, String customPath, String defaultValue )
    {
        // For now...
        if ( profile == null ) return;
        
        mGameSidebar.addRow( icon,
            getString( title ),
            profile.name,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    
                }
            }, 0x0, indentation);
        
        /*ConfigFile configBuiltin = new ConfigFile( builtinPath );
        ConfigFile configCustom = new ConfigFile( customPath );
        List<Profile> profiles = new ArrayList<Profile>();
        profiles.addAll( Profile.getProfiles( configBuiltin, true ) );
        profiles.addAll( Profile.getProfiles( configCustom, false ) );
        Collections.sort( profiles );
        
        int offset = mAllowDisable ? 1 : 0;
        int numEntries = profiles.size() + offset;
        CharSequence[] entries = new CharSequence[numEntries];
        String[] values = new String[numEntries];
        if( mAllowDisable )
        {
            entries[0] = getContext().getText( R.string.listItem_disabled );
            values[0] = "";
        }
        for( int i = 0; i < profiles.size(); i++ )
        {
            Profile profile = profiles.get( i );
            String entryHtml = profile.name;
            if( !TextUtils.isEmpty( profile.comment ) )
                entryHtml += "<br><small>" + profile.comment + "</small>";
            entries[i + offset] = Html.fromHtml( entryHtml );
            values[i + offset] = profile.name;
        }
        
        // Set the list entries and values; select default if persisted selection no longer exists
        setEntries( entries );
        setEntryValues( values );
        String selectedValue = getPersistedString( null );
        if( !ArrayUtils.contains( values, selectedValue ) )
            persistString( defaultValue );
        selectedValue = getPersistedString( null );
        setValue( selectedValue );*/
    }
    
    private void addCheats()
    {
        if ( mSelectedItem == null || mRomHeader == null ) return;
        
        // Display the cheats for this game
        String crc = mRomHeader.crc;
        Log.v( "GalleryActivity", "building from CRC = " + crc );
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "GalleryActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        
        ArrayList<Cheat> cheats = new ArrayList<Cheat>();
        cheats.addAll( CheatUtils.populate( crc, mupencheat_txt, true, this ) );
        CheatUtils.reset();
        
        // Layout the menu, populating it with appropriate cheat options
        for( int i = 0; i < cheats.size(); i++ )
        {
            // Get the short title of the cheat (shown in the menu)
            String title;
            if( cheats.get( i ).name == null )
                title = getString( R.string.cheats_defaultName, i );
            else
                title = cheats.get( i ).name;
            String notes = cheats.get( i ).desc;
            
            /*String options = cheats.get( i ).option;
            String[] optionStrings = null;
            if( !TextUtils.isEmpty( options ) )
                optionStrings = options.split( "\n" );
            
            // Create the menu item associated with this cheat
            final CheatPreference pref = new CheatPreference( this, title, notes, optionStrings );
            pref.setKey( crc + " Cheat" + i );*/
            
            /*int cheatIcon = R.drawable.ic_box;
            if ( pref.isCheatEnabled() )
                cheatIcon = R.drawable.ic_check;*/
            
            // Add the preference menu item to the cheats category
            mGameSidebar.addRow( R.drawable.ic_box, title, notes, new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    
                }
            });
        }
    }
    
    public void updateSidebar()
    {
        GalleryItem item = mSelectedItem;
        if ( item == null ) return;
        
        // Set the game options
        mGameSidebar.clear();
        
        final GalleryItem finalItem = item;
        final Context finalContext = this;
        
        mGameSidebar.addRow( R.drawable.ic_play,
            getString( R.string.actionResume_title ),
            getString( R.string.actionResume_summary ),
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    launchGame( false );
                }
            });
        
        mGameSidebar.addRow( R.drawable.ic_undo,
            getString( R.string.actionRestart_title ),
            getString( R.string.actionRestart_summary ),
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    CharSequence title = getText( R.string.confirm_title );
                    CharSequence message = getText( R.string.confirmResetGame_message );
                    Prompt.promptConfirm( finalContext, title, message, new PromptConfirmListener()
                    {
                        @Override
                        public void onConfirm()
                        {
                            launchGame( true );
                        }
                    });
                }
            });
        
        /*mGameSidebar.addRow( R.drawable.ic_settings,
            "Settings",
            null,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    PlayMenuActivity.action = null;
                    launchPlayMenuActivity( finalItem.romFile.getAbsolutePath(), finalItem.md5 );
                }
            });*/
        
        // Cheats
        int cheatsSummary = mGamePrefs.getCheatsEnabled()
                ? R.string.screenCheats_summaryEnabled
                : R.string.screenCheats_summaryDisabled;
        
        int cheatsIcon = R.drawable.ic_arrow_d;
        if ( mShowCheats ) cheatsIcon = R.drawable.ic_arrow_u;
        
        mGameSidebar.addRow( R.drawable.ic_key,
            getString( R.string.screenCheats_title ),
            getString( cheatsSummary ),
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    mShowCheats = !mShowCheats;
                    updateSidebar();
                }
            }, cheatsIcon, 0 );
        
        if ( mShowCheats )
        {
            int icon = R.drawable.ic_box;
            if ( mGamePrefs.getCheatsEnabled() ) icon = R.drawable.ic_check;
            
            mGameSidebar.addRow( icon, getString( R.string.playShowCheats_title ), getString( R.string.playShowCheats_summary ), new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    mGamePrefs.putCheatsEnabled( !mGamePrefs.getCheatsEnabled() );
                    updateSidebar();
                }
            }, 0x0, 0 );
            
            // Add the cheats available for this game
            if ( mGamePrefs.getCheatsEnabled() )
                addCheats();
            
            // New cheat
            mGameSidebar.addRow( R.drawable.ic_plus, getString( R.string.newCheat_title ), getString( R.string.newCheat_summary ), new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    // Show the new cheat dialog
                    
                }
            }, 0x0, 0 );
        }
        
        // Add Emulation, Touchscreen, Controllers 1 through 4, and the Player Map
        mGameSidebar.addHeading( getString( R.string.menuItem_profiles ) );
        
        addProfiles( R.drawable.ic_circuit, R.string.emulationProfile_title, 0, mGamePrefs.emulationProfile, mAppData.emulationProfiles_cfg, mUserPrefs.emulationProfiles_cfg, mUserPrefs.getEmulationProfileDefault() );
        
        addProfiles( R.drawable.ic_phone, R.string.touchscreenProfile_title, 0, mGamePrefs.touchscreenProfile, mAppData.touchscreenProfiles_cfg, mUserPrefs.touchscreenProfiles_cfg, mUserPrefs.getTouchscreenProfileDefault() );
        
        int gamepadIcon = R.drawable.ic_arrow_d;
        if ( mShowGamepads ) gamepadIcon = R.drawable.ic_arrow_u;
        
        mGameSidebar.addRow( R.drawable.ic_gamepad,
            getString( R.string.menuItem_controllerProfiles ),
            "1 player configured",
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    mShowGamepads = !mShowGamepads;
                    updateSidebar();
                }
            }, gamepadIcon, 0);
        
        if ( mShowGamepads )
        {
            if ( mSelectedItem.players >= 1 )
                addProfiles( 0x0, R.string.controllerProfile1_title, 1, mGamePrefs.controllerProfile1, mAppData.controllerProfiles_cfg, mUserPrefs.controllerProfiles_cfg, "" );
            
            if ( mSelectedItem.players >= 2 )
                addProfiles( 0x0, R.string.controllerProfile2_title, 1, mGamePrefs.controllerProfile2, mAppData.controllerProfiles_cfg, mUserPrefs.controllerProfiles_cfg, "" );
            
            if ( mSelectedItem.players >= 3 )
                addProfiles( 0x0, R.string.controllerProfile3_title, 1, mGamePrefs.controllerProfile3, mAppData.controllerProfiles_cfg, mUserPrefs.controllerProfiles_cfg, "" );
            
            if ( mSelectedItem.players >= 4 )
                addProfiles( 0x0, R.string.controllerProfile4_title, 1, mGamePrefs.controllerProfile4, mAppData.controllerProfiles_cfg, mUserPrefs.controllerProfiles_cfg, "" );
        }
        
        
        // Add a Help & Feedback section for Wiki, Report Bug, and Reset defaults
        mGameSidebar.addHeading( getString( R.string.menuItem_help ) );
        
        final String wikiUrl = RomDatabase.wikiUrlForName( item.goodName );
        if( !TextUtils.isEmpty( wikiUrl ) )
        {
            mGameSidebar.addRow( R.drawable.ic_help,
                getString( R.string.actionWiki_title ),
                getString( R.string.actionWiki_summary ),
                new GameSidebar.Action()
                {
                    @Override
                    public void onAction()
                    {
                        Utility.launchUri( finalContext, wikiUrl );
                    }
                });
        }
        
        mGameSidebar.addRow( R.drawable.ic_debug,
            getString( R.string.menuItem_reportBug ),
            getString( R.string.menuItem_reportBug_subtitle, item.baseName ),
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    
                }
            });
        
        mGameSidebar.addRow( R.drawable.ic_undo,
            getString( R.string.actionResetGamePrefs_title ),
            getString( R.string.actionResetGamePrefs_summary ),
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    
                }
            });
        
        // Lastly, add the Information section
        mGameSidebar.addInformation( item.goodName, item.date, item.developer, item.publisher, item.genre, item.players + "", item.esrb, item.lastPlayed );
    }
    
    public void onGalleryItemClick( GalleryItem item, View parentView )
    {
        mSelectedItem = item;
        mRomHeader = new RomHeader( item.romPath );
        mGamePrefs = new GamePrefs( this, item.md5, mRomHeader );
        
        // Show the game info sidebar
        mDrawerList.setVisibility( View.GONE );
        mGameSidebar.setVisibility( View.VISIBLE );
        mGameSidebar.scrollTo( 0, 0 );
        
        // Set the cover art in the sidebar
        item.loadBitmap();
        mGameSidebar.setImage( item.artBitmap );
        
        // Set the game title
        String romName = item.goodName;
        if ( !mUserPrefs.getShowFullNames() )
            romName = item.baseName;
        mGameSidebar.setTitle( romName );
        
        updateSidebar();
        
        // Open the navigation drawer
        mDrawerLayout.openDrawer( GravityCompat.START );
    }
    
    private void launchGame( boolean isRestarting )
    {
        if ( mSelectedItem == null ) return;
        
        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        if( mSelectedItem.players > 1 && mGamePrefs.playerMap.isEnabled()
                && mUserPrefs.getPlayerMapReminder() )
        {
            mGamePrefs.playerMap.removeUnavailableMappings();
            boolean needs1 = mGamePrefs.isControllerEnabled1 && !mGamePrefs.playerMap.isMapped( 1 );
            boolean needs2 = mGamePrefs.isControllerEnabled2 && !mGamePrefs.playerMap.isMapped( 2 );
            boolean needs3 = mGamePrefs.isControllerEnabled3 && !mGamePrefs.playerMap.isMapped( 3 )
                    && mSelectedItem.players > 2;
            boolean needs4 = mGamePrefs.isControllerEnabled4 && !mGamePrefs.playerMap.isMapped( 4 )
                    && mSelectedItem.players > 3;
            
            /*if( needs1 || needs2 || needs3 || needs4 )
            {
                @SuppressWarnings( "deprecation" )
                PlayerMapPreference pref = (PlayerMapPreference) findPreference( "playerMap" );
                pref.show();
                return;
            }*/
        }
        
        // Make sure that the storage is accessible
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "CheatMenuHandler", "SD Card not accessible in method onPreferenceClick" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );
            return;
        }
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );
        
        // Update the ConfigSection with the new value for lastPlayed
        String lastPlayed = Integer.toString( (int) ( new Date().getTime()/1000 ) );
        ConfigFile config = new ConfigFile( mUserPrefs.romInfoCache_cfg );
        if ( config != null )
        {
            config.put( mSelectedItem.md5, "lastPlayed", lastPlayed );
            config.save();
        }
        
        // Launch the appropriate game activity
        Intent intent = mUserPrefs.isTouchpadEnabled ? new Intent( this,
                GameActivityXperiaPlay.class ) : new Intent( this, GameActivity.class );
        
        // Pass the startup info via the intent
        intent.putExtra( Keys.Extras.ROM_PATH, mSelectedItem.romPath );
        intent.putExtra( Keys.Extras.ROM_MD5, mSelectedItem.md5 );
        //intent.putExtra( Keys.Extras.CHEAT_ARGS, getCheatArgs() );
        intent.putExtra( Keys.Extras.ART_PATH, mSelectedItem.artPath );
        intent.putExtra( Keys.Extras.ROM_NAME, mSelectedItem.goodName );
        intent.putExtra( Keys.Extras.ROM_DATE, mSelectedItem.date );
        intent.putExtra( Keys.Extras.ROM_DEVELOPER, mSelectedItem.developer );
        intent.putExtra( Keys.Extras.ROM_PUBLISHER, mSelectedItem.publisher );
        intent.putExtra( Keys.Extras.ROM_GENRE, mSelectedItem.genre );
        intent.putExtra( Keys.Extras.ROM_ESRB, mSelectedItem.esrb );
        intent.putExtra( Keys.Extras.ROM_PLAYERS, mSelectedItem.players );
        
        startActivity( intent );
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
            ConfigFile config = new ConfigFile( mUserPrefs.romInfoCache_cfg );
            String romName = config.get( md5, "goodName" );
            String artPath = config.get( md5, "artPath" );
            String romDate = config.get( md5, "date" );
            String romDeveloper = config.get( md5, "developer" );
            String romPublisher = config.get( md5, "publisher" );
            String romGenre = config.get( md5, "genre" );
            String romPlayers = config.get( md5, "players" );
            String romESRB = config.get( md5, "esrb" );
            
            Intent intent = new Intent( GalleryActivity.this, PlayMenuActivity.class );
            intent.putExtra( Keys.Extras.ROM_PATH, romPath );
            intent.putExtra( Keys.Extras.ROM_MD5, md5 );
            intent.putExtra( Keys.Extras.ROM_NAME, romName );
            intent.putExtra( Keys.Extras.ART_PATH, artPath );
            intent.putExtra( Keys.Extras.ROM_DATE, romDate );
            intent.putExtra( Keys.Extras.ROM_DEVELOPER, romDeveloper );
            intent.putExtra( Keys.Extras.ROM_PUBLISHER, romPublisher );
            intent.putExtra( Keys.Extras.ROM_ESRB, romESRB );
            intent.putExtra( Keys.Extras.ROM_GENRE, romGenre );
            intent.putExtra( Keys.Extras.ROM_PLAYERS, romPlayers );
            startActivity( intent );
        }
    }
    
    private void promptSearchPath( File startDir, boolean searchZips )
    {
        // Prompt for search path, then asynchronously search for ROMs
        if( startDir == null || !startDir.exists() )
            startDir = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        
        ScanRomsDialog dialog = new ScanRomsDialog( this, startDir, false, searchZips, new ScanRomsDialogListener()
        {
            @Override
            public void onDialogClosed( File file, int which, boolean searchZips )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    // Add this directory to the list of ROMs folders to search
                    mUserPrefs.addRomsFolder( new RomsFolder( file.getAbsolutePath(), searchZips ) );
                    
                    // Search this folder for ROMs
                    refreshRoms( new RomsFolder( file.getAbsolutePath(), searchZips ) );
                }
                else if( file != null )
                {
                    if( file.isDirectory() )
                    {
                        promptSearchPath( file, searchZips );
                    }
                    else
                    {
                        // The user selected an individual file
                        refreshRoms( new RomsFolder( file.getAbsolutePath(), searchZips ) );
                    }
                }
            }
        });
        
        dialog.show();
    }
    
    private void refreshRoms( final RomsFolder startDir )
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
    
    private void refreshGrid()
    {
        refreshGrid( new ConfigFile( mUserPrefs.romInfoCache_cfg ) );
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
                String date = config.get( md5, "date" );
                String developer = config.get( md5, "developer" );
                String publisher = config.get( md5, "publisher" );
                String genre = config.get( md5, "genre" );
                
                // Strip the region and dump information
                String baseName = goodName;
                if ( !showFullNames )
                    baseName = RomDatabase.baseNameForName( goodName );
                
                boolean matchesSearch = true;
                if ( searches != null && searches.length > 0 )
                {
                    // Make sure the ROM name contains every token in the query
                    String lowerName = baseName.toLowerCase();
                    String lowerDev = developer.toLowerCase();
                    String lowerPub = publisher.toLowerCase();
                    String lowerGen = genre.toLowerCase();
                    
                    for( String search : searches )
                    {
                        if ( search.length() > 0 &&
                             !lowerName.contains( search ) &&
                             !(lowerDev != null && lowerDev.contains( search ) ) &&
                             !(lowerPub != null && lowerPub.contains( search ) ) &&
                             !(lowerGen != null && lowerGen.contains( search ) ) &&
                             !(date != null && date.contains( search ) ) )
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
                    String players = config.get( md5, "players" );
                    String esrb = config.get( md5, "esrb" );
                    
                    String lastPlayedStr = config.get( md5, "lastPlayed" );
                    int lastPlayed = 0;
                    if ( lastPlayedStr != null )
                        lastPlayed = Integer.parseInt( lastPlayedStr );
                    
                    GalleryItem item = new GalleryItem( this, md5, goodName, baseName, romPath, artPath, date, developer, publisher, genre, players, esrb, lastPlayed );
                    items.add( item );
                    if ( showRecentlyPlayed && currentTime - item.lastPlayed <= 60 * 60 * 24 * 7 ) // 7 days
                        recentItems.add( item );
                }
            }
        }
        
        Collections.sort( items, new GalleryItem.NameComparator() );
        String sort = mUserPrefs.getGallerySort();
        if ( sort != null )
        {
            if ( mUserPrefs.SORT_BY_DATE.equals( sort ) )
                Collections.sort( items, new GalleryItem.DateComparator() );
            else if ( mUserPrefs.SORT_BY_DEVELOPER.equals( sort ) )
                Collections.sort( items, new GalleryItem.DeveloperComparator() );
            else if ( mUserPrefs.SORT_BY_PUBLISHER.equals( sort ) )
                Collections.sort( items, new GalleryItem.PublisherComparator() );
            else if ( mUserPrefs.SORT_BY_GENRE.equals( sort ) )
                Collections.sort( items, new GalleryItem.GenreComparator() );
            else if ( mUserPrefs.SORT_BY_ESRB.equals( sort ) )
                Collections.sort( items, new GalleryItem.ESRBComparator() );
            else if ( mUserPrefs.SORT_BY_PLAYERS.equals( sort ) )
                Collections.sort( items, new GalleryItem.PlayersComparator() );
            else if ( mUserPrefs.SORT_BY_LAST_PLAYED.equals( sort ) )
                Collections.sort( items, new GalleryItem.LastPlayedComparator() );
        }
        if ( mUserPrefs.getGallerySortReverse() )
            Collections.reverse( items );
        
        if ( recentItems != null )
        {
            Collections.sort( recentItems, new GalleryItem.LastPlayedComparator() );
            if ( mUserPrefs.getGallerySortReverse() )
                Collections.reverse( recentItems );
        }
        
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
        
        mGalleryItems = items;
        mGridView.setAdapter( new GalleryItem.Adapter( this, items ) );
        
        // Allow the headings to take up the entire width of the layout
        final List<GalleryItem> finalItems = items;
        GridLayoutManager layoutManager = new GridLayoutManager( this, galleryColumns );
        layoutManager.setSpanSizeLookup( new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize( int position )
            {
                // Headings will take up every span (column) in the grid
                if ( finalItems.get( position ).isHeading )
                    return galleryColumns;
                
                // Games will fit in a single column
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
        
        // Set the sidebar opacity on the two sidebars
        mDrawerList.setBackgroundDrawable( new DrawerDrawable( mUserPrefs.displaySidebarTransparency ) );
        mGameSidebar.setBackgroundDrawable( new DrawerDrawable( mUserPrefs.displaySidebarTransparency ) );
        
        // Refresh the gallery
        refreshGrid();
    }
}
