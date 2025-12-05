package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.generated.tables.daos.RolesDao;
import org.jooq.generated.tables.pojos.Roles;
import org.springframework.stereotype.Repository;

@Repository
public class RolesRepository {

    private final RolesDao dao;

    public RolesRepository(Configuration configuration) {
        this.dao = new RolesDao(configuration);
    }

    public Roles fetchByName(String name) {
        return dao.fetchOneByName(name);
    }

}