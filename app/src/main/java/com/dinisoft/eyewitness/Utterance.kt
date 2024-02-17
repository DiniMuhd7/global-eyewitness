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

package com.dinisoft.eyewitness

/**
 * Data class representing a response from Mycroft or command from the user
 */
data class Utterance(val utterance: String, val from: UtteranceFrom, val username: String, val cdate: String)