// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg

import java.nio.MappedByteBuffer

internal fun MappedByteBuffer.getUInt8(): UByte = get().toUByte()
internal fun MappedByteBuffer.getUInt16(): UShort = getShort().toUShort()
internal fun MappedByteBuffer.getUInt32(): UInt = getInt().toUInt()
