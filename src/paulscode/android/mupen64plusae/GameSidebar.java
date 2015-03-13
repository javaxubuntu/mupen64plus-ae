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

import org.mupen64plusae.v3.alpha.R;
import paulscode.android.mupen64plusae.DrawerDrawable;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.content.Context;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.util.AttributeSet;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.app.Activity;
import android.preference.Preference;
import android.util.DisplayMetrics;

public class GameSidebar extends ScrollView
{
    private LinearLayout mLayout;
    private ImageView mInfoArt;
    private LinearLayout mImageLayout;
    private TextView mGameTitle;
    private Context mContext;
    
    public GameSidebar( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        inflater.inflate( R.layout.game_sidebar, this );
        
        mContext = context;
        mLayout = (LinearLayout) findViewById( R.id.layout );
        mInfoArt = (ImageView) findViewById( R.id.imageArt );
        mImageLayout = (LinearLayout) findViewById( R.id.imageLayout );
        mGameTitle = (TextView) findViewById( R.id.gameTitle );
        
        // Have the game cover art scroll at half the speed as the rest of the content
        final ScrollView scroll = this;
        getViewTreeObserver().addOnScrollChangedListener( new OnScrollChangedListener()
        {
            @Override
            public void onScrollChanged()
            {
                int scrollY = scroll.getScrollY();
                mImageLayout.setPadding( 0, scrollY/2, 0, 0 );
            }
        });
    }
    
    public void setImage( BitmapDrawable image )
    {
        if ( image != null )
            mInfoArt.setImageDrawable( image );
        else
            mInfoArt.setImageResource( R.drawable.default_coverart );
    }
    
    public void setTitle( String title )
    {
        mGameTitle.setText( title );
    }
    
    public void clear()
    {
        mLayout.removeAllViews();
    }
    
    public void addHeading( String heading )
    {
        // Perhaps we should just inflate this from XML?
        TextView headingView = new TextView( mContext );
        headingView.setText( heading );
        headingView.setTextSize( TypedValue.COMPLEX_UNIT_SP, 14.0f );
        
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int padding = (int) ( metrics.density * 5 );
        headingView.setPadding( padding, padding, padding, padding );
        mLayout.addView( headingView );
    }
    
    public void addRow( int icon, String title, String summary, Action action )
    {
        addRow( icon, title, summary, action, 0x0, 0 );
    }
    
    public void addRow( int icon, String title, String summary, Action action, int indicator, int indentation )
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view = inflater.inflate( R.layout.list_item_menu, null );
        
        if ( indentation != 0 )
        {
            DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
            
            view.setPadding( (int) ( indentation * 15 * metrics.density ), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom() );
        }
        
        ImageView iconView = (ImageView) view.findViewById( R.id.icon );
        TextView text1 = (TextView) view.findViewById( R.id.text1 );
        TextView text2 = (TextView) view.findViewById( R.id.text2 );
        iconView.setImageResource( icon );
        text1.setText( title );
        text2.setText( summary );
        if ( TextUtils.isEmpty( summary ) )
            text2.setVisibility( View.GONE );
        
        ImageView indicatorView = (ImageView) view.findViewById( R.id.indicator );
        indicatorView.setImageResource( indicator );
        if ( indicator == 0x0 )
            indicatorView.setVisibility( View.GONE );
        
        mLayout.addView( view );
        
        if ( action == null ) return;
        
        // Pass the action to the click listener
        final Action finalAction = action;
        
