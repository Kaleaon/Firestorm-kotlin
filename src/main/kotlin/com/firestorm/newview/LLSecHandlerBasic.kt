package com.firestorm.newview

import java.io.File
import java.time.Instant
import java.util.UUID

// ---- Certificate helpers (OpenSSL-backed in C++; stubbed here) ----

fun certNameFromX509Name(name: Any): MutableMap<String, Any> {
    TODO("APR: use JVM PKI (X500Principal) to extract name components")
}

fun certStringNameFromX509Name(name: Any): String {
    TODO("APR: use JVM PKI (X500Principal.getName) to produce RFC 2253 string")
}

fun certStringFromAsn1Integer(value: Any): String {
    TODO("APR: use JVM BigInteger to convert ASN.1 integer to hex string")
}

fun certDateFromAsn1Time(asn1Time: Any): Instant {
    TODO("APR: parse ASN.1 UTCTime / GeneralizedTime string to java.time.Instant")
}

fun certGetDigest(digestType: String, cert: Any): String {
    TODO("APR: use JVM MessageDigest to compute $digestType digest of certificate")
}

// ---- LLBasicCertificate ----

class LLBasicCertificate : LLCertificate {
    private var pemData: String = ""
    private val llsdInfo: MutableMap<String, Any> = mutableMapOf()

    constructor(pemCert: String) {
        TODO("APR: use JVM CertificateFactory.getInstance(\"X.509\") to parse PEM")
    }

    constructor(x509Cert: Any) {
        TODO("APR: wrap existing java.security.cert.X509Certificate")
    }

    override fun getPem(): String {
        TODO("APR: use JVM Base64 to re-encode the certificate in PEM format")
    }

    override fun getBinary(): MutableList<UByte> {
        TODO("APR: return X509Certificate.getEncoded() as UByte list")
    }

    override fun getLLSD(llsd: MutableMap<String, Any>) {
        if (llsdInfo.isEmpty()) initLLSD()
        llsd.putAll(llsdInfo)
    }

    fun setLLSD(name: String, value: Any) {
        llsdInfo[name] = value
    }

    private fun initLLSD() {
        TODO("APR: populate llsdInfo from JVM X509Certificate fields and extensions")
    }
}

// ---- LLBasicCertificateVector ----

open class LLBasicCertificateVector : LLCertificateVector() {
    protected val certs: MutableList<LLCertificate> = mutableListOf()

    override fun get(index: Int): LLCertificate = certs[index]
    override fun size(): Int = certs.size
    override fun begin(): Iterator<LLCertificate> = certs.iterator()
    override fun end(): Iterator<LLCertificate> = certs.listIterator(certs.size)

    override fun find(params: Map<String, Any>): Iterator<LLCertificate> {
        val certInfo = mutableMapOf<String, Any>()
        for (cert in certs) {
            cert.getLLSD(certInfo)
            if (params.all { (k, v) -> certInfo.containsKey(k) && valueCompareLLSD(certInfo[k], v) }) {
                val idx = certs.indexOf(cert)
                return certs.listIterator(idx)
            }
            certInfo.clear()
        }
        return certs.listIterator(certs.size)
    }

    override fun add(cert: LLCertificate) = insert(end(), cert)

    override fun insert(location: Iterator<LLCertificate>, cert: LLCertificate) {
        val certInfo = mutableMapOf<String, Any>()
        cert.getLLSD(certInfo)
        if (!certInfo.containsKey(CERT_SUBJECT_KEY_IDENTFIER)) return

        val searchParams = mapOf(CERT_SUBJECT_KEY_IDENTFIER to certInfo[CERT_SUBJECT_KEY_IDENTFIER]!!)
        if (find(searchParams) == end()) {
            // Insert at the position indicated by the iterator; we approximate by appending for the end() case.
            val insertIdx = (location as? java.util.ListIterator<*>)?.nextIndex() ?: certs.size
            certs.add(insertIdx.coerceIn(0, certs.size), cert)
        }
    }

    override fun erase(cert: Iterator<LLCertificate>): LLCertificate? {
        val li = cert as? java.util.ListIterator<LLCertificate> ?: return null
        val idx = li.nextIndex() - 1
        if (idx < 0 || idx >= certs.size) return null
        return certs.removeAt(idx)
    }
}

