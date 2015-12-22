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
 * Authors: xperia64
 */
package paulscode.android.mupen64plusae.cheat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.compat.AppCompatListActivity;
import paulscode.android.mupen64plusae.dialog.EditCheatDialog;
import paulscode.android.mupen64plusae.dialog.EditCheatDialog.CheatOptionData;
import paulscode.android.mupen64plusae.dialog.EditCheatDialog.OnEditCompleteListener;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment.OnDialogMenuItemSelectedListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask.ExtractCheatListener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CheatEditorActivity extends AppCompatListActivity implements View.OnClickListener, ExtractCheatListener,
    OnDialogMenuItemSelectedListener, OnEditCompleteListener
{
    private static class CheatListAdapter extends ArrayAdapter<Cheat>
    {
        private static final int RESID = R.layout.list_item_two_text_icon;
        
        public CheatListAdapter( Context context, List<Cheat> cheats )
        {
            super( context, RESID, cheats );
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
            
            Cheat item = getItem( position );
            if( item != null )
            {
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                text1.setText( item.name );
                text2.setText( item.desc );
                icon.setImageResource( R.drawable.ic_key );
            }
            return view;
        }
    }
    
    private static final String STATE_MENU_DIALOG_FRAGMENT = "STATE_MENU_DIALOG_FRAGMENT";
    private static final String STATE_CHEAT_EDIT_DIALOG_FRAGMENT = "STATE_CHEAT_EDIT_DIALOG_FRAGMENT";
    
    private final ArrayList<Cheat> userCheats = new ArrayList<Cheat>();
    private final ArrayList<Cheat> systemCheats = new ArrayList<Cheat>();
    private CheatListAdapter cheatListAdapter = null;
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private int mSelectedCheat = 0;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGlobalPrefs.enforceLocale( this );
        
        // Get the ROM header info
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path must be passed via the extras bundle when starting CheatEditorActivity" );
        
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
        
        setContentView( R.layout.cheat_editor );
        reload( mRomCrc, mRomCountryCode );
        findViewById( R.id.imgBtnChtAdd ).setOnClickListener( this );
        findViewById( R.id.imgBtnChtSave ).setOnClickListener( this );
        findViewById( R.id.imgBtnChtInfo ).setOnClickListener( this );
        
        //default state is cancelled unless we save
        setResult(RESULT_CANCELED, null);
    }
    
    private void reload( String crc, byte countryCode )
    {
        Log.v( "CheatEditorActivity", "building from CRC = " + crc );
        
        if( crc == null )
            return;
        
        //Do this in a separate task since it takes longer
        ExtractCheatsTask cheatsTask = new ExtractCheatsTask(this, this, mAppData.mupencheat_default, crc, countryCode);
        cheatsTask.execute((String) null);
        
        //We don't extract user cheats in a separate task since there aren't as many
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );
        userCheats.clear();        
        userCheats.addAll( CheatUtils.populate( mRomCrc, mRomCountryCode, usrcheat_txt, false, this ) );
        
        cheatListAdapter = new CheatListAdapter( this, userCheats );
        setListAdapter( cheatListAdapter );
    }
    
    @Override
    public void onExtractFinished(ArrayList<Cheat> moreCheats)
    {
        systemCheats.clear();
        systemCheats.addAll( moreCheats );
    }    
    
    private void save( String crc )
    {
        ArrayList<Cheat> combinedCheats;
        combinedCheats = new ArrayList<Cheat>();
        combinedCheats.addAll(systemCheats);
        combinedCheats.addAll(userCheats);
        Collections.sort(combinedCheats);
        
        CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt, true );
        CheatUtils.save( crc, usrcheat_txt, userCheats, mRomHeaderName, mRomCountryCode, this, false );
        CheatUtils.save( crc, mupencheat_txt, combinedCheats, mRomHeaderName, mRomCountryCode, this, true );
        
        setResult(RESULT_OK, null);
    }
    
    @Override
    protected void onListItemClick( ListView l, View v, final int position, long id )
    {
        int resId = R.menu.cheat_editor_activity;
        int stringId = R.string.touchscreenProfileActivity_menuTitle;
        mSelectedCheat = position; 
        
        MenuDialogFragment menuDialogFragment = MenuDialogFragment.newInstance(0,
           getString(stringId), resId);
        
        FragmentManager fm = getSupportFragmentManager();
        menuDialogFragment.show(fm, STATE_MENU_DIALOG_FRAGMENT);
    }
    
    @Override
    public void onPrepareMenuList(MenuListView listView)
    {
        //Nothing to do here
    }
    
    @Override
    public void onDialogMenuItemSelected( int dialogId, MenuItem item)
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_edit:
                CreateCheatEditorDialog();
                break;
            case R.id.menuItem_delete:
                //TODO: Account for rotation
                promptDelete(mSelectedCheat);
                break;
            default:
                return;
        }
    }
    
    private void CreateCheatEditorDialog()
    {
        int stringId = R.string.cheatEditor_edit1;
        final Cheat cheat = userCheats.get( mSelectedCheat );
        ArrayList<CheatOptionData> optionsList = new ArrayList<CheatOptionData>();
        
        String options = cheat.option;
        String[] optionStrings = null;
        if( !TextUtils.isEmpty( options ) )
        {
            optionStrings = options.split( "\n" );
            
            for(String option : optionStrings)
            {
                CheatOptionData cheatData = new CheatOptionData();
                cheatData.value = Integer.valueOf(option.substring(option.length()-4, option.length()));
                cheatData.description = option.substring(0, option.length() - 5);
                optionsList.add(cheatData);
            }
        }
        else
        {
            CheatOptionData cheatData = new CheatOptionData();
            cheatData.value = Integer.valueOf(cheat.code.substring(cheat.code.length()-4, cheat.code.length()));
            optionsList.add(cheatData);
        }
        
        EditCheatDialog editCheatDialogFragment =
            EditCheatDialog.newInstance(getString(stringId), cheat.name, cheat.desc,
                Integer.valueOf(cheat.code.substring(0, 8)), optionsList);
        
        FragmentManager fm = getSupportFragmentManager();
        editCheatDialogFragment.show(fm, STATE_CHEAT_EDIT_DIALOG_FRAGMENT);
    }
    
    @Override
    public void onClick( View v )
    {
        AlertDialog alertDialog;
        
        switch( v.getId() )
        {
            case R.id.imgBtnChtAdd:
                Cheat cheat = new Cheat();
                cheat.name = getString( R.string.cheatEditor_empty );
                cheat.desc = getString( R.string.cheatNotes_none );
                cheat.code = "";
                cheat.option = "";
                userCheats.add( cheat );
                Collections.sort(userCheats);
                cheatListAdapter.notifyDataSetChanged();
                Toast t = Toast.makeText( CheatEditorActivity.this, getString( R.string.cheatEditor_added ), Toast.LENGTH_SHORT );
                t.show();
                break;
            
            case R.id.imgBtnChtSave:
                save( mRomCrc );
                CheatEditorActivity.this.finish();
                break;
                
            case R.id.imgBtnChtInfo:
                StringBuilder message = new StringBuilder();
                message.append( getString( R.string.cheatEditor_readme1 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme2 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme3 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme4 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme5 ) + "\n\n" );
                message.append( getString( R.string.cheatEditor_readme6 ) );
                
                alertDialog = new AlertDialog.Builder( CheatEditorActivity.this ).create();
                alertDialog.setTitle( getString( R.string.cheatEditor_help ) );
                alertDialog.setMessage( message.toString() );
                alertDialog.show();
                break;
        }
    }

    @Override
    // onBackPressed could probably be used
    public boolean onKeyDown( int KeyCode, KeyEvent event )
    {
        if( KeyCode == KeyEvent.KEYCODE_BACK )
        {
            final OnClickListener listener = new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        save( mRomCrc );
                    }
                    CheatEditorActivity.this.finish();
                }
            };            
            Builder builder = new Builder( this );
            builder.setTitle( R.string.cheatEditor_saveConfirm );
            builder.setPositiveButton( android.R.string.yes, listener );
            builder.setNegativeButton( android.R.string.no, listener );
            builder.create().show();
            return true;
        }
        return super.onKeyDown( KeyCode, event );
    }

    private void promptDelete( final int pos )
    {
        final OnClickListener listener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    userCheats.remove( pos );
                    cheatListAdapter.notifyDataSetChanged();
                }
            }
        };            
        Builder builder = new Builder( this );
        builder.setTitle( R.string.cheatEditor_delete );
        builder.setMessage( R.string.cheatEditor_confirm );
        builder.setPositiveButton( android.R.string.yes, listener );
        builder.setNegativeButton( android.R.string.no, listener );
        builder.create().show();
    }

    @Override
    public void onEditComplete(int selectedButton, String name, String comment, int address,
        List<CheatOptionData> options)
    {        
        if( selectedButton == DialogInterface.BUTTON_POSITIVE )
        {
            Cheat cheat = userCheats.get( mSelectedCheat );
            cheat.name = name;
            cheat.desc = comment.replace( '\n', ' ' );
            
            //Only a single cheat present, place the value within the code
            if(options.size() == 1)
            {
                cheat.code = String.format("%08d %04d", address, options.get(0).value);
            }
            else
            {
                cheat.code = address + " ????";
                StringBuilder builder = new StringBuilder();
                for(CheatOptionData data: options)
                {
                    builder.append(String.format("%s %04d\n", data.description, data.value));
                }
                cheat.option = builder.toString();
            }
            
            
            Collections.sort(userCheats);
            cheatListAdapter.notifyDataSetChanged();
        }
    }
}
