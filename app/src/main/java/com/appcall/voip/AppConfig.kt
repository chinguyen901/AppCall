package com.appcall.voip

object AppConfig {
    const val DEFAULT_SIP_DOMAIN = "192.168.2.2"
    const val DEFAULT_SIP_PROXY = "sip:192.168.2.2:5060"

    fun buildProxyFromDomain(domain: String): String = "sip:$domain:5060"
}
