package com.firestorm.newview

import java.util.UUID

// Certificate field key constants
const val CERT_SUBJECT_NAME = "subject_name"
const val CERT_ISSUER_NAME = "issuer_name"
const val CERT_NAME_CN = "commonName"
const val CERT_SUBJECT_NAME_STRING = "subject_name_string"
const val CERT_ISSUER_NAME_STRING = "issuer_name_string"
const val CERT_SERIAL_NUMBER = "serial_number"
const val CERT_VALID_FROM = "valid_from"
const val CERT_VALID_TO = "valid_to"
const val CERT_SHA1_DIGEST = "sha1_digest"
const val CERT_MD5_DIGEST = "md5_digest"
const val CERT_HOSTNAME = "hostname"
const val CERT_BASIC_CONSTRAINTS = "basicConstraints"
const val CERT_BASIC_CONSTRAINTS_CA = "CA"
const val CERT_BASIC_CONSTRAINTS_PATHLEN = "pathLen"
const val CERT_KEY_USAGE = "keyUsage"
const val CERT_KU_DIGITAL_SIGNATURE = "digitalSignature"
const val CERT_KU_NON_REPUDIATION = "nonRepudiation"
const val CERT_KU_KEY_ENCIPHERMENT = "keyEncipherment"
const val CERT_KU_DATA_ENCIPHERMENT = "dataEncipherment"
const val CERT_KU_KEY_AGREEMENT = "keyAgreement"
const val CERT_KU_CERT_SIGN = "certSigning"
const val CERT_KU_CRL_SIGN = "crlSigning"
const val CERT_KU_ENCIPHER_ONLY = "encipherOnly"
const val CERT_KU_DECIPHER_ONLY = "decipherOnly"
const val BASIC_SECHANDLER = "BASIC_SECHANDLER"
const val CERT_VALIDATION_DATE = "validation_date"
const val CERT_EXTENDED_KEY_USAGE = "extendedKeyUsage"
const val CERT_EKU_SERVER_AUTH = "serverAuth"
const val CERT_EKU_TLS_SERVER_AUTH = "TLS Web Server Authentication"
const val CERT_SUBJECT_KEY_IDENTFIER = "subjectKeyIdentifier"
const val CERT_AUTHORITY_KEY_IDENTIFIER = "authorityKeyIdentifier"
const val CERT_AUTHORITY_KEY_IDENTIFIER_ID = "authorityKeyIdentifierId"
const val CERT_AUTHORITY_KEY_IDENTIFIER_NAME = "authorityKeyIdentifierName"
const val CERT_AUTHORITY_KEY_IDENTIFIER_SERIAL = "authorityKeyIdentifierSerial"

// Validation policy flags
const val VALIDATION_POLICY_TIME = 1
const val VALIDATION_POLICY_TRUSTED = 2
const val VALIDATION_POLICY_HOSTNAME = 4
const val VALIDATION_POLICY_SSL_KU = 8
const val VALIDATION_POLICY_CA_KU = 16
const val VALIDATION_POLICY_CA_BASIC_CONSTRAINTS = 32
const val VALIDATION_POLICY_SSL = (VALIDATION_POLICY_TIME
        or VALIDATION_POLICY_HOSTNAME
        or VALIDATION_POLICY_TRUSTED
        or VALIDATION_POLICY_SSL_KU
        or VALIDATION_POLICY_CA_BASIC_CONSTRAINTS
        or VALIDATION_POLICY_CA_KU)

// Credential type constants
const val CRED_IDENTIFIER_TYPE_ACCOUNT = "account"
const val CRED_IDENTIFIER_TYPE_AGENT = "agent"
const val CRED_AUTHENTICATOR_TYPE_CLEAR = "clear"
const val CRED_AUTHENTICATOR_TYPE_HASH = "hash"

class LLProtectedDataException(msg: String) : RuntimeException(msg)

abstract class LLCertificate {
    abstract fun getPem(): String
    abstract fun getBinary(): MutableList<UByte>
    abstract fun getLLSD(llsd: MutableMap<String, Any>)
    // OpenSSL X509 handle intentionally omitted; callers use JVM PKI equivalents
}

abstract class LLCertificateVector {
    abstract operator fun get(index: Int): LLCertificate
    abstract fun begin(): Iterator<LLCertificate>
    abstract fun end(): Iterator<LLCertificate>
    abstract fun find(params: Map<String, Any>): Iterator<LLCertificate>
    abstract fun size(): Int
    abstract fun add(cert: LLCertificate)
    abstract fun insert(location: Iterator<LLCertificate>, cert: LLCertificate)
    abstract fun erase(cert: Iterator<LLCertificate>): LLCertificate?
}

abstract class LLCertificateChain : LLCertificateVector()

abstract class LLCertificateStore : LLCertificateVector() {
    abstract fun save()
    abstract fun storeId(): String
    abstract fun validate(validationPolicy: Int, certChain: LLCertificateChain, validationParams: Map<String, Any>)
    abstract fun clearSertCache()
}

