package com.sismics.docs.core.dao;

import com.sismics.docs.core.model.jpa.UserRegistrationRequest;
import com.sismics.util.context.ThreadLocalContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User registration request DAO.
 *
 * @author ZS
 */
public class UserRegistrationRequestDao {

    /**
     * Creates a new registration request.
     *
     * @param userRegistrationRequest User registration request
     * @return Created request ID
     */
    public String create(UserRegistrationRequest userRegistrationRequest) {
        // Generate ID if not set
        if (userRegistrationRequest.getId() == null) {
            userRegistrationRequest.setId(UUID.randomUUID().toString());
        }

        // Set creation date if not set
        if (userRegistrationRequest.getCreateDate() == null) {
            userRegistrationRequest.setCreateDate(new Date());
        }

        // Create the request
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.persist(userRegistrationRequest);

        return userRegistrationRequest.getId();
    }

    /**
     * Returns a registration request by ID.
     *
     * @param id ID
     * @return User registration request
     */
    public UserRegistrationRequest getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            return em.find(UserRegistrationRequest.class, id);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Returns a registration request by username.
     *
     * @param username Username
     * @return User registration request
     */
    public UserRegistrationRequest getByUsername(String username) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            TypedQuery<UserRegistrationRequest> q = em.createQuery("select r from UserRegistrationRequest r where r.username = :username",
                    UserRegistrationRequest.class);
            q.setParameter("username", username);
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Returns all pending registration requests.
     *
     * @return List of registration requests
     */
    public List<UserRegistrationRequest> findAllPending() {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        TypedQuery<UserRegistrationRequest> q = em.createQuery("select r from UserRegistrationRequest r where r.status = :status order by r.createDate desc",
                UserRegistrationRequest.class);
        q.setParameter("status", "PENDING");
        return q.getResultList();
    }

    /**
     * Update the status of a registration request.
     *
     * @param id ID
     * @param status New status
     */
    public void updateStatus(String id, String status) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        UserRegistrationRequest request = getById(id);
        if (request != null) {
            request.setStatus(status);
        }
    }

    /**
     * Delete a registration request.
     *
     * @param id Request ID
     */
    public void delete(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        UserRegistrationRequest request = getById(id);
        if (request != null) {
            em.remove(request);
        }
    }
}