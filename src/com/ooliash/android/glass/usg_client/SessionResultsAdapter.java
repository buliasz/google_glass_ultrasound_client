/*
 * Copyright (C) 2018 Bartlomiej Uliasz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ooliash.android.glass.usg_client;

import com.google.android.glass.widget.CardScrollAdapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Generates the cards (using custom layouts, rather than the {@code Card} class) that show the
 * results of the session after it has ended.
 */
public class SessionResultsAdapter extends CardScrollAdapter {

    /** The number of phrases that fit on a single results card. */
    private static final int ROWS_PER_CARD = 5;

    /** The view type for the leftmost summary card. */
    private static final int ITEM_VIEW_TYPE_SUMMARY = 0;

    /** The view type for the cards that show lists of phrases. */
    private static final int ITEM_VIEW_TYPE_CONNECTION = 1;

    /** The number of view types used by this adapter. */
    private static final int NUMBER_OF_VIEW_TYPES = 2;

    /** The layout inflater used to inflate the summary and table layouts. */
    private final LayoutInflater mLayoutInflater;

    /** The resource bundle used to load strings used by the adapter. */
    private final Resources mResources;

    /** The model that will be displayed on the cards. */
    private final ClientModel mModel;

    /**
     * Holds references to the views in the summary card that they can be populated quickly when
     * the card is recycled. This object is set as the root view's tag so that it can be retrieved
     * quickly, which is faster than calling {@code findViewById} every time the view is recycled.
     */
    private class SummaryViewHolder {
        /**
         * The {@code TextView} displaying the "Great job" message.
         */
        public TextView messageView;

        /** The {@code TextView} that contains the summary of the session. */
        public TextView summaryView;
    }

    /**
     * Holds references to the views in a table row so that they can be populated quickly when a
     * table layout is recycled. An array of these is set as the layout's tag.
     */
    private class ConnectionViewHolder {
        /**
         * The {@code ImageView} displayed next to the phrase showing whether the word was guessed
         * correctly or not.
         */
        public ImageView imageView;

        /** The {@code TextView} that contains the phrase. */
        public TextView connectionView;
    }

    /** Initializes the adapter with the specified layout inflater and model. */
    public SessionResultsAdapter(
            LayoutInflater layoutInflater,
            Resources resources,
            ClientModel model) {
        mLayoutInflater = layoutInflater;
        mResources = resources;
        mModel = model;
    }

    @Override
    public int getViewTypeCount() {
        return NUMBER_OF_VIEW_TYPES;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_VIEW_TYPE_SUMMARY;
        }
        return ITEM_VIEW_TYPE_CONNECTION;
    }

    @Override
    public int getPosition(Object item) {
        return ((Integer) item).intValue();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return 7;
    }

    @Override
    public Object getItem(int position) {
        return Integer.valueOf(position);
    }

    /**
     * Returns the summary card at position 0.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // If convertView is not null, then we simply onGestureSwipeRight it to the appropriate updateXXX() method
        // which will access the view holder and modify the existing widgets instead of
        // re-inflating it.
        if (position == 0) {
            View summaryView;
            if (convertView != null) {
                summaryView = convertView;
            } else {
                summaryView = inflateSummaryView(parent);
            }
            updateSummaryView(summaryView);
            return summaryView;
        } else {
            View connectionView;
            if (convertView != null) {
                connectionView = convertView;
            } else {
                connectionView = inflateConnectionView(parent);
            }
            updateConnectionView(connectionView, position);
            return connectionView;
        }
    }

    /**
     * Inflates the summary card that appears in the first position of the card scroller.
     * <p>
     * This method only inflates the views and sets the tag of the view to be a
     * {@link SummaryViewHolder} object. The caller should onGestureSwipeRight this view, or a recycled view if
     * it has one, to {@link #updateSummaryView(View)} in order to populate it with data.
     */
    private View inflateSummaryView(ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.card_results_summary, parent);

        SummaryViewHolder holder = new SummaryViewHolder();
        holder.messageView = (TextView) view.findViewById(R.id.message);
        holder.summaryView = (TextView) view.findViewById(R.id.score_summary);
        view.setTag(holder);

        return view;
    }

    /**
     *
     */
    private void updateSummaryView(View summaryView) {
        SummaryViewHolder holder = (SummaryViewHolder) summaryView.getTag();
        holder.messageView.setText(R.string.great_job);
        holder.summaryView.setText(
                mResources.getString(R.string.time_summary, 0));
    }

    /**
     * Inflates and returns a tabular layout that can hold five phrases.
     * <p>
     * This method only inflates the views and sets the tag of the root view to be an array of
     * {@link ConnectionViewHolder} objects. The caller should onGestureSwipeRight this view, or a recycled view if
     * it has one, to {@link #updateConnectionView(View,int)} in order to populate it with data.
     */
    private View inflateConnectionView(ViewGroup parent) {
        TableLayout table =
                (TableLayout) mLayoutInflater.inflate(R.layout.card_results_connection, parent);

        // Add five phrases to the card, or fewer if it is the last card and the total number of
        // phrases is not an even multiple of ROWS_PER_CARD.
        ConnectionViewHolder[] holders = new ConnectionViewHolder[ROWS_PER_CARD];
        for (int i = 0; i < ROWS_PER_CARD; i++) {
            TableRow row = (TableRow) mLayoutInflater.inflate(R.layout.table_row_result, null);
            table.addView(row);

            ConnectionViewHolder holder = new ConnectionViewHolder();
            holder.imageView = (ImageView) row.findViewById(R.id.image);
            holder.connectionView = (TextView) row.findViewById(R.id.phrase);
            holders[i] = holder;
        }
        table.setTag(holders);
        return table;
    }

    /**
     * Populates the specified phrase list view (a {@code TableLayout}) with phrases from the
     * model, assuming that the view will be displayed at the specified position in the card
     * scroller.
     */
    private void updateConnectionView(View phraseListView, int position) {
        // Compute the indices of the first phrase that should appear on this card.
        int start = (position - 1) * ROWS_PER_CARD;

        ConnectionViewHolder[] holders = (ConnectionViewHolder[]) phraseListView.getTag();
        for (int i = 0; i < ROWS_PER_CARD; i++) {
            ConnectionViewHolder holder = holders[i];

            int summaryIndex = i + start;
            if (summaryIndex < 7) {
                holder.connectionView.setText(String.valueOf(summaryIndex));

                // Update the image and text color depending on whether the phrase was guessed
                // correctly.
                holder.connectionView.setTextColor(Color.WHITE);
                holder.imageView.setImageResource(R.drawable.ic_circle_green_30);
//                holder.connectionView.setTextColor(Color.GRAY);
//                holder.imageView.setImageResource(R.drawable.ic_circle_red_30);

                holder.imageView.setVisibility(View.VISIBLE);
            } else {
                // Make sure the views are clear/hidden if there is no phrase at this index. This
                // might happen if the number of phrases in the model is not an even multiple of
                // ROWS_PER_CARD.
                holder.connectionView.setText("");
                holder.imageView.setVisibility(View.INVISIBLE);
            }
        }
    }
}
