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
package paulscode.android.mupen64plusae.toolbar;

import org.mupen64plusae.v3.alpha.R;
import paulscode.android.mupen64plusae.persistent.AppData;

import android.view.View;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.Toolbar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.support.v7.internal.widget.TintCheckBox;
import android.support.v7.internal.widget.TintCheckedTextView;
import android.support.v7.internal.widget.TintEditText;
import android.support.v7.internal.widget.TintRadioButton;
import android.support.v7.internal.widget.TintSpinner;

/*
    The AppCompat library provides ActionBarActivity to use the new Toolbar,
    but ListActivity and PreferenceActivity don't inherit from this new class
    so they do not support Toolbars.
    
    The recommended workaround is to use Fragments, but those are only available
    in API 11 and newer, which is not an option for Mupen.
    
    Any attempts at subclassing an Activity will require the exact same code
    changes to be applied to all other subclasses, as none of them inherit from
    each other.
    
    Therefore this file provides an interface for defining support for Toolbars,
    as well as a set of reusable helper functions that the individual subclasses
    can call. It uses a layout to embed the actual Activity below its Toolbar.
    
    See ListToolbarActivity for an example of how this should be implemented
    
    Steps:
    1. Override onCreate and call ToolbarUtils.create to set the correct theme
    2. Override setContentView to embed the content view within the Toolbar layout
    
    That alone is enough to get the Toolbar working, but there will be some problems:
    - The AppCompat.NoActionBar theme uses black widgets and ignores style settings
    - The Toolbar won't have a title or a functional menu
    
    Therefore:
    3. Override onCreateView and replace the widgets with their theme-able versions
       (Using code taken directly from AppCompat's source code)
    4. Manually inflate the Toolbar menu and assign a menu item selected listener
    5. Override setTitle and set the Toolbar's title from there

*/

interface ToolbarActivity
{
    public Toolbar getToolbar();
    public void invalidateToolbarMenu();
    public void onCreateToolbarMenu( Toolbar toolbar );
    public boolean onToolbarItemSelected( MenuItem item );
}

final class ToolbarUtils {
    private ToolbarUtils()
    {
    }
    
    // This of course only works if the Toolbar is used instead of the older ActionBar
    public static void create( Activity activity )
    {
        activity.setTheme( android.support.v7.appcompat.R.style.Theme_AppCompat_NoActionBar );
    }
    
    // Inflate the content for this Activity and return the Toolbar created for it
    public static Toolbar getToolbarForContentView( Activity activity, int layoutResID )
    {
        // Inflate the layout containing the Toolbar and an empty container for the content
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        LinearLayout layout = (LinearLayout) inflater.inflate( R.layout.toolbar_activity, null );
        
        // Inflate the content into the container view provided for it
        FrameLayout wrapper = (FrameLayout) layout.findViewById( R.id.wrapper );
        inflater.inflate( layoutResID, wrapper, true);
        activity.getWindow().setContentView( layout );
        
        // Set the Toolbar's title and return it
        Toolbar toolbar = (Toolbar) layout.findViewById( R.id.toolbar );
        toolbar.setTitle( activity.getTitle() );
        
        return toolbar;
    }
    
    // Replace the non-tinted views with their tinted counterparts during the inflation process
    // Without this the widgets will be black, which is hidden against the dark background
    
    public static View createThemeView( Activity activity, String name, Context context, AttributeSet attrs )
    {
        // This is identical to what the AppCompat library does for ActionBarActivity
        // https://github.com/android/platform_frameworks_support/blob/master/v7/appcompat/src/android/support/v7/app/ActionBarActivityDelegateBase.java
        
        if ( !AppData.IS_LOLLIPOP )
        {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the standard framework versions
            if ( "EditText".equals( name ) )
                return new TintEditText( activity, attrs );
            else if ( "Spinner".equals( name ) )
                return new TintSpinner( activity, attrs );
            else if ( "CheckBox".equals( name ) )
                return new TintCheckBox( activity, attrs );
            else if ( "RadioButton".equals( name ) )
                return new TintRadioButton( activity, attrs );
            else if ( "CheckedTextView".equals( name ) )
                return new TintCheckedTextView( activity, attrs );
        }
        
        return null;
    }
    
    // Recreate the Toolbar's menu
    public static void invalidateToolbarMenu( ToolbarActivity toolbarActivity )
    {
        // Sanity check: ToolbarActivity should only be implemented by Activities!
        if ( !( toolbarActivity instanceof Activity ) )
            return;
        
        // There's apparently a bug with using getMenuInflater().inflate( resource, toolbar.getMenu() )
        // When that is used, the showAsAction tags don't seem to inflate correctly
        // Therefore use the solution presented here
        
        final ToolbarActivity parentActivity = toolbarActivity;
        Toolbar toolbar = toolbarActivity.getToolbar();
        toolbar.getMenu().clear();
        toolbarActivity.onCreateToolbarMenu( toolbar );
        
        // When a menu item is selected, pass it back to the parent ToolbarActivity
        toolbar.setOnMenuItemClickListener( new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem item )
            {
                return parentActivity.onToolbarItemSelected( item );
            }
        });
    }
}
