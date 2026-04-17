package com.appcall.voip

object AppConfig {
    const val DEFAULT_SIP_DOMAIN = "192.168.2.2"
    const val DEFAULT_SIP_PROXY = "sip:192.168.2.2:5065"
    const val USE_PREBUILT_UI = true

    fun buildProxyFromDomain(domain: String): String = "sip:$domain:5065"
}
