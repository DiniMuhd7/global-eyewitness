/*
 *  Copyright (c) 2021. Shamsudeen A. Muhammed, Dinisoft Technology Ltd
 *
 *  This file is part of Eyewitness-Android a client for Eyewitness Core.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.dinisoft.eyewitness.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dinisoft.eyewitness.ChatActivity
import com.dinisoft.eyewitness.utils.NetworkUtil

/**
 * Simple class to detect changes in network connectivity.
 *
 *
 * It should trigger connection and disconnection actions
 * on the appropriate handler, which for now is [MainActivity].
 *
 *
 * @see .setMainActivityHandler
 * @author Paul Scott
 */

class NetworkChangeReceiver : BroadcastReceiver() {

    private var main: ChatActivity? = null

    fun setMainActivityHandler(main: ChatActivity) {
        this.main = main
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = NetworkUtil.getConnectivityStatusString(context)
        if ("android.net.conn.CONNECTIVITY_CHANGE" != intent.action) {
            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                // do something about it.. IDK
            } else if (main != null) {
                // reconnect websocket
                if (main!!.webSocketClient == null || main!!.webSocketClient!!.connection.isClosed) {
                    main!!.connectWebSocket()
                }
            }
        }
    }
}
