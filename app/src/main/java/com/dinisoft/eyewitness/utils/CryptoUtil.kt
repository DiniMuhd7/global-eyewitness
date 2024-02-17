package com.dinisoft.eyewitness.utils

import android.media.MediaCodec.CryptoException
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class CryptoUtil {

    private val ALGORITHM = "AES"
    private val TRANSFORMATION = "AES"

    @Throws(CryptoException::class)
    fun encrypt(key: String, inputFile: File, outputFile: File) {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile)
    }

    @Throws(CryptoException::class)
    fun decrypt(key: String, inputFile: File, outputFile: File) {
        doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile)
    }

    @Throws(CryptoException::class)
    private fun doCrypto(cipherMode: Int, key: String, inputFile: File,
                         outputFile: File) {
        try {

            val secretKey: Key = SecretKeySpec(key.toByteArray(), ALGORITHM)
            val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(cipherMode, secretKey)

            val inputStream = FileInputStream(inputFile)
            val inputBytes = ByteArray(inputFile.length() as Int)
            inputStream.read(inputBytes)

            val outputBytes: ByteArray = cipher.doFinal(inputBytes)
            val outputStream = FileOutputStream(outputFile)
            outputStream.write(outputBytes)

            inputStream.close()
            outputStream.close()

        } catch (ex: NoSuchPaddingException) {
            throw CryptoException(0, ex.localizedMessage)
        } catch (ex: NoSuchAlgorithmException) {
            throw CryptoException(0, ex.localizedMessage)
        } catch (ex: InvalidKeyException) {
            throw CryptoException(0, ex.localizedMessage)
        } catch (ex: BadPaddingException) {
            throw CryptoException(0, ex.localizedMessage)
        } catch (ex: IllegalBlockSizeException) {
            throw CryptoException(0, ex.localizedMessage)
        } catch (ex: IOException) {
            throw CryptoException(0, ex.localizedMessage)
        }
    }

}