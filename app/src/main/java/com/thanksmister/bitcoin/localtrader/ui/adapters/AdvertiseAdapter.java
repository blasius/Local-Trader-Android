/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.Method;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.Collections;
import java.util.List;

public class AdvertiseAdapter extends BaseAdapter {
    private List<Advertisement> data = Collections.emptyList();
    private List<Method> methods = Collections.emptyList();
    private Context context;
    private final LayoutInflater inflater;

    public AdvertiseAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        if (data == null) return 0;
        return data.size();
    }

    @Override
    public Advertisement getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void replaceWith(List<Advertisement> data, List<Method> methods) {
        this.methods = methods;
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(R.layout.adapter_advertise_layout, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        Advertisement advertisement = getItem(position);

        if (TradeUtils.Companion.isOnlineTrade(advertisement)) { // online trade
            String paymentMethod = TradeUtils.Companion.getPaymentMethod(context, advertisement, methods);
            holder.tradLocation.setText(paymentMethod);
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            String units_prefs = preferences.getString(context.getString(R.string.pref_key_distance), "0");
            String unit = (units_prefs.equals("0") ? context.getString(R.string.list_unit_km) : context.getString(R.string.list_unit_mi));
            String distance = (units_prefs.equals("0")) ? Conversions.formatDecimalToString(Doubles.convertToDouble(advertisement.getDistance()), Conversions.TWO_DECIMALS) : Conversions.kilometersToMiles(Doubles.convertToDouble(advertisement.getDistance()));
            holder.tradLocation.setText(distance + " " + unit + " → " + advertisement.getLocation());
        }

        if (advertisement.isATM()) {
            holder.tradePrice.setText("ATM");
        } else {
            holder.tradePrice.setText(context.getString(R.string.trade_price, advertisement.getTempPrice(), advertisement.getCurrency()));
        }

        holder.traderName.setText(advertisement.getProfile().getUsername());
        holder.tradeFeedback.setText(advertisement.getProfile().getFeedbackScore());
        holder.tradeCount.setText(advertisement.getProfile().getTradeCount());

        /*if(editAdvertisement.isATM()) {
            holder.tradeLimit.setText("");
        } else if(editAdvertisement.min_amount == null) {
            holder.tradeLimit.setText("");
        } else if(editAdvertisement.max_amount == null) {
            holder.tradeLimit.setText(context.getString(R.string.trade_limit_min, editAdvertisement.min_amount, editAdvertisement.currency));
        } else { // no maximum set
            holder.tradeLimit.setText(context.getString(R.string.trade_limit_short, editAdvertisement.min_amount, editAdvertisement.max_amount));
        }*/

        if (advertisement.isATM()) {

            holder.tradeLimit.setText("");

        } else {
            if (advertisement.getMaxAmount() != null && advertisement.getMinAmount() != null) {
                holder.tradeLimit.setText(context.getString(R.string.trade_limit, advertisement.getMinAmount(), advertisement.getMaxAmount() , advertisement.getCurrency()));
            }

            if (advertisement.getMaxAmount()  == null && advertisement.getMinAmount() != null) {
                holder.tradeLimit.setText(context.getString(R.string.trade_limit_min, advertisement.getMinAmount(), advertisement.getCurrency()));
            }

            if (advertisement.getMaxAmountAvailable() != null && advertisement.getMinAmount() != null) { // no maximum set
                holder.tradeLimit.setText(context.getString(R.string.trade_limit, advertisement.getMinAmount(), advertisement.getMaxAmountAvailable(), advertisement.getCurrency()));
            } else if (advertisement.getMaxAmountAvailable() != null) {
                holder.tradeLimit.setText(context.getString(R.string.trade_limit_max, advertisement.getMaxAmountAvailable(), advertisement.getCurrency()));
            }
        }

        holder.lastSeenIcon.setBackgroundResource(TradeUtils.Companion.determineLastSeenIcon(advertisement.getProfile().getLastOnline()));

        return view;
    }

    static class ViewHolder {
        View row;
        TextView tradePrice;
        TextView traderName;
        TextView tradeLimit;
        TextView tradeFeedback;
        TextView tradeCount;
        TextView tradLocation;
        View lastSeenIcon;

        public ViewHolder(View view) {
        }
    }
}


