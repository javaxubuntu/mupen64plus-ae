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

import paulscode.android.mupen64plusae.toolbar.ToolbarActivity;

import java.lang.CharSequence;
import android.os.Bundle;
import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.preference.PreferenceActivity;

public class PreferenceToolbarActivity extends PreferenceActivity implements ToolbarActivity {
    protected Toolbar mToolbar;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        ToolbarUtils.create( this );
        super.onCreate( savedInstanceState );
    }
    
    @Override
    public void setContentView( int layoutResID )
    {
        mToolbar = ToolbarUtils.getToolbarForContentView( this, layoutResID );
        invalidateToolbarMenu();
    }
    
    @Override
    public void setTitle( CharSequence title )
    {
        super.setTitle( title );
        mToolbar.setTitle( title );
    }
    
    @Override
    public View onCreateView( String name, Context context, AttributeSet attrs )
    {
        final View result = super.onCreateView(name, context, attrs);
        if (result != null)
            return result;
        
        return ToolbarUtils.createThemeView( this, name, context, attrs );
    }
    
    @Override
    public void invalidateOptionsMenu()
    {
        invalidateToolbarMenu();
    }
    
    public void invalidateToolbarMenu()
    {
        ToolbarUtils.invalidateToolbarMenu( this );
    }
    
    public void onCreateToolbarMenu( Toolbar toolbar )
    {
    }
    
    public boolean onToolbarItemSelected( MenuItem item )
    {
        return false;
    }
    
    public Toolbar getToolbar()
    {
        return mToolbar;
    }
}
