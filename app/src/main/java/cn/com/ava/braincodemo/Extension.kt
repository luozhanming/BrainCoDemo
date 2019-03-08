package cn.com.ava.braincodemo

import tech.brainco.fusi.sdk.FusiHeadband

fun FusiHeadband.clearAllListener(): Unit {
    this.setOnAttentionListener(null)
    this.setOnBlinkListener(null)
    this.setOnConnectionChangeListener(null)
    this.setOnContactStateChangeListener(null)
    this.setOnBrainWaveListener(null)
    this.setOnEEGListener(null)
}



// fun FusiHeadband.equals(headband: Any?): Boolean {
//    headband ?: return false
//    val comparableBand = headband as? FusiHeadband ?: return false
//    return comparableBand.ip.equals(ip) and comparableBand.name.equals(name) and comparableBand.mac.equals(mac)
//}