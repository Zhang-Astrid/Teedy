package com.sismics.docs.rest.util;

import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.UserRegistrationRequestDao;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserRegistrationRequest;
import com.sismics.rest.exception.ForbiddenClientException;

/**
 * User authentication utility.
 *
 * @author ZS
 */
public class UserAuthenticationUtil {

    /**
     * Authenticate a user and handle registration request statuses.
     *
     * @param username Username
     * @param password Password
     * @return The authenticated user
     * @throws ForbiddenClientException If authentication fails
     */
    public static User authenticateWithRequestCheck(String username, String password) throws ForbiddenClientException {
        UserDao userDao = new UserDao();
        User user = userDao.authenticate(username, password);

        if (user == null) {
            // 检查是否有待审核的注册请求
            UserRegistrationRequestDao requestDao = new UserRegistrationRequestDao();
            UserRegistrationRequest request = requestDao.getByUsername(username);

            if (request != null) {
                if ("PENDING".equals(request.getStatus())) {
                    throw new ForbiddenClientException("您的账号正在等待管理员审核，请耐心等待");
                } else if ("REJECTED".equals(request.getStatus())) {
                    throw new ForbiddenClientException("您的注册申请已被拒绝，请联系管理员");
                }
            }

            throw new ForbiddenClientException("认证失败，用户名或密码错误");
        }

        return user;
    }
}