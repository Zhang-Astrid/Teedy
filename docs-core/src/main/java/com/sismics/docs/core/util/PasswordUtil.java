package com.sismics.docs.core.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.common.base.Strings;
import com.sismics.docs.core.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Password related utilities.
 *
 * @author ZS
 */
public class PasswordUtil {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(PasswordUtil.class);

    /**
     * Hash the user's password.
     *
     * @param password Clear password
     * @return Hashed password
     */
    public static String hashPassword(String password) {
        int bcryptWork = Constants.DEFAULT_BCRYPT_WORK;
        String envBcryptWork = System.getenv(Constants.BCRYPT_WORK_ENV);
        if (!Strings.isNullOrEmpty(envBcryptWork)) {
            try {
                int envBcryptWorkInt = Integer.parseInt(envBcryptWork);
                if (envBcryptWorkInt >= 4 && envBcryptWorkInt <= 31) {
                    bcryptWork = envBcryptWorkInt;
                } else {
                    log.warn(Constants.BCRYPT_WORK_ENV + " needs to be in range 4...31. Falling back to " + Constants.DEFAULT_BCRYPT_WORK + ".");
                }
            } catch (NumberFormatException e) {
                log.warn(Constants.BCRYPT_WORK_ENV + " needs to be a number in range 4...31. Falling back to " + Constants.DEFAULT_BCRYPT_WORK + ".");
            }
        }
        return BCrypt.withDefaults().hashToString(bcryptWork, password.toCharArray());
    }

    /**
     * Verify a password against a hash.
     *
     * @param password Clear password
     * @param hash Hash to check against
     * @return True if the password matches
     */
    public static boolean verify(String password, String hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}