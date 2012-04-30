/*
 * Copyright 2011 Gary Lo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.crossconnect.activity.main;

import java.util.List;

import net.sword.engine.sword.SwordContentFacade;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.crossconnect.activity.MainActivity;
import com.crossconnect.model.BibleText;
import com.crossconnect.model.Note;
import com.crossconnect.model.SwordBibleText;
import com.crossconnect.views.BibleTextScrollView;
import com.crossconnect.views.BibleTextViewImpl;
import com.crossconnect.views.R;
import com.crossconnect.views.WindowsActivity;

public class BibleTextFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Note>> {

    private static final String TAG = "BibleTextFragment";

    private BibleTextViewImpl bibleTextView;
    
    private BibleTextScrollView bibleScrollView;
    
    private Handler updateTitleHandler;
    
    private TextView headTitleTextView;
    
    private static final int LOADING_DIALOG = 1;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.bible_text_column, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // Find Views After Setup
        headTitleTextView = (TextView) getActivity().findViewById(R.id.header_title);
        headTitleTextView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/" + "Ubuntu-R.ttf"),Typeface.NORMAL);

        
        bibleScrollView = (BibleTextScrollView) getActivity().findViewById(R.id.bibleScrollView);
        
        
        //Handler created on UI thread to handle updates to title
        updateTitleHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                
                final String verseText = msg.getData().getString("Verse_Text");
                        headTitleTextView.setText(bibleTextView.getBibleText().getReferenceBookChapter() + verseText);
            }
            
        };
        bibleScrollView.setTitleHandler(updateTitleHandler);
        
        bibleTextView = (BibleTextViewImpl) getActivity().findViewById(R.id.bible_text);
        bibleTextView.setBibleSrcollView(bibleScrollView);
        
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Now, we're going to check for the Text size variable and set the text
        // size

        bibleTextView
            .setTextSize(Float.parseFloat(appPreferences.getString(getActivity().getString(R.string.text_size_key), "18")));

        
        
        
        //Previous chapter button
        ((Button) getActivity().findViewById(R.id.prev_chapter_button)).setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                //Start thread to get verse
                getActivity().showDialog(LOADING_DIALOG);
                new SwordVerseTask().execute(bibleTextView.getBibleText().getPrevChapterRef());
            }
            
        });

        
        //Next chapter button
        ((Button) getActivity().findViewById(R.id.next_chapter_button)).setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                //Start thread to get verse
                getActivity().showDialog(LOADING_DIALOG);
                new SwordVerseTask().execute(bibleTextView.getBibleText().getNextChapterRef());
            }
            
        });
        
        //Go to windows view
        ((ImageButton) getActivity().findViewById(R.id.menu_button_windows)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WindowsActivity.class);
                

                //Set the verseNumber to what is currently being viewed
                int verseNumber = bibleTextView.yToVerse(bibleScrollView.getScrollY(), (getActivity().findViewById(R.id.prev_chapter_button)).getHeight());
                
                
                Log.i("Main", "Set BibleTextView verse number " + verseNumber);
                
                bibleTextView.getBibleText().setVerse(verseNumber);
                
                Log.i("Main", "BibleTextView sent " + bibleTextView.getBibleText().getReferenceBookChapterVerse());

                //TODO: add string preview  from existing rendered text probably just do a substring of boundaries
                intent.putExtra("BibleText", bibleTextView.getBibleText());
                intent.putExtra("WindowId", ((MainActivity) getActivity()).windowId);
                startActivityForResult(intent, MainActivity.WINDOW_SELECT_CODE);    
//                overridePendingTransition(R.anim.zoom_enter, 0);

            }
        });        


        
        //Load last opened verse
        String currentBook = getActivity().getSharedPreferences("APP SETTINGS", Context.MODE_PRIVATE).getString("current_book", "Philipiians");
        String currentChapter = getActivity().getSharedPreferences("APP SETTINGS", Context.MODE_PRIVATE).getString("current_chapter", "1");
        String currentVerse = getActivity().getSharedPreferences("APP SETTINGS", Context.MODE_PRIVATE).getString("current_verse", "1");
        String currentTranslation = getActivity().getSharedPreferences("APP SETTINGS", Context.MODE_PRIVATE).getString("current_translation", "ESV");
        //TODO:make it not assume ESV is installed

        new SwordInitTask().execute(currentBook, currentChapter, currentVerse, currentTranslation);



//        // Give some text to display if there is no data. In a real
//        // application this would come from a resource.
//        setEmptyText("No applications");
//
//        // We have a menu item to show in action bar.
//        setHasOptionsMenu(true);
//
//        // Only have one choice
//        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//
//        // Create an empty adapter we will use to display the loaded data.
//        mAdapter = new AppListAdapter(getActivity());
//        setListAdapter(mAdapter);
//
//        // Start out with a progress indicator.
//        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }
    
    /**
     * Thread to get verse data  - overrides existing boundaries and text of BibleText
     * @author garylo
     *
     */
    private class SwordVerseTask extends AsyncTask<BibleText,String,BibleText> {

          @Override
          protected BibleText doInBackground(BibleText... params) {
              BibleText bibleText = params[0];
              SwordContentFacade.getInstance().injectChapterFromJsword(bibleText); 
              return bibleText;
          }
      
          @Override
          protected void onPostExecute(BibleText bibleText){
              ((MainActivity) getActivity()).updateFragments(bibleText);
          }
          
    }
    
    /**
     * Thread to initilise Sword without creating a BibleText
     * @author garylo
     *
     */
    private class SwordInitTask extends AsyncTask<String,String,Void> {


          String bibleBook;
          int chapter;
          int verse;
          String translation;

        
        @Override
          protected Void doInBackground(String... params) {
              bibleBook = params[0];
              chapter = Integer.valueOf(params[1]);
              verse  = Integer.valueOf(params[2]);
              translation = params[3];
              
              //Initialise the Sword
              SwordContentFacade.getInstance();
              return null;
          }
      
          @Override
          protected void onPostExecute(Void blah){
              BibleText bibleText = new SwordBibleText(bibleBook, chapter, verse, translation);
              SwordContentFacade.getInstance().injectChapterFromJsword(bibleText); 
              ((MainActivity) getActivity()).updateFragments(bibleText);

//            new SwordVerseTask().execute(new SwordBibleText(bibleBook, chapter, translation));
          }
    }
    
    
    /**
     * Update bible text with new BibleText TODO: translation is current static
     */
    public void updateBibleTextView(final BibleText bibleText) {
        bibleTextView.setBibleText(bibleText);
        headTitleTextView.setText(bibleText.getReferenceBookChapterVerse());
        
//        String note = notesService.getNotes(bibleText.getKey());
//        if (note != null) {
//            notesEditText.setText(note);
//        } else {
//            notesEditText.setText("");
//        }
//        notesEditText.lock();

        
        //Scroll to the required  verse after reload, will be verse 1 if new chapter
        bibleTextView.post(new Runnable(){
            public void run(){
                try{
                    getActivity().dismissDialog(LOADING_DIALOG);
                } catch (Exception e) {
                    
                }
//                sermonBtn.requestFocus();
                
                //only scroll to the verse if this thing is even visible
                if (getActivity().findViewById(R.id.prev_chapter_button) != null && bibleTextView.isShown()) {
                    bibleTextView.scrollToVerse(bibleText.getVerse(), getActivity().findViewById(R.id.prev_chapter_button).getHeight());
                }
                
                //do not give the editbox focus automatically when activity starts
                bibleTextView.clearFocus();
//                grabFocus.requestFocus();

            };
        });
        
        //Save the current passage so it can be reloaded //TODO: is it needed here since onRestore has it?
        saveCurrentPassage(bibleText);
    }

    private void saveCurrentPassage(BibleText bibleText) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getActivity().getSharedPreferences("APP SETTINGS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("current_book", bibleText.getBook());
        editor.putString("current_chapter", String.valueOf(bibleText.getChapter()));

        //TODO: at the moment this gets called even when it is not visible and causes problems so hacky solution to just try catch it
        try {
            //Set the verseNumber to what is currently being viewed
            int verseNumber = bibleTextView.yToVerse(bibleScrollView.getScrollY(), getActivity().findViewById(R.id.prev_chapter_button).getHeight());         
            editor.putString("current_verse", String.valueOf(verseNumber));
        } catch (Exception e) {
        }
        
        //TODO: verse restore doens't yet work
        editor.putString("current_translation", bibleText.getTranslation().getInitials());

        
        // Commit the edits!
        editor.commit();
    }

    @Override
    public Loader<List<Note>> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCreateLoader()");
        return null;
//        return new AppListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Note>> loader, List<Note> data) {
        Log.i(TAG, "onLoadFinished");
    }

    @Override
    public void onLoaderReset(Loader<List<Note>> loader) {
        // Clear the data in the adapter.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy losing DB");
    }
}