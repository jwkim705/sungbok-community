package com.sungbok.community.repository.member;

import static org.jooq.generated.Tables.MEMBERS;

import com.sungbok.community.dto.UpdateMember;
import java.util.Optional;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.MembersDao;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.records.MembersRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MembersRepository {

    private final DSLContext dsl;
    private final MembersDao dao;

    public MembersRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new MembersDao(configuration);
    }

    public Optional<Members> findById(Long id) {
        return Optional.ofNullable(dao.findById(id));
    }

    public Optional<Members> findByUserId(Long userId) {
         return dsl.selectFrom(MEMBERS)
                  .where(MEMBERS.USER_ID.eq(userId))
                  .fetchOptionalInto(Members.class);
    }

    public Members save(Members member) {
        MembersRecord record = dsl.newRecord(MEMBERS, member);

        // Insert the record and retrieve only the generated ID
        MembersRecord insertedRecord = dsl.insertInto(MEMBERS)
                                          .set(record)
                                          .returning(MEMBERS.ID) // Only return the ID
                                          .fetchOne();

        if (insertedRecord == null || insertedRecord.getId() == null) {
            throw new RuntimeException("Failed to save member or retrieve generated ID.");
        }

        Long newMemberId = insertedRecord.getId();
        return this.findById(newMemberId)
                   .orElseThrow(() -> new RuntimeException("Failed to find member with ID " + newMemberId + " immediately after saving."));
    }

   public void updateUsingStore(Members member) {
       if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("Member POJO with ID must be provided for update.");
       }

       MembersRecord membersRecord = dsl.fetchOptional(MEMBERS, MEMBERS.ID.eq(member.getId()))
           .orElseThrow(() -> new RuntimeException("Member not found for update with ID: " + member.getId()));

       membersRecord.setName(member.getName());
       membersRecord.setBirthdate(member.getBirthdate());
       membersRecord.setGender(member.getGender());
       membersRecord.setAddress(member.getAddress());
       membersRecord.setPhoneNumber(member.getPhoneNumber());
       membersRecord.setPicture(member.getPicture());
       membersRecord.store();
   }

}