// ---- LLBasicCertificateStore ----

class LLBasicCertificateStore(filename: String) : LLBasicCertificateVector(), LLCertificateStore() {
    private var mFilename: String = filename
    private val trustedCertCache: MutableMap<String, Pair<Instant, Instant>> = mutableMapOf()

    init {
        loadFromFile(filename)
    }

    fun loadFromFile(filename: String) {
        TODO("APR: read PEM bundle from file, parse each certificate with JVM CertificateFactory")
    }

    override fun save() {
        TODO("APR: write each certificate PEM to file at mFilename")
    }

    override fun storeId(): String = ""

    override fun validate(validationPolicy: Int, certChain: LLCertificateChain, validationParams: Map<String, Any>) {
        TODO("APR: implement chain validation using javax.net.ssl or BouncyCastle; consult trustedCertCache")
    }

    override fun clearSertCache() {
        trustedCertCache.clear()
    }

    // LLCertificateVector delegation (already satisfied by LLBasicCertificateVector)
}

// ---- LLBasicCertificateChain ----

class LLBasicCertificateChain(storeCtx: Any?) : LLBasicCertificateVector(), LLCertificateChain() {
    init {
        if (storeCtx == null) return@init
        TODO("APR: extract leaf cert and untrusted chain from JVM SSL context equivalent; build ordered chain")
    }
}

// ---- LLSecAPIBasicCredential ----

class LLSecAPIBasicCredential(credName: String) : LLCredential(credName) {
    override fun userID(): String {
        if (identifier.isEmpty()) return "$credentialName(null)"
        return when (identifier["type"] as? String) {
            "agent" -> {
                val id = "${identifier["first_name"]}_${identifier["last_name"]}"
                id.lowercase()
            }
            "account" -> (identifier["account_name"] as? String ?: "").lowercase()
            else -> "unknown"
        }
    }

    override fun asString(): String {
        if (identifier.isEmpty()) return "$credentialName:(null)"
        return when (identifier["type"] as? String) {
            "agent" -> "$credentialName:${identifier["first_name"]} ${identifier["last_name"]}"
            "account" -> "$credentialName:${identifier["account_name"]}"
            else -> "$credentialName:(unknown type)"
        }
    }
}

// ---- LLSecAPIBasicHandler ----

class LLSecAPIBasicHandler : LLSecAPIHandler {
    private var protectedDataFilename: String = ""
    private var protectedDataMap: MutableMap<String, Any> = mutableMapOf()
    private var store: LLBasicCertificateStore? = null
    private var legacyPasswordPath: String = ""

    constructor(protectedDataFile: String, legacyPasswordPath: String) {
        this.protectedDataFilename = protectedDataFile
        this.legacyPasswordPath = legacyPasswordPath
    }

    constructor()

    override fun init() {
        protectedDataMap = mutableMapOf()
        if (protectedDataFilename.isEmpty()) {
            TODO("APR: resolve user settings paths via JVM equivalent of gDirUtilp; load CA store")
        }
        readProtectedData()
    }

    private fun readProtectedData(uniqueId: ByteArray) {
        TODO("APR: read RC4-encrypted protected data file; decrypt with XOR key derived from machine ID")
    }

    private fun readProtectedData() {
        TODO("APR: get machine unique ID via JVM equivalent of LLMachineID; call readProtectedData(ByteArray)")
    }

    private fun writeProtectedData() {
        TODO("APR: serialize protectedDataMap to XML LLSD, encrypt with RC4+machine-ID salt, write atomically")
    }

    private fun legacyLoadPassword(): String {
        TODO("APR: read legacy password.dat, decrypt with machine MAC address via XOR cipher")
    }

    override fun getCertificate(pemCert: String): LLCertificate = LLBasicCertificate(pemCert)

    override fun getCertificateChain(chainContext: Any): LLCertificateChain = LLBasicCertificateChain(chainContext)

    override fun getCertificateStore(storeId: String): LLCertificateStore = store
        ?: throw IllegalStateException("Certificate store not initialized")

    override fun getProtectedData(dataType: String, dataId: String): Any? {
        val typeMap = protectedDataMap[dataType] as? MutableMap<*, *> ?: return null
        return typeMap[dataId]
    }

