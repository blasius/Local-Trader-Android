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

package com.thanksmister.bitcoin.localtrader.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement
import com.thanksmister.bitcoin.localtrader.network.api.model.Method
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.utils.Dates
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils

class AdvertisementsAdapter(private val context: Context, private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<AdvertisementsAdapter.ViewHolder>() {

    private var items = emptyList<Advertisement>()
    private var methods = emptyList<Method>()

    interface OnItemClickListener {
        fun onSearchButtonClicked()
        fun onAdvertiseButtonClicked()
    }

    fun replaceWith(data: List<Advertisement>, methods: List<Method>) {
        this.items = data
        this.methods = methods
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvertisementsAdapter.ViewHolder {
        // create a new view
        val itemLayoutView = LayoutInflater.from(context).inflate(viewType, parent, false)
        if (viewType == TYPE_ITEM) {
            return AdvertisementViewHolder(itemLayoutView)
        }
        return EmptyViewHolder(itemLayoutView)
    }

    override fun getItemViewType(position: Int): Int {
        if (items.isEmpty()) {
            return TYPE_EMPTY
        }
        return TYPE_ITEM
    }

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.size else 1
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (viewHolder is AdvertisementViewHolder) {
            val advertisement = items[position]
            val tradeType = TradeType.valueOf(advertisement.tradeType)
            var type = ""
            when (tradeType) {
                TradeType.LOCAL_BUY -> type = context.getString(R.string.text_advertisement_item_local_buy)
                TradeType.LOCAL_SELL -> type = context.getString(R.string.text_advertisement_item_local_sale)
                TradeType.ONLINE_BUY -> type = context.getString(R.string.text_advertisement_item_online_buy)
                TradeType.ONLINE_SELL -> type = context.getString(R.string.text_advertisement_item_online_sale)
                TradeType.NONE -> TODO()
            }

            val price = advertisement.tempPrice + " " + advertisement.currency
            val location = advertisement.location
            if (TradeType.LOCAL_SELL == tradeType || TradeType.LOCAL_BUY == tradeType) {
                if (TradeUtils.isAtm(advertisement)) {
                    viewHolder.advertisementType!!.text = context.getString(R.string.text_atm)
                } else {
                    viewHolder.advertisementType!!.text = "$type $price"
                    viewHolder.advertisementDetails!!.text = context.getString(R.string.text_in_caps, location)
                }

            } else {

                val adLocation = if (TradeUtils.isOnlineTrade(advertisement)) advertisement.location else advertisement.city
                val paymentMethod = TradeUtils.getPaymentMethodFromItems(context, advertisement, methods)
                if (TextUtils.isEmpty(paymentMethod)) {
                    viewHolder.advertisementDetails!!.text = context.getString(R.string.text_in_caps, adLocation)
                } else {
                    viewHolder.advertisementDetails!!.text = context.getString(R.string.text_with_int, paymentMethod, adLocation)
                }

                viewHolder.advertisementType!!.text = "$type $price"
            }

            if (advertisement.visible) {
                viewHolder.icon!!.setImageResource(R.drawable.ic_action_visibility_dark)
            } else {
                viewHolder.icon!!.setImageResource(R.drawable.ic_action_visibility_off_dark)
            }

            val date = Dates.parseLocaleDate(advertisement.createdAt)
            viewHolder.advertisementId!!.setText(advertisement.adId)
            viewHolder.advertisementDate!!.text = date
        }
    }

    fun getItemAt(position: Int): Advertisement? {
        return if (!items.isEmpty() && items.size > position) {
            items[position]
        } else null
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class AdvertisementViewHolder(itemView: View) : ViewHolder(itemView) {
        var row: View? = null
        var advertisementType: TextView? = null
        var icon: ImageView? = null
        var advertisementDetails: TextView? = null
        var advertisementId: TextView? = null
        var advertisementDate: TextView? = null
    }

    // TODO kotlin replacement
    inner class EmptyViewHolder(itemView: View) : ViewHolder(itemView) {
        fun advertiseButtonClicked() {
            onItemClickListener.onAdvertiseButtonClicked()
        }
        fun searchButtonClicked() {
            onItemClickListener.onSearchButtonClicked()
        }
    }

    companion object {
        private val TYPE_EMPTY = R.layout.view_empty_dashboard
        private val TYPE_ITEM = R.layout.adapter_dashboard_advertisement_list
    }
}
