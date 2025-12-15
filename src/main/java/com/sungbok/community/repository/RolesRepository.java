package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.RolesDao;
import org.jooq.generated.tables.pojos.Roles;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.generated.Tables.ROLES;

@Repository
public class RolesRepository {

    private final DSLContext dsl;
    private final RolesDao dao;

    public RolesRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new RolesDao(configuration);
    }

    public Optional<Roles> fetchByName(String name) {
        return dsl.selectFrom(ROLES)
                .where(ROLES.NAME.eq(name))
                .fetchOptionalInto(Roles.class);
    }

    /**
     * ID로 역할 조회
     *
     * @param id 역할 ID
     * @return 역할 Optional
     */
    public Optional<Roles> fetchById(Long id) {
        return Optional.ofNullable(dao.findById(id));
    }

    /**
     * 조직 ID와 level로 역할 조회 (signup 시 기본 역할 할당용)
     *
     * @param orgId 조직 ID
     * @param level 역할 레벨 (1=낮음, 3=높음)
     * @return 역할 Optional
     */
    public Optional<Roles> fetchByOrgIdAndLevel(Long orgId, Integer level) {
        return dsl.selectFrom(ROLES)
                .where(ROLES.ORG_ID.eq(orgId))
                .and(ROLES.LEVEL.eq(level))
                .orderBy(ROLES.ID.asc())
                .limit(1)
                .fetchOptionalInto(Roles.class);
    }

    /**
     * 조직 ID로 모든 역할 조회 (프론트엔드 API용)
     *
     * @param orgId 조직 ID
     * @return 역할 목록 (level, name 순 정렬)
     */
    public List<Roles> fetchAllByOrgId(Long orgId) {
        return dsl.selectFrom(ROLES)
                .where(ROLES.ORG_ID.eq(orgId))
                .orderBy(ROLES.LEVEL.asc(), ROLES.NAME.asc())
                .fetchInto(Roles.class);
    }

}