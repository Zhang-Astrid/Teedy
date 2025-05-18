package com.sismics.docs.rest.resource;

import com.sismics.docs.core.constant.AuditLogType;
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.UserRegistrationRequestDao;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserRegistrationRequest;
import com.sismics.docs.core.util.AuditLogUtil;
import com.sismics.docs.core.util.EncryptionUtil;
import com.sismics.docs.core.util.PasswordUtil;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.util.context.ThreadLocalContext;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User registration request REST resources.
 *
 * @author ZS
 */
@Path("/user/registration")
public class UserRegistrationResource extends BaseResource {

    /**
     * Creates a new user registration request.
     *
     * @param username User's username
     * @param password Password
     * @param email Email address
     * @return Response
     * @api {put} /user/registration Create a user registration request
     * @apiName PutUserRegistration
     * @apiGroup UserRegistration
     * @apiParam {String{3..50}} username Username
     * @apiParam {String{8..50}} password Password
     * @apiParam {String{1..100}} email E-mail
     * @apiSuccess {String} status Status OK
     * @apiError (client) ValidationError Validation error
     * @apiError (client) AlreadyExistingUsername Username already used
     * @apiError (client) AlreadyExistingRequest A request is already pending for this username
     * @apiError (server) UnknownError Unknown server error
     * @apiPermission none
     * @apiVersion 1.5.0
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("email") String email) {

        try {
            // 验证输入数据
            if (username == null || username.trim().isEmpty()) {
                throw new ClientException("ValidationError", "用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new ClientException("ValidationError", "密码不能为空");
            }
            if (email == null || email.trim().isEmpty()) {
                throw new ClientException("ValidationError", "邮箱不能为空");
            }

            username = ValidationUtil.validateLength(username, "username", 3, 50);
            ValidationUtil.validateUsername(username, "username");
            password = ValidationUtil.validateLength(password, "password", 8, 50);
            email = ValidationUtil.validateLength(email, "email", 3, 100);
            ValidationUtil.validateEmail(email, "email");

            // 检查用户名是否已存在
            UserDao userDao = new UserDao();
            User user = userDao.getActiveByUsername(username);
            if (user != null) {
                throw new ClientException("AlreadyExistingUsername", "用户名已被使用");
            }

            // 检查是否已有该用户名的待处理请求
            UserRegistrationRequestDao requestDao = new UserRegistrationRequestDao();
            UserRegistrationRequest existingRequest = requestDao.getByUsername(username);
            if (existingRequest != null && "PENDING".equals(existingRequest.getStatus())) {
                throw new ClientException("AlreadyExistingRequest", "该用户名已有一个待处理的注册请求");
            }

            // 创建注册请求，而不是直接创建用户
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setId(UUID.randomUUID().toString());
            request.setUsername(username);
            request.setPassword(PasswordUtil.hashPassword(password)); // 存储哈希后的密码
            request.setEmail(email);
            request.setCreateDate(new Date());
            request.setStatus("PENDING");

            // 保存注册请求
            requestDao.create(request);

            // 返回成功
            JsonObjectBuilder response = Json.createObjectBuilder()
                    .add("status", "ok");
            return Response.ok().entity(response.build()).build();
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerException("UnknownError", "创建注册请求时出错: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all pending registration requests.
     *
     * @return Response
     * @api {get} /user/registration Get all pending registration requests
     * @apiName GetUserRegistration
     * @apiGroup UserRegistration
     * @apiSuccess {Object[]} requests List of registration requests
     * @apiSuccess {String} requests.id ID
     * @apiSuccess {String} requests.username Username
     * @apiSuccess {String} requests.email Email
     * @apiSuccess {Number} requests.create_date Create date (timestamp)
     * @apiError (client) ForbiddenError Access denied
     * @apiPermission admin
     * @apiVersion 1.5.0
     */
    @GET
    public Response getPendingRequests() {
        if (!authenticate()) {
            throw new ForbiddenClientException("您的注册申请已被拒绝，请联系管理员");
        }
        checkBaseFunction(BaseFunction.ADMIN);

        UserRegistrationRequestDao requestDao = new UserRegistrationRequestDao();
        List<UserRegistrationRequest> requestList = requestDao.findAllPending();

        JsonArrayBuilder requests = Json.createArrayBuilder();
        for (UserRegistrationRequest request : requestList) {
            requests.add(Json.createObjectBuilder()
                    .add("id", request.getId())
                    .add("username", request.getUsername())
                    .add("email", request.getEmail())
                    .add("create_date", request.getCreateDate().getTime()));
        }

        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("requests", requests);
        return Response.ok().entity(response.build()).build();
    }

    /**
     * Processes a registration request.
     *
     * @param id     Request ID
     * @param status New status
     * @return Response
     * @api {post} /user/registration/{id} Process a registration request
     * @apiName PostUserRegistrationId
     * @apiGroup UserRegistration
     * @apiParam {String} id Request ID
     * @apiParam {String} status New status (APPROVED or REJECTED)
     * @apiSuccess {String} status Status OK
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) ValidationError Validation error
     * @apiError (client) NotFound Request not found or not in PENDING status
     * @apiError (server) UnknownError Unknown server error
     * @apiPermission admin
     * @apiVersion 1.5.0
     */
    @POST
    @Path("{id: [a-zA-Z0-9-]+}")
    public Response process(
            @PathParam("id") String id,
            @FormParam("status") String status) {
        if (!authenticate()) {
            throw new ForbiddenClientException("您的注册申请已被拒绝，请联系管理员");
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // 验证输入数据
        status = ValidationUtil.validateLength(status, "status", 1, 10);
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new ClientException("ValidationError", "状态必须是APPROVED或REJECTED");
        }

        // 获取请求
        UserRegistrationRequestDao requestDao = new UserRegistrationRequestDao();
        UserRegistrationRequest request = requestDao.getById(id);
        if (request == null || !"PENDING".equals(request.getStatus())) {
            throw new ClientException("NotFound", "找不到注册请求或请求不是待处理状态");
        }

        // 更新请求状态
        requestDao.updateStatus(id, status);

        // 如果审批通过，创建用户
        if (status.equals("APPROVED")) {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setRoleId(Constants.DEFAULT_USER_ROLE);
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword()); // 已经哈希过的密码
            user.setEmail(request.getEmail());
            user.setCreateDate(new Date());
            user.setPrivateKey(EncryptionUtil.generatePrivateKey());
            user.setStorageCurrent(0L);
            user.setStorageQuota(1000000000L); // 1 GB默认配额
            user.setOnboarding(true);

            // 创建用户
            UserDao userDao = new UserDao();
            try {
                EntityManager em = ThreadLocalContext.get().getEntityManager();
                em.persist(user);

                // 创建审计日志
                AuditLogUtil.create(user, AuditLogType.CREATE, principal.getId());
            } catch (Exception e) {
                throw new ServerException("UnknownError", "创建用户时出错: " + e.getMessage(), e);
            }
        }

        // 返回成功
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }

    /**
     * 检查用户名是否通过审核或审核状态
     *
     * @param username 用户名
     * @return 状态字符串或null
     */
    public static String checkUserApprovalStatus(String username) {
        try {
            UserRegistrationRequestDao requestDao = new UserRegistrationRequestDao();
            UserRegistrationRequest request = requestDao.getByUsername(username);
            if (request != null) {
                return request.getStatus();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}