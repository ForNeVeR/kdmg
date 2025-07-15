// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg

import java.nio.MappedByteBuffer

fun MappedByteBuffer.getUInt32() = getInt().toUInt()
