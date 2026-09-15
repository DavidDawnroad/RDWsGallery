package com.test.prac.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserAccountEntity is a Querydsl query type for UserAccountEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserAccountEntity extends EntityPathBase<UserAccountEntity> {

    private static final long serialVersionUID = -1275749067L;

    public static final QUserAccountEntity userAccountEntity = new QUserAccountEntity("userAccountEntity");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final EnumPath<com.test.prac.enums.AccLv> accessLevel = createEnum("accessLevel", com.test.prac.enums.AccLv.class);

    public final StringPath email = createString("email");

    public final StringPath id = createString("id");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> moddate = _super.moddate;

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regdate = _super.regdate;

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final EnumPath<com.test.prac.enums.UStat> userStatus = createEnum("userStatus", com.test.prac.enums.UStat.class);

    public QUserAccountEntity(String variable) {
        super(UserAccountEntity.class, forVariable(variable));
    }

    public QUserAccountEntity(Path<? extends UserAccountEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserAccountEntity(PathMetadata metadata) {
        super(UserAccountEntity.class, metadata);
    }

}