    override fun deleteProtectedData(dataType: String, dataId: String) {
        val typeMap = protectedDataMap[dataType] as? MutableMap<*, *> ?: return
        @Suppress("UNCHECKED_CAST")
        (typeMap as MutableMap<String, Any>).remove(dataId)
    }

    override fun setProtectedData(dataType: String, dataId: String, data: Any) {
        @Suppress("UNCHECKED_CAST")
        val typeMap = protectedDataMap.getOrPut(dataType) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        typeMap[dataId] = data
    }

    override fun addToProtectedMap(dataType: String, dataId: String, mapElem: String, data: Any) {
        @Suppress("UNCHECKED_CAST")
        val typeMap = protectedDataMap.getOrPut(dataType) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        @Suppress("UNCHECKED_CAST")
        val idMap = typeMap.getOrPut(dataId) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        idMap[mapElem] = data
    }

    override fun removeFromProtectedMap(dataType: String, dataId: String, mapElem: String) {
        @Suppress("UNCHECKED_CAST")
        val typeMap = protectedDataMap[dataType] as? MutableMap<String, Any> ?: return
        @Suppress("UNCHECKED_CAST")
        val idMap = typeMap[dataId] as? MutableMap<String, Any> ?: return
        idMap.remove(mapElem)
    }

    override fun syncProtectedMap() = writeProtectedData()

    override fun createCredential(grid: String, identifier: Map<String, Any>, authenticator: Map<String, Any>): LLCredential {
        val result = LLSecAPIBasicCredential(grid)
        result.setCredentialData(identifier, authenticator)
        return result
    }

    override fun loadCredential(name: String): LLCredential {
        val result = LLSecAPIBasicCredential(name)
        val credential = getProtectedData(DEFAULT_CREDENTIAL_STORAGE, name)
        if (credential is Map<*, *> && credential.containsKey("identifier")) {
            @Suppress("UNCHECKED_CAST")
            val id = credential["identifier"] as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val auth = credential["authenticator"] as? Map<String, Any> ?: emptyMap()
            result.setCredentialData(id, auth)
        } else {
            TODO("APR: fall back to legacy first/last name from saved settings and legacy password file")
        }
        return result
    }

    override fun saveCredential(cred: LLCredential, saveAuthenticator: Boolean) {
        val credential = mutableMapOf<String, Any>("identifier" to cred.getIdentifier())
        if (saveAuthenticator) credential["authenticator"] = cred.getAuthenticator()
        setProtectedData(DEFAULT_CREDENTIAL_STORAGE, cred.getCredentialName(), credential)
        writeProtectedData()
    }

    override fun deleteCredential(cred: LLCredential) {
        deleteProtectedData(DEFAULT_CREDENTIAL_STORAGE, cred.getCredentialName())
        cred.setCredentialData(emptyMap(), emptyMap())
        writeProtectedData()
    }

    override fun listCredentials(): MutableList<String> {
        @Suppress("UNCHECKED_CAST")
        val storage = protectedDataMap[DEFAULT_CREDENTIAL_STORAGE] as? Map<String, Any> ?: return mutableListOf()
        return storage.keys.toMutableList()
    }

    override fun hasCredentialMap(storage: String, grid: String): Boolean {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        return getProtectedData(storage, grid) is Map<*, *>
    }

    override fun emptyCredentialMap(storage: String, grid: String): Boolean {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        val data = getProtectedData(storage, grid)
        return data !is Map<*, *> || (data as Map<*, *>).isEmpty()
    }

    override fun loadCredentialMap(storage: String, grid: String, credentialMap: MutableMap<String, LLCredential>) {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        @Suppress("UNCHECKED_CAST")
        val credential = getProtectedData(storage, grid) as? Map<String, Any> ?: return
        for ((name, linkMap) in credential) {
            @Suppress("UNCHECKED_CAST")
            val lm = linkMap as? Map<String, Any> ?: continue
            val result = LLSecAPIBasicCredential(grid)
            @Suppress("UNCHECKED_CAST")
            val id = lm["identifier"] as? Map<String, Any> ?: continue
            @Suppress("UNCHECKED_CAST")
            val auth = lm["authenticator"] as? Map<String, Any> ?: emptyMap()
            result.setCredentialData(id, auth)
            credentialMap[name] = result
        }
    }

