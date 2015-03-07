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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.content.Context;
import android.util.AttributeSet;

public class GalleryView extends RecyclerView
{
    private OnLayoutChangeListener mListener;
    
    public GalleryView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        mListener = null;
    }
    
    @Override
    protected void onLayout (boolean changed, int l, int t, int r, int b)
    {
        if ( mListener != null )
            mListener.onLayoutChange( this, l, t, r, b );
        super.onLayout( changed, l, t, r, b);
    }
    
    public void setOnLayoutChangeListener( OnLayoutChangeListener listener )
    {
        mListener = listener;
    }
    
    public OnLayoutChangeListener getOnLayoutChangeListener()
    {
        return mListener;
    }
    
    public static class OnLayoutChangeListener
    {
        public void onLayoutChange( View view, int left, int top, int right, int bottom )
        {
        }
    }
}
