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
import java.util.List;
import java.util.Comparator;

import org.mupen64plusae.v3.alpha.R;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.util.TypedValue;

public class GalleryItem
{
    public final String md5;
    public final String goodName;
    public final String artPath;
    public final int lastPlayed;
    public final File romFile;
    public final Context context;
    public final boolean isHeading;
    public BitmapDrawable artBitmap;
    
    public GalleryItem( Context context, String md5, String goodName, String romPath, String artPath, int lastPlayed )
    {
        this.md5 = md5;
        this.goodName = goodName;
        this.context = context;
        this.artPath = artPath;
        this.artBitmap = null;
        this.lastPlayed = lastPlayed;
        this.isHeading = false;
        
        romFile = TextUtils.isEmpty( romPath ) ? null : new File( romPath );
    }
    
    public GalleryItem( Context context, String headingName )
    {
        this.goodName = headingName;
        this.context = context;
        this.isHeading = true;
        this.md5 = null;
        this.artPath = null;
        this.artBitmap = null;
        this.lastPlayed = 0;
        romFile = null;
    }
    
    public void loadBitmap()
    {
        if ( artBitmap != null )
            return;
        
        if( !TextUtils.isEmpty( artPath ) && new File( artPath ).exists() )
            artBitmap = new BitmapDrawable( context.getResources(), artPath );
    }
    
    public void clearBitmap()
    {
        artBitmap = null;
    }
    
    @Override
    public String toString()
    {
        if( !TextUtils.isEmpty( goodName ) )
            return goodName;
        else if( romFile != null && !TextUtils.isEmpty( romFile.getName() ) )
            return romFile.getName();
        else
            return "unknown file";
    }
    
    public static class NameComparator implements Comparator<GalleryItem>
    {
        @Override
        public int compare( GalleryItem item1, GalleryItem item2 )
        {
            return item1.toString().compareToIgnoreCase( item2.toString() );
        }
    }
    
    public static class RecentlyPlayedComparator implements Comparator<GalleryItem>
    {
        @Override
        public int compare( GalleryItem item1, GalleryItem item2 )
        {
            return item2.lastPlayed - item1.lastPlayed;
        }
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener, OnLongClickListener
    {
        public GalleryItem item;
        private Context mContext;
        
        public ViewHolder( Context context, View view )
        {
            super( view );
            mContext = context;
            
            // Tapping the view itself will go directly to the game
            view.setOnClickListener( this );
            
            // Long-pressing the view will trigger a contextual menu
            view.setOnLongClickListener( this );
            
            // Tapping the dotsView will trigger a contextual menu
            ImageView dotsView = (ImageView) view.findViewById( R.id.dots );
            if ( dotsView != null )
            {
                dotsView.setOnClickListener( new OnClickListener()
                {
                    @Override
                    public void onClick( View view )
                    {
                        showContextualMenu( view );
                    }
                });
            }
        }
        
        public void showContextualMenu( View view )
        {
            final GalleryActivity galleryActivity = (GalleryActivity) mContext;
            PopupMenu popupMenu = new PopupMenu( mContext, view );
            popupMenu.setOnMenuItemClickListener( new OnMenuItemClickListener()
            {
                public boolean onMenuItemClick( MenuItem menuItem )
                {
                    return galleryActivity.onGalleryItemMenuSelected( item, menuItem );
                }
            });
            
            if ( galleryActivity.onGalleryItemCreateMenu( item, popupMenu.getMenu() ) )
                popupMenu.show();
        }
        
        @Override
        public String toString() {
            return item.toString();
        }
        
        @Override
        public void onClick( View view )
        {
            if ( mContext instanceof GalleryActivity )
            {
                GalleryActivity activity = (GalleryActivity) mContext;
                activity.onGalleryItemClick( item );
            }
        }
        
        @Override
        public boolean onLongClick( View view )
        {
            showContextualMenu( view );
            return true;
        }
    }
    
    public static class Adapter extends RecyclerView.Adapter<ViewHolder>
    {
        private final Context mContext;
        private final List<GalleryItem> mObjects;
        
        public Adapter( Context context, List<GalleryItem> objects )
        {
            mContext = context;
            mObjects = objects;
        }
        
        @Override
        public int getItemCount()
        {
            return mObjects.size();
        }
        
        @Override
        public long getItemId( int position )
        {
            return 0;
        }
        
        @Override
        public int getItemViewType( int position )
        {
            return mObjects.get( position ).isHeading ? 1 : 0;
        }
        
        public void onBindViewHolder( ViewHolder holder, int position )
        {
            // Clear the now-offscreen bitmap to conserve memory
            if ( holder.item != null )
                holder.item.clearBitmap();
            
            // Called by RecyclerView to display the data at the specified position.
            View view = holder.itemView;
            GalleryItem item = mObjects.get( position );
            holder.item = item;
            
            if( item != null )
            {
                ImageView artView = (ImageView) view.findViewById( R.id.imageArt );
                ImageView dotsView = (ImageView) view.findViewById( R.id.dots );
                TextView tv1 = (TextView) view.findViewById( R.id.text1 );
                tv1.setText( item.toString() );
                
                LinearLayout linearLayout = (LinearLayout) view.findViewById( R.id.galleryItem );
                GalleryActivity activity = (GalleryActivity) item.context;
                
                if ( item.isHeading )
                {
                    view.setClickable( false );
                    linearLayout.setPadding( 0, 0, 0, 0 );
                    tv1.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 18.0f );
                    dotsView.setVisibility( View.GONE );
                    artView.setVisibility( View.GONE );
                }
                else
                {
                    view.setClickable( true );
                    linearLayout.setPadding( activity.galleryHalfSpacing, activity.galleryHalfSpacing, activity.galleryHalfSpacing, activity.galleryHalfSpacing );
                    tv1.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 13.0f );
                    dotsView.setVisibility( View.VISIBLE );
                    artView.setVisibility( View.VISIBLE );
                    
                    item.loadBitmap();
                    if( item.artBitmap != null )
                        artView.setImageDrawable( item.artBitmap );
                    else
                        artView.setImageResource( R.drawable.default_coverart );
                    
                    artView.getLayoutParams().width = activity.galleryWidth;
                    artView.getLayoutParams().height = (int) ( activity.galleryWidth / activity.galleryAspectRatio );
                    
                    RelativeLayout layout = (RelativeLayout) view.findViewById( R.id.info );
                    layout.getLayoutParams().width = activity.galleryWidth;
                }
            }
        }
        
        public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType )
        {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = inflater.inflate( R.layout.gallery_item_adapter, parent, false );
            return new ViewHolder( mContext, view );
        }
    }
}