    override fun loadFromCredentialMap(storage: String, grid: String, userid: String): LLCredential {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        val result = LLSecAPIBasicCredential(grid)
        @Suppress("UNCHECKED_CAST")
        val credential = getProtectedData(storage, grid) as? Map<String, Any> ?: return result
        @Suppress("UNCHECKED_CAST")
        val entry = credential[userid] as? Map<String, Any> ?: return result
        @Suppress("UNCHECKED_CAST")
        val id = entry["identifier"] as? Map<String, Any> ?: return result
        @Suppress("UNCHECKED_CAST")
        val auth = entry["authenticator"] as? Map<String, Any> ?: emptyMap()
        result.setCredentialData(id, auth)
        return result
    }

    override fun addToCredentialMap(storage: String, cred: LLCredential, saveAuthenticator: Boolean) {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        val credential = mutableMapOf<String, Any>("identifier" to cred.getIdentifier())
        if (saveAuthenticator) credential["authenticator"] = cred.getAuthenticator()
        addToProtectedMap(storage, cred.getCredentialName(), cred.userID(), credential)
        writeProtectedData()
    }

    override fun removeFromCredentialMap(storage: String, cred: LLCredential) {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        removeFromProtectedMap(storage, cred.getCredentialName(), cred.userID())
        cred.setCredentialData(emptyMap(), emptyMap())
        writeProtectedData()
    }

    override fun removeFromCredentialMap(storage: String, grid: String, userid: String) {
        require(storage != DEFAULT_CREDENTIAL_STORAGE) { "Storing maps in default single-item storage is not allowed" }
        val cred = loadFromCredentialMap(storage, grid, userid)
        removeFromProtectedMap(storage, grid, userid)
        cred.setCredentialData(emptyMap(), emptyMap())
        writeProtectedData()
    }

    override fun removeCredentialMap(storage: String, grid: String) {
        deleteProtectedData(storage, grid)
        writeProtectedData()
    }

    companion object {
        private const val DEFAULT_CREDENTIAL_STORAGE = "credential"
    }
}

// ---- Utility ----

fun valueCompareLLSD(lhs: Any?, rhs: Any?): Boolean {
    if (lhs == null && rhs == null) return true
    if (lhs == null || rhs == null) return false
    if (lhs::class != rhs::class) return false
    return when (lhs) {
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val l = lhs as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val r = rhs as Map<String, Any>
            l.keys.all { r.containsKey(it) } && r.all { (k, v) -> valueCompareLLSD(l[k], v) }
        }
        is List<*> -> {
            val l = lhs as List<*>
            val r = rhs as List<*>
            l.size == r.size && l.zip(r).all { (a, b) -> valueCompareLLSD(a, b) }
        }
        else -> lhs.toString() == rhs.toString()
    }
}

fun certSubdomainWildcardMatch(subdomain: String, wildcard: String): Boolean {
    val starPos = wildcard.indexOf('*')
    if (starPos == -1) return subdomain == wildcard
    if (!subdomain.startsWith(wildcard.substring(0, starPos))) return false
    val afterStar = wildcard.substring(starPos + 1)
    if (afterStar.isEmpty()) return true
    val nextStarInAfter = afterStar.indexOf('*')
    val matchSegment = if (nextStarInAfter == -1) afterStar else afterStar.substring(0, nextStarInAfter)
    var remaining = subdomain.substring(starPos)
    var pos = remaining.indexOf(matchSegment)
    while (pos != -1) {
        remaining = remaining.substring(pos)
        if (certSubdomainWildcardMatch(remaining, afterStar)) return true
        pos = remaining.indexOf(matchSegment, 1)
    }
    return false
}

fun certHostnameWildcardMatch(hostname: String, commonName: String): Boolean {
    var host = hostname.trimEnd('.')
    var cn = commonName.trimEnd('.')
    while (true) {
        val hDot = host.lastIndexOf('.')
        val cDot = cn.lastIndexOf('.')
        if (hDot == -1 || cDot == -1) break
        val hPart = host.substring(hDot + 1)
        val cPart = cn.substring(cDot + 1)
        if (!certSubdomainWildcardMatch(hPart, cPart)) return false
        host = host.substring(0, hDot)
        cn = cn.substring(0, cDot)
    }
    if (cn == "*") return true
    return certSubdomainWildcardMatch(host, cn)
}
