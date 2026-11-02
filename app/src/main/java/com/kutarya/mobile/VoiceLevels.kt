package com.kutarya.mobile

object VoiceLevels {
    fun fromRms(value:Float):Float = if(value.isFinite()) ((value+2f)/12f).coerceIn(0f,1f) else 0f
}
