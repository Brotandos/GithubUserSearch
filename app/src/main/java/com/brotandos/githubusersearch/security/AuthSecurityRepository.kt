package com.brotandos.githubusersearch.security

import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.security.auth.x500.X500Principal

private const val KEY_STORE_TYPE = "AndroidKeyStore"
private const val PREF_KEY_PIN_ENCRYPTION = "PREF_KEY_PIN_ENCRYPTION"
private const val PREF_KEY_FINGERPRINT_ENCRYPTION = "PREF_KEY_FINGERPRINT_ENCRYPTION"
private const val PREF_KEY_PIN_IV = "PREF_KEY_PIN_IV"
private const val PREF_KEY_FINGERPRINT_IV = "PREF_KEY_FINGERPRINT_IV"
private const val FINGERPRINT_ALIAS = "GUS_FINGERPRINT_ALIAS"
private const val PIN_ALIAS_PREFIX = "GUS_PIN_ALIAS_"
private const val BASE_64_CODE_FLAG = Base64.NO_WRAP
private const val IS_USER_AUTHENTICATED = false

@RequiresApi(Build.VERSION_CODES.M)
private const val KEY_PARAMETER_SPEC_PURPOSES =
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT

@RequiresApi(Build.VERSION_CODES.M)
private const val TRANSFORMATION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

@RequiresApi(Build.VERSION_CODES.M)
private const val TRANSFORMATION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC

@RequiresApi(Build.VERSION_CODES.M)
private const val TRANSFORMATION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

@RequiresApi(Build.VERSION_CODES.M)
private const val CIPHER_TRANSFORMATION =
    "$TRANSFORMATION_ALGORITHM/$TRANSFORMATION_BLOCK_MODE/$TRANSFORMATION_PADDING"

private const val LOLLIPOP_TRANSFORMATION_ALGORITHM = "RSA"

private const val LOLLIPOP_CIPHER_TRANSFORMATION =
    "$LOLLIPOP_TRANSFORMATION_ALGORITHM/ECB/PKCS1Padding"

@RequiresApi(Build.VERSION_CODES.M)
private const val AES_KEY_SIZE = 128

private const val RSA_KEY_SIZE = 512

private const val KEY_EXPIRATION_YEARS: Int = 25

