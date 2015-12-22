package paulscode.android.mupen64plusae.dialog;

import java.util.ArrayList;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class EditCheatDialog extends DialogFragment
{
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_NAME = "STATE_NAME";
    private static final String STATE_COMMENT = "STATE_COMMENT";
    private static final String STATE_ADDRESS = "STATE_ADDRESS";
    private static final String STATE_NUM_ITEMS = "STATE_NUM_ITEMS";
    private static final String STATE_ITEMS_DESC = "STATE_ITEMS_DESC";
    private static final String STATE_ITEMS_OPTION = "STATE_ITEMS_OPTION";

    private String mName = null;
    private String mComment = null;
    private int mAddress = 0;
    private List<CheatOptionData> mItems = null;

    private View mDialogView = null;
    
    static public final class CheatOptionData
    {
        public String description;
        public int value;
    }

    public interface OnEditCompleteListener
    {
        /**
         * Called after cheat editing is complete
         * @param selectedButton The selected button.
         * @param name chosen cheat title
         * @param comment chosen cheat comment
         * @param address cheat address
         * @param options cheat options
         */
        public void onEditComplete(int selectedButton, String name, String comment,
            int address, List<CheatOptionData> options);
    }
    
    

    public static EditCheatDialog newInstance(String title, String name, String comment,
        int address, List<CheatOptionData> options)
    {
        EditCheatDialog frag = new EditCheatDialog();
        Bundle args = new Bundle();
        args.putString(STATE_TITLE, title);
        args.putString(STATE_NAME, name);
        args.putString(STATE_COMMENT, comment);
        args.putInt(STATE_ADDRESS, address);
        
        args.putInt(STATE_NUM_ITEMS, options.size());
        for (int index = 0; index < options.size(); ++index)
        {
            CheatOptionData seq = options.get(index);
            args.putString(STATE_ITEMS_DESC + index, seq.description);
            args.putInt(STATE_ITEMS_OPTION + index, seq.value);
        }

        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        final String title = getArguments().getString(STATE_TITLE);
        mName = getArguments().getString(STATE_NAME);
        mComment = getArguments().getString(STATE_COMMENT);
        mAddress = getArguments().getInt(STATE_ADDRESS);
        
        final int numItems = getArguments().getInt(STATE_NUM_ITEMS);

        mItems = new ArrayList<CheatOptionData>();

        //Fill the values
        for (int index = 0; index < numItems; ++index)
        {
            String desc = getArguments().getString(STATE_ITEMS_DESC + index);
            int option = getArguments().getInt(STATE_ITEMS_OPTION + index);
            
            CheatOptionData data = new CheatOptionData();
            data.description = desc;
            data.value = option;
            mItems.add(data);
        }

        mDialogView = View.inflate(getActivity(), R.layout.cheat_edit_dialog, null);
        
        final EditText editName = (EditText) mDialogView.findViewById(R.id.textCheatTitle);
        final EditText editComment = (EditText) mDialogView.findViewById(R.id.textCheatNotes);
        final EditText editAddress = (EditText) mDialogView.findViewById(R.id.textCheatAddress);
        final EditText editValue = (EditText) mDialogView.findViewById(R.id.textCheatMainValue);
        final Button addOptionButton = (Button) mDialogView.findViewById(R.id.addMoreCheatOptionsButton);
        final LinearLayout optionsLayoutHolder = (LinearLayout) mDialogView.findViewById(R.id.linearLayoutCheatOptionsHolder);
        
        final String invalidValue = "N/A";
        
        //Add an option
        addOptionButton.setOnClickListener(new View.OnClickListener ()
        {
            @Override
            public void onClick(View v)
            {
                final LinearLayout optionsLayoutHolder = (LinearLayout) mDialogView.findViewById(R.id.linearLayoutCheatOptionsHolder);
                final View optionLayout = View.inflate( getActivity(), R.layout.cheat_edit_dialog_options, null );
                optionsLayoutHolder.addView(optionLayout);
                
                //Disable this field
                editValue.setEnabled(false);
                editValue.setText(invalidValue);
                
                ImageButton cheatOptionDelete = (ImageButton) optionLayout.findViewById(R.id.removeCheatOptionButton);
                cheatOptionDelete.setOnClickListener(new View.OnClickListener ()
                {
                    //Remove an option
                    @Override
                    public void onClick(View v)
                    {
                        optionsLayoutHolder.removeView(optionLayout);
                        
                        if(optionsLayoutHolder.getChildCount() == 0)
                        {
                            editValue.setEnabled(true);
                            editValue.setText("");
                        }
                    }
                });
            }
        });

        editName.setText(mName);
        editComment.setText(mComment);
        editAddress.setText(Integer.toString(mAddress));
        
        //Single value, no options, so populate single value
        if(mItems.size() == 1)
        {
            editValue.setText(Integer.toString(mItems.get(0).value));
        }
        //This is a set of options, populate all the options
        else
        {
            //Disable this field
            editValue.setEnabled(false);
            editValue.setText(invalidValue);
            
            for(CheatOptionData data : mItems)
            {
                final View optionLayout = View.inflate( getActivity(), R.layout.cheat_edit_dialog_options, null );
                EditText cheatValueText = (EditText) optionLayout.findViewById(R.id.textCheatValue);
                EditText cheatValueDescription = (EditText) optionLayout.findViewById(R.id.textCheatValueDescription);
                ImageButton cheatOptionDelete = (ImageButton) optionLayout.findViewById(R.id.removeCheatOptionButton);
                
                cheatValueText.setText(Integer.toString(data.value));
                cheatValueDescription.setText(data.description);
                cheatOptionDelete.setOnClickListener(new View.OnClickListener ()
                {
                    //Remove an option
                    @Override
                    public void onClick(View v)
                    {
                        optionsLayoutHolder.removeView(optionLayout);
                        
                        if(optionsLayoutHolder.getChildCount() == 0)
                        {
                            editValue.setEnabled(true);
                            editValue.setText("");
                        }
                    }
                });
                
                optionsLayoutHolder.addView(optionLayout);
            }
        }

        //Time to create the dialog
        Builder builder = new Builder(getActivity());
        builder.setTitle(title);

        // Create listener for OK/cancel button clicks
        OnClickListener clickListener = new OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //Populate the options and submit to activity
                if (getActivity() instanceof OnEditCompleteListener)
                {
                    List<CheatOptionData> options = new ArrayList<CheatOptionData>();
                    
                    //Single value
                    if(optionsLayoutHolder.getChildCount() == 0)
                    {
                        CheatOptionData data = new CheatOptionData();
                        String valueString = editValue.getText().toString();
                        
                        if(!valueString.isEmpty() && !valueString.equals(invalidValue))
                        {
                            data.value = Integer.valueOf(editValue.getText().toString());
                            data.description = "";
                            options.add(data);
                        }
                    }
                    //Many options
                    else
                    {
                        for(int index = 0; index < optionsLayoutHolder.getChildCount(); ++index)
                        {
                            View child = optionsLayoutHolder.getChildAt(index);
                            EditText cheatValueText = (EditText) child.findViewById(R.id.textCheatValue);
                            EditText cheatValueDescription = (EditText) child.findViewById(R.id.textCheatValueDescription);
                            
                            CheatOptionData data = new CheatOptionData();
                            String valueString = cheatValueText.getText().toString();
                            if(!valueString.isEmpty())
                            {
                                data.value = Integer.valueOf(cheatValueText.getText().toString());
                                data.description = cheatValueDescription.getText().toString();
                                options.add(data);
                            }
                        }
                    }

                    ((OnEditCompleteListener) getActivity()).onEditComplete( which,
                        editName.getText().toString(), editComment.getText().toString(),
                        Integer.valueOf(editAddress.getText().toString()), options);
                }
                else
                {
                    Log.e("EditCheatDialog", "Activity doesn't implement OnEditCompleteListener");
                }
            }
        };

        builder.setView(mDialogView);
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, clickListener);

        return builder.create();
        
        //TODO: Add field verification
    }

    @Override
    public void onDestroyView()
    {
        // This is needed because of this:
        // https://code.google.com/p/android/issues/detail?id=17423

        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }
}