        view.setFocusable( true );
        view.setBackgroundResource( android.R.drawable.list_selector_background );
        view.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                finalAction.onAction();
            }
        });
    }
    
    public void addInformation( String name, String date, String developer, String publisher, String genre, String players, String esrb, int lastPlayed )
    {
        // Add a section explaining the region and dump information for the ROM
        // http://forums.emulator-zone.com/archive/index.php/t-5533.html
        
        // There's probably some clever regex to do this, but use basic string functions to parse out the dump info
        List<Preference> prefs = new ArrayList<Preference>();
        int index = 0, length = name.length();
        
        while ( index < length )
        {
            int startIndex = length, endIndex = length;
            int paren = name.indexOf( "(", index );
            int bracket = name.indexOf( "[", index );
            if ( paren > -1 && paren < startIndex ) startIndex = paren + 1;
            if ( bracket > -1 && bracket < startIndex ) startIndex = bracket + 1;
            if ( startIndex >= length ) break;
            
            paren = name.indexOf( ")", startIndex );
            bracket = name.indexOf( "]", startIndex );
            if ( paren > -1 && paren < endIndex ) endIndex = paren;
            if ( bracket > -1 && bracket < endIndex ) endIndex = bracket;
            if ( endIndex >= length ) break;
            
            // parse out the tokens between startIndex and endIndex
            String code = name.substring( startIndex, endIndex );
            
            Preference info = new Preference( mContext );
            
            if ( code.length() <= 2 )
            {
                if ( code.startsWith( "a" ) )
                {
                    // a# = alternate
                    info.setTitle( mContext.getString( R.string.infoAlternate_title ) );
                    info.setSummary( mContext.getString( R.string.infoAlternate_summary ) );
                }
                else if ( code.startsWith( "b" ) )
                {
                    // b# = bad dump
                    info.setTitle( mContext.getString( R.string.infoBad_title ) );
                    info.setSummary( mContext.getString( R.string.infoBad_summary ) );
                }
                else if ( code.startsWith( "t" ) )
                {
                    // t# = trained
                    info.setTitle( mContext.getString( R.string.infoTrained_title ) );
                    info.setSummary( mContext.getString( R.string.infoTrained_summary ) );
                }
                else if ( code.startsWith( "f" ) )
                {
                    // f# = fix
                    info.setTitle( mContext.getString( R.string.infoFixed_title ) );
                    info.setSummary( mContext.getString( R.string.infoFixed_summary ) );
                }
                else if ( code.startsWith( "h" ) )
                {
                    // h# = hack
                    info.setTitle( mContext.getString( R.string.infoHack_title ) );
                    info.setSummary( mContext.getString( R.string.infoHack_summary ) );
                }
                else if ( code.startsWith( "o" ) )
                {
                    // o# = overdump
                    info.setTitle( mContext.getString( R.string.infoOverdump_title ) );
                    info.setSummary( mContext.getString( R.string.infoOverdump_summary ) );
                }
                else if ( code.equals( "!" ) )
                {
                    // ! = good dump
                    info.setTitle( mContext.getString( R.string.infoVerified_title ) );
                    info.setSummary( mContext.getString( R.string.infoVerified_summary ) );
                }
                else if ( code.equals( "A" ) )
                {
                    // A = Australia
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoAustralia_title ) );
                }
                else if ( code.equals( "U" ) )
                {
                    // U = USA
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoUSA_title ) );
                }
                else if ( code.equals( "J" ) )
                {
                    // J = Japan
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoJapan_title ) );
                }
                else if ( code.equals( "JU" ) )
                {
                    // JU = Japan and USA
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoJapanUSA_title ) );
                }
                else if ( code.equals( "E" ) )
                {
                    // E = Europe
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoEurope_title ) );
                }
                else if ( code.equals( "G" ) )
                {
                    // G = Germany
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoGermany_title ) );
                }
                else if ( code.equals( "F" ) )
                {
                    // F = France
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoFrance_title ) );
                }
                else if ( code.equals( "S" ) )
                {
                    // S = Spain
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoSpain_title ) );
                }
                else if ( code.equals( "I" ) )
                {
                    // I = Italy
                    info.setTitle( mContext.getString( R.string.infoRegion_title ) );
                    info.setSummary( mContext.getString( R.string.infoItaly_title ) );
                }
                else if ( code.equals( "PD" ) )
                {
                    // PD = public domain
                    //info.setTitle( mContext.getString( R.string.infoPublicDomain_title ) );
                    //info.setSummary( mContext.getString( R.string.infoPublicDomain_summary ) );
                }
                else if ( code.startsWith( "M" ) )
                {
                    // M# = multi-language
                    info.setTitle( mContext.getString( R.string.infoMultilanguage_title ) );
                    info.setSummary( mContext.getString( R.string.infoMultilanguage_summary, code.substring( 1 ) ) );
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
                info.setTitle( mContext.getString( R.string.infoTranslated_title ) );
                info.setSummary( mContext.getString( R.string.infoTranslated_summary ) );
            }
            else if ( code.startsWith( "T-" ) )
            {
                // T-* = translated
                info.setTitle( mContext.getString( R.string.infoTranslated_title ) );
                info.setSummary( mContext.getString( R.string.infoTranslated_summary ) );
            }
            else if ( code.startsWith( "V" ) && code.length() <= 6 )
            {
                // V = version code
                info.setTitle( mContext.getString( R.string.infoVersion_title ) );
                info.setSummary( code.substring(1) );
            }
            else if ( code.startsWith( "PAL" ) )
            {
                // PAL = PAL version
                info.setTitle( mContext.getString( R.string.infoCompatibility_title ) );
                info.setSummary( mContext.getString( R.string.infoPAL_title ) );
            }
            else if ( code.startsWith( "PAL-NTSC" ) )
            {
                // PAL-NTSC = PAL and NTSC compatible
                info.setTitle( mContext.getString( R.string.infoCompatibility_title ) );
                info.setSummary( mContext.getString( R.string.infoPALNTSC_title ) );
            }
            else if ( code.startsWith( "NTSC" ) )
            {
                // NTSC = NTSC version
                info.setTitle( mContext.getString( R.string.infoCompatibility_title ) );
                info.setSummary( mContext.getString( R.string.infoNTSC_title ) );
            }
            else
            {
                // Everything else is listed raw and treated as a hack
                info.setTitle( code );
                info.setSummary( mContext.getString( R.string.infoHack_summary ) );
            }
            
            if ( info != null )
                prefs.add( info );
            
            index = endIndex + 1;
        }
        
        boolean addHeading = ( !TextUtils.isEmpty( developer ) || !TextUtils.isEmpty( publisher ) || !TextUtils.isEmpty( date ) || !TextUtils.isEmpty( genre ) || !TextUtils.isEmpty( players ) || !TextUtils.isEmpty( esrb ) || lastPlayed > 0 );
        
        if ( prefs.size() > 0 || addHeading )
            this.addHeading( mContext.getString( R.string.categoryGameInfo_title ) );
        
        //if ( lastPlayed > 0 )
        //    this.addRow( 0, "Last played", "Dunno", null );
        if ( !TextUtils.isEmpty( developer ) )
            this.addRow( 0, "Developer", developer, null );
        if ( !TextUtils.isEmpty( publisher ) )
            this.addRow( 0, "Publisher", publisher, null );
        if ( !TextUtils.isEmpty( genre ) )
            this.addRow( 0, "Genre", genre, null );
        if ( !TextUtils.isEmpty( date ) )
            this.addRow( 0, "Date", date, null );
        if ( !TextUtils.isEmpty( players ) )
        {
            if ( "1".equals( players ) )
                this.addRow( 0, "Players", players, null );
            else
                this.addRow( 0, "Players", "1 – " + players, null );
        }
        if ( !TextUtils.isEmpty( esrb ) )
            this.addRow( 0, "ESRB Rating", esrb, null );
        
        for ( Preference pref : prefs )
            this.addRow( 0, pref.getTitle().toString(), pref.getSummary().toString(), null );
        
    }
    
    public abstract static class Action
    {
        abstract public void onAction();
    }
}