class AuthSecurityRepository(
    private val fingerprintManager: FingerprintManagerCompat,
    private val keyPairGeneratorSpecBuilder: KeyPairGeneratorSpec.Builder,
    private val sharedPreferences: SharedPreferences,
    private val packageName: String
) {

    companion object {
        const val CRYPTO_PREFERENCE_FILENAME = "crypto_prefs"
        const val PIN_LENGTH = 4
    }

    val isFingerprintEnabled = fingerprintManager.isHardwareDetected &&
            fingerprintManager.hasEnrolledFingerprints()

    fun checkPinIsValid(pin: String) = getLoadedKeyStore().containsAlias(PIN_ALIAS_PREFIX + pin)

    fun isFingerprintConfigured(): Boolean =
        sharedPreferences.contains(PREF_KEY_FINGERPRINT_ENCRYPTION)

    fun savePassword(pin: String, password: String) {
        val alias = PIN_ALIAS_PREFIX + pin
        val cipher = getEncryptionCipher(alias)
        val encryption = cipher.doFinal(password.toByteArray())
        saveEncryption(encryption, alias)
        saveInitializationVector(cipher.iv, alias)
        if (isFingerprintEnabled) initFingerprintEntering(password)
    }

    fun getPasswordByPin(pin: String): String? {
        if (!checkPinIsValid(pin)) return null
        val alias = PIN_ALIAS_PREFIX + pin
        val cipher = getDecryptionCipher(alias)
        val decryption = cipher.doFinal(getEncryption(alias))
        return String(decryption)
    }

    fun listenToFingerprint(): Flowable<FingerprintStatus> =
        Flowable.create<FingerprintStatus>(::authenticateByFingerprint, BackpressureStrategy.LATEST)

    private fun getLoadedKeyStore() = KeyStore
        .getInstance(KEY_STORE_TYPE)
        .apply { load(null) }

    private fun getCipherInstance() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } else {
            Cipher.getInstance(LOLLIPOP_CIPHER_TRANSFORMATION)
        }

    private fun getEncryptionCipher(alias: String) = getCipherInstance().also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val secretKey = getSecretKey(alias)
            it.init(Cipher.ENCRYPT_MODE, secretKey)
        } else {
            val publicKey = getPublicKey(alias)
            it.init(Cipher.ENCRYPT_MODE, publicKey)
        }
    }

    private fun getDecryptionCipher(alias: String) = getCipherInstance().also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val secretKey = getSecretKey(alias)
            val prefKey = if (alias == FINGERPRINT_ALIAS) PREF_KEY_FINGERPRINT_IV else PREF_KEY_PIN_IV
            val initializationVector = sharedPreferences.getString(prefKey, "").orEmpty()
            it.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                IvParameterSpec(
                    Base64.decode(initializationVector.toByteArray(), Base64.DEFAULT)
                )
            )
        } else {
            val privateKey = getPrivateKey(alias)
            it.init(Cipher.DECRYPT_MODE, privateKey)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getSecretKey(alias: String): SecretKey {
        val keyStore = getLoadedKeyStore()
        val aliases = keyStore.aliases()
        while (aliases.hasMoreElements()) {
            if (alias != aliases.nextElement()) continue
            return keyStore.getKey(alias, null) as SecretKey
        }
        generateSecretKey(alias)
        return keyStore.getKey(alias, null) as SecretKey
    }

    private fun getPublicKey(alias: String): PublicKey {
        val keyStore = getLoadedKeyStore()
        generateKeyPair(alias)
        val publicKey = keyStore.getCertificate(alias).publicKey
        val publicKeySpec = X509EncodedKeySpec(publicKey.encoded)
        return KeyFactory.getInstance(publicKey.algorithm).generatePublic(publicKeySpec)
    }

    private fun getPrivateKey(alias: String): PrivateKey =
        getLoadedKeyStore().getKey(alias, null) as PrivateKey

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateSecretKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(
            TRANSFORMATION_ALGORITHM,
            KEY_STORE_TYPE
        )
        keyGenerator.init(getAlgorithmParameters(alias))
        keyGenerator.generateKey()
    }

    private fun generateKeyPair(alias: String) {
        val keyGenerator = KeyPairGenerator.getInstance(
            LOLLIPOP_TRANSFORMATION_ALGORITHM,
            KEY_STORE_TYPE
        )
        keyGenerator.initialize(getAlgorithmParameters(alias))
        keyGenerator.generateKeyPair()
    }

    private fun getAlgorithmParameters(alias: String): AlgorithmParameterSpec =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyGenParameterSpec.Builder(alias, KEY_PARAMETER_SPEC_PURPOSES)
                .setKeySize(AES_KEY_SIZE)
                .setBlockModes(TRANSFORMATION_BLOCK_MODE)
                .setEncryptionPaddings(TRANSFORMATION_PADDING)
                .setUserAuthenticationRequired(IS_USER_AUTHENTICATED)
                .build()
        } else {
            val startDate = Calendar.getInstance()
            val endDate = Calendar.getInstance()
            endDate.add(
                Calendar.YEAR,
                KEY_EXPIRATION_YEARS
            )
            keyPairGeneratorSpecBuilder
                .setAlias(alias)
                .setKeySize(RSA_KEY_SIZE)
                .setSubject(X500Principal("CN=$alias, OU=$packageName"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(startDate.time)
                .setEndDate(endDate.time)
                .build()
        }

    private fun saveEncryption(encryption: ByteArray, alias: String = FINGERPRINT_ALIAS) {
        val encryptionString = Base64.encodeToString(
            encryption,
            BASE_64_CODE_FLAG
        )
        val key =
            if (alias == FINGERPRINT_ALIAS) PREF_KEY_FINGERPRINT_ENCRYPTION else PREF_KEY_PIN_ENCRYPTION
        sharedPreferences.edit {
            putString(key, encryptionString)
        }
    }

    private fun saveInitializationVector(iv: ByteArray, alias: String) {
        val ivString = Base64.encodeToString(iv, BASE_64_CODE_FLAG)
        val key = if (alias == FINGERPRINT_ALIAS) PREF_KEY_FINGERPRINT_IV else PREF_KEY_PIN_IV
        sharedPreferences.edit {
            putString(key, ivString)
        }
    }

    private fun getEncryption(alias: String): ByteArray {
        val key =
            if (alias == FINGERPRINT_ALIAS) PREF_KEY_FINGERPRINT_ENCRYPTION else PREF_KEY_PIN_ENCRYPTION
        val encryptionString = sharedPreferences.getString(key, null)
            ?: throw RuntimeException("Encryption for decoding lost, probably preference file named $CRYPTO_PREFERENCE_FILENAME deleted before")
        return Base64.decode(encryptionString, BASE_64_CODE_FLAG)
    }

    private fun initFingerprintEntering(password: String) {
        val encryptionCipher = getEncryptionCipher(FINGERPRINT_ALIAS)
        val encryption = encryptionCipher.doFinal(password.toByteArray())
        saveEncryption(encryption)
        saveInitializationVector(encryptionCipher.iv, FINGERPRINT_ALIAS)
    }

    private fun authenticateByFingerprint(
        flowableEmitter: FlowableEmitter<FingerprintStatus>
    ) = fingerprintManager.authenticate(
        FingerprintManagerCompat.CryptoObject(getDecryptionCipher(FINGERPRINT_ALIAS)),
        0,
        CancellationSignal(),
        FingerprintAuthenticationCallback(flowableEmitter, getEncryption(FINGERPRINT_ALIAS)), // Data changes here
        null
    )

    fun isPasswordStored(): Boolean = getLoadedKeyStore()
        .aliases()
        .toList()
        .find { it.contains(FINGERPRINT_ALIAS) || it.contains(PIN_ALIAS_PREFIX) } != null

    fun dropPassword() {
        val keystore = getLoadedKeyStore()
        keystore.deleteEntry(FINGERPRINT_ALIAS)
        keystore
            .aliases()
            .toList()
            .find { it.contains(PIN_ALIAS_PREFIX) }
            ?.let(keystore::deleteEntry)
        sharedPreferences.edit { clear() }
    }
}