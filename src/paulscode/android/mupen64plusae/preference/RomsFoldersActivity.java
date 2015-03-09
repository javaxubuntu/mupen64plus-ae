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
package paulscode.android.mupen64plusae.preference;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.io.File;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog.ScanRomsDialogListener;
import paulscode.android.mupen64plusae.preference.RomsFolder;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.toolbar.ListToolbarActivity;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

public class RomsFoldersActivity extends ListToolbarActivity
{
    /** The user preferences wrapper, available as a convenience to subclasses. */
    protected UserPrefs mUserPrefs;
    
    private final List<RomsFolder> mFolders = new ArrayList<RomsFolder>();
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        refreshList();
    }
    
    @Override
    public void onCreateToolbarMenu( Toolbar toolbar )
    {
        toolbar.inflateMenu( R.menu.profile_activity );
        toolbar.getMenu().findItem( R.id.menuItem_toggleBuiltins ).setVisible( false );
    }
    
    @Override
    public boolean onToolbarItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_new:
                addFolder();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }
    
    @Override
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        // Popup a dialog with a context-sensitive list of options for the folder
        final RomsFolder folder = (RomsFolder) getListView().getItemAtPosition( position );
        if( folder != null )
        {
            CharSequence[] items = getResources().getTextArray( R.array.romsFoldersClickCustom_entries );
            
            Builder builder = new Builder( this );
            builder.setTitle( getString( R.string.popup_titleCustom, folder.path ) );
            builder.setItems( items, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    switch( which )
                    {
                        case 0:
                            editFolder( folder );
                            break;
                        case 1:
                            removeFolder( folder );
                            break;
                    }
                }
            });
            builder.create().show();
        }
        super.onListItemClick( l, v, position, id );
    }
    
    private void addFolder()
    {
        updateFolder( null, null );
    }
    
    private void editFolder( RomsFolder folder )
    {
        updateFolder( folder, null );
    }
    
    private void updateFolder( RomsFolder folder, File startDir )
    {
        if ( startDir == null && folder != null && folder.path != null )
            startDir = new File( folder.path );
        
        // Prompt for search path for the ROMs folder
        if( startDir == null || !startDir.exists() )
            startDir = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        
        final RomsFolder oldFolder = new RomsFolder( folder );
        boolean searchZips = true;
        if ( folder != null )
            searchZips = folder.searchZips;
        
        ScanRomsDialog dialog = new ScanRomsDialog( this, startDir, true, searchZips, new ScanRomsDialogListener()
        {
            @Override
            public void onDialogClosed( File file, int which, boolean searchZips )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    if ( oldFolder != null )
                        mUserPrefs.removeRomsFolder( oldFolder );
                    
                    mUserPrefs.addRomsFolder( new RomsFolder( file.getAbsolutePath(), searchZips ) );
                    refreshList();
                }
                else if( file != null )
                {
                    oldFolder.searchZips = searchZips;
                    updateFolder( oldFolder, file );
                }
            }
        });
        
        dialog.show();
    }
    
    private void removeFolder( RomsFolder folder )
    {
        final RomsFolder finalFolder = folder;
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.confirmRemoveFolder_message, folder.path );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                mUserPrefs.removeRomsFolder( finalFolder );
                refreshList();
            }
        } );
    }
    
    private void refreshList()
    {
        // Get the folders to be shown to the user
        setListAdapter( new RomsFoldersListAdapter( this, Arrays.asList( mUserPrefs.romsDirs ) ) );
    }
    
    private class RomsFoldersListAdapter extends ArrayAdapter<RomsFolder>
    {
        private static final int RESID = R.layout.list_item_two_text_icon;
        
        public RomsFoldersListAdapter( Context context, List<RomsFolder> folders )
        {
            super( context, RESID, folders );
        }
        
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            Context context = getContext();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( RESID, null );
            
            RomsFolder folder = getItem( position );
            if( folder != null && folder.path != null )
            {
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                int index = folder.path.lastIndexOf( "/" ) + 1;
                text1.setText( folder.path.substring( index ) );
                text2.setText( folder.path.substring( 0, index ) );
                icon.setImageResource( R.drawable.ic_folder );
            }
            return view;
        }
    }
}
