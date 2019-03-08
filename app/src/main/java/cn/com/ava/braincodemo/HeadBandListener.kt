package cn.com.ava.braincodemo

import tech.brainco.fusi.sdk.FusiHeadband
import tech.brainco.fusi.sdk.OnAttentionListener
import tech.brainco.fusi.sdk.OnContactStateChangeListener


/**
 * 传入头环对象的监听
 * */
abstract class OnAttentionChangedListenerEx(val headband: FusiHeadband) :OnAttentionListener

abstract  class OnContactChangedListenerEx(val headband: FusiHeadband):OnContactStateChangeListener