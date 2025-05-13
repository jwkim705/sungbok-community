package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.generated.tables.daos.DepartmentsDao;
import org.jooq.generated.tables.pojos.Departments;
import org.springframework.stereotype.Repository;

@Repository
public class DeptRepository {

    private final DepartmentsDao dao;

    public DeptRepository(Configuration configuration) {
        this.dao = new DepartmentsDao(configuration);
    }

    public Departments findByName(String name) {
        return dao.fetchOneByName(name);
    }

}