open class LLCredential(val credentialName: String = "") {
    var identifier: MutableMap<String, Any> = mutableMapOf()
    var authenticator: MutableMap<String, Any> = mutableMapOf()

    open fun setCredentialData(id: Map<String, Any>, auth: Map<String, Any>) {
        identifier = id.toMutableMap()
        authenticator = auth.toMutableMap()
    }

    open fun getIdentifier(): Map<String, Any> = identifier
    open fun getAuthenticator(): Map<String, Any> = authenticator

    open fun identifierType(): String = identifier["type"] as? String ?: ""
    open fun authenticatorType(): String = authenticator["type"] as? String ?: ""

    open fun getLoginParams(): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        try {
            when (identifier["type"] as? String) {
                "agent" -> {
                    result["passwd"] = "\$1\$" + (authenticator["secret"] as? String ?: "")
                    result["first"] = identifier["first_name"] ?: ""
                    result["last"] = identifier["last_name"] ?: ""
                }
                "account" -> {
                    result["username"] = identifier["account_name"] ?: ""
                    result["passwd"] = authenticator["secret"] ?: ""
                }
            }
        } catch (_: Exception) {
            // corrupt credential data; return empty params
        }
        return result
    }

    open fun getCredentialName(): String = credentialName
    open fun clearAuthenticator() { authenticator = mutableMapOf() }
    open fun userID(): String = "unknown"
    open fun asString(): String = "unknown"
    override fun toString(): String = asString()
}

class LLCertException(val certData: Map<String, Any>, msg: String) : RuntimeException(msg)
class LLAllocationCertException(certData: Map<String, Any>) : LLCertException(certData, "CertAllocationFailure")
class LLInvalidCertificate(certData: Map<String, Any>) : LLCertException(certData, "CertInvalid")
class LLCertValidationTrustException(certData: Map<String, Any>) : LLCertException(certData, "CertUntrusted")
class LLCertValidationHostnameException(val hostname: String, certData: Map<String, Any>) : LLCertException(certData, "CertInvalidHostname")
class LLCertValidationExpirationException(certData: Map<String, Any>, val time: Double) : LLCertException(certData, "CertExpired")
class LLCertKeyUsageValidationException(certData: Map<String, Any>) : LLCertException(certData, "CertKeyUsage")
class LLCertBasicConstraintsValidationException(certData: Map<String, Any>) : LLCertException(certData, "CertBasicConstraints")
class LLCertValidationInvalidSignatureException(certData: Map<String, Any>) : LLCertException(certData, "CertInvalidSignature")

abstract class LLSecAPIHandler {
    open fun init() {}
    abstract fun getCertificate(pemCert: String): LLCertificate
    abstract fun getCertificateChain(chainContext: Any): LLCertificateChain
    abstract fun getCertificateStore(storeId: String): LLCertificateStore
    abstract fun setProtectedData(dataType: String, dataId: String, data: Any)
    abstract fun getProtectedData(dataType: String, dataId: String): Any?
    abstract fun deleteProtectedData(dataType: String, dataId: String)
    abstract fun addToProtectedMap(dataType: String, dataId: String, mapElem: String, data: Any)
    abstract fun removeFromProtectedMap(dataType: String, dataId: String, mapElem: String)
    abstract fun syncProtectedMap()
    abstract fun createCredential(grid: String, identifier: Map<String, Any>, authenticator: Map<String, Any>): LLCredential
    abstract fun loadCredential(name: String): LLCredential
    abstract fun listCredentials(): MutableList<String>
    abstract fun saveCredential(cred: LLCredential, saveAuthenticator: Boolean)
    abstract fun deleteCredential(cred: LLCredential)
    abstract fun hasCredentialMap(storage: String, grid: String): Boolean
    abstract fun emptyCredentialMap(storage: String, grid: String): Boolean
    abstract fun loadCredentialMap(storage: String, grid: String, credentialMap: MutableMap<String, LLCredential>)
    abstract fun loadFromCredentialMap(storage: String, grid: String, userid: String): LLCredential
    abstract fun addToCredentialMap(storage: String, cred: LLCredential, saveAuthenticator: Boolean)
    abstract fun removeFromCredentialMap(storage: String, cred: LLCredential)
    abstract fun removeFromCredentialMap(storage: String, grid: String, userid: String)
    abstract fun removeCredentialMap(storage: String, grid: String)
}

// Global registry of security handlers
private val gHandlerMap: MutableMap<String, LLSecAPIHandler> = mutableMapOf()
var gSecAPIHandler: LLSecAPIHandler? = null

fun initializeSecHandler() {
    System.err.println("LLSecApi: initializeSecHandler not yet implemented")
}

fun clearSecHandler() {
    gSecAPIHandler = null
    gHandlerMap.clear()
}

fun getSecHandler(handlerType: String): LLSecAPIHandler? = gHandlerMap[handlerType]

fun registerSecHandler(handlerType: String, handler: LLSecAPIHandler) {
    gHandlerMap[handlerType] = handler
}
