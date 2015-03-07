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

import android.widget.ListView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.BaseExpandableListAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Context;
import android.view.LayoutInflater;
import java.util.Arrays;
import android.view.ViewGroup;
import android.support.v7.internal.view.menu.MenuBuilder;

import android.app.Activity;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.util.AttributeSet;

/* ExpandableListView which stores its data set as a Menu hierarchy */

public class MenuListView extends ExpandableListView
{
    private MenuListAdapter mAdapter;
    private OnClickListener mListener;
    private Menu mListData;
    
    public MenuListView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        mAdapter = null;
        mListener = null;
        mListData = null;
    }
    
    public void setMenuResource( int menuResource )
    {
        Context context = getContext();
        Menu menu = new MenuBuilder( context );
        Activity activity = (Activity) context;
        activity.getMenuInflater().inflate( menuResource, menu );
        setMenu( menu );
    }
    
    public void setMenu( Menu menu )
    {
        Context context = getContext();
        mListData = menu;
        mAdapter = new MenuListAdapter( this, menu );
        setAdapter( mAdapter );
        setGroupIndicator(null);
        setChoiceMode( ListView.CHOICE_MODE_SINGLE );
        
        // In case we want to add custom expand/collapse views to the groups
        setOnGroupExpandListener( new OnGroupExpandListener()
        {
            @Override
            public void onGroupExpand( int groupPosition )
            {
                
            }
        });
        
        setOnGroupCollapseListener( new OnGroupCollapseListener()
        {
            @Override
            public void onGroupCollapse( int groupPosition )
            {
                
            }
        });
        
        setOnGroupClickListener( new OnGroupClickListener()
        {
            @Override
            public boolean onGroupClick( ExpandableListView parent, View view, int groupPosition, long itemId )
            {
                MenuItem menuItem = mListData.getItem( groupPosition );
                SubMenu submenu = menuItem.getSubMenu();
                if ( submenu == null )
                {
                    if ( mListener != null )
                        mListener.onClick( menuItem );
                }
                return false;
            }
        });
        
        setOnChildClickListener(new OnChildClickListener()
        {
            @Override
            public boolean onChildClick( ExpandableListView parent, View view, int groupPosition, int childPosition, long itemId )
            {
                MenuItem menuItem = mListData.getItem( groupPosition ).getSubMenu().getItem( childPosition );
                if ( mListener != null )
                    mListener.onClick( menuItem );
                return false;
            }
        });
    }
    
    public Menu getMenu()
    {
        return mListData;
    }
    
    public MenuListAdapter getMenuListAdapter()
    {
        return mAdapter;
    }
    
    public void reload()
    {
        mAdapter.notifyDataSetChanged();
    }
    
    public void setOnClickListener( OnClickListener listener )
    {
        mListener = listener;
    }
    
    public OnClickListener getOnClickListener()
    {
        return mListener;
    }
    
    public static class MenuListAdapter extends BaseExpandableListAdapter
    {
        private MenuListView mListView;
        private Menu mListData;
        
        public MenuListAdapter( MenuListView listView, Menu listData )
        {
            mListView = listView;
            mListData = listData;
        }
        
        @Override
        public boolean isChildSelectable( int groupPosition, int childPosition )
        {
            return true;
        }
        
        @Override
        public MenuItem getChild( int groupPosition, int childPosition )
        {
            return getGroup( groupPosition ).getSubMenu().getItem( childPosition );
        }
        
        @Override
        public long getChildId( int groupPosition, int childPosition )
        {
            return getChild( groupPosition, childPosition ).getItemId();
        }
        
        @Override
        public int getChildrenCount( int groupPosition )
        {
            SubMenu submenu = mListData.getItem( groupPosition ).getSubMenu();
            return ( submenu != null ) ? submenu.size() : 0;
        }
        
        @Override
        public View getChildView( int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = (LayoutInflater) mListView.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( R.layout.list_item_two_text_icon, null );
            
            MenuItem item = getChild( groupPosition, childPosition );
            if( item != null )
            {
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                text1.setText( item.getTitle() );
                text2.setVisibility( View.GONE );
                icon.setImageDrawable( item.getIcon() );
                
                if ( item.isChecked() )
                    view.setBackgroundColor( 0x44FFFFFF );
                else
                    view.setBackgroundColor( 0x0 );
            }
            return view;
        }
        
        @Override
        public MenuItem getGroup( int groupPosition )
        {
            return mListData.getItem( groupPosition );
        }
        
        @Override
        public long getGroupId( int groupPosition )
        {
            return getGroup( groupPosition ).getItemId();
        }
        
        @Override
        public int getGroupCount()
        {
            return mListData.size();
        }
        
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mListView.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( R.layout.list_item_two_text_icon, null );
            
            MenuItem item = getGroup( groupPosition );
            if( item != null )
            {
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                text1.setText( item.getTitle() );
                text2.setVisibility( View.GONE );
                icon.setImageDrawable( item.getIcon() );
                
                if ( item.isChecked() )
                    view.setBackgroundColor( 0x44FFFFFF );
                else
                    view.setBackgroundColor( 0x0 );
            }
            return view;
        }
        
        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
    
    public static class OnClickListener
    {
        OnClickListener()
        {
        }
        
        public void onClick( MenuItem menuItem )
        {
        }
    }